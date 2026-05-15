package com.ecommerce.auth.service;

import com.ecommerce.auth.domain.entity.*;
import com.ecommerce.auth.domain.repository.*;
import com.ecommerce.auth.dto.request.*;
import com.ecommerce.auth.dto.response.AuthResponse;
import com.ecommerce.auth.dto.response.UserResponse;
import com.ecommerce.auth.exception.*;
import com.ecommerce.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    private final EmailService emailService;

    @Value("${app.email-verification-expiry}")
    private long emailVerificationExpiry;

    @Value("${app.password-reset-expiry}")
    private long passwordResetExpiry;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Transactional
    public UserResponse register(RegisterRequest request, String ipAddress, String userAgent) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already registered: " + request.getEmail());
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Default role not found"));

        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .roles(Set.of(userRole))
                .isEnabled(false)
                .build();

        user = userRepository.save(user);

        sendVerificationEmail(user);

        return mapToUserResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (DisabledException e) {
            throw new AccountNotVerifiedException("Please verify your email before logging in.");
        } catch (LockedException e) {
            throw new AccountLockedException("Your account has been temporarily locked. Try again later.");
        } catch (BadCredentialsException e) {
            handleFailedLogin(request.getEmail(), ipAddress, userAgent);
            throw new InvalidCredentialsException("Invalid email or password.");
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException("Authentication failed.");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        userRepository.updateLastLoginAndResetAttempts(user.getId(), LocalDateTime.now());

        String accessToken = jwtUtil.generateAccessToken(user);
        RefreshToken refreshToken = tokenService.createRefreshToken(user, request.getDeviceInfo(), ipAddress);

        return buildAuthResponse(accessToken, refreshToken.getToken(), user);
    }


    @Transactional
    public AuthResponse refreshToken(String token, String ipAddress, String userAgent) {
        RefreshToken newRefreshToken = tokenService.validateAndRotateRefreshToken(token);
        User user = newRefreshToken.getUser();

        String accessToken = jwtUtil.generateAccessToken(user);

        return buildAuthResponse(accessToken, newRefreshToken.getToken(), user);
    }

    @Transactional
    public void logout(String accessToken, String refreshToken, Long userId, String ipAddress, String userAgent) {
        tokenService.blacklistAccessToken(accessToken, accessTokenExpiration);

        if (refreshToken != null) {
            tokenService.revokeRefreshToken(refreshToken);
        }
    }

    @Transactional
    public void logoutAll(Long userId, String ipAddress, String userAgent) {
        tokenService.revokeAllUserTokens(userId);
    }

    @Transactional
    public void verifyEmail(String token) {
        EmailVerification verification = emailVerificationRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired verification token"));

        if (!verification.isValid()) {
            throw new InvalidTokenException("Verification token has expired. Please request a new one.");
        }

        User user = verification.getUser();
        user.setIsEnabled(true);
        userRepository.save(user);

        emailVerificationRepository.markAsUsed(verification.getId());
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getIsEnabled()) {
            throw new BadRequestException("Email is already verified");
        }

        sendVerificationEmail(user);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request, Long userId, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        tokenService.revokeAllUserTokens(userId);

        emailService.sendPasswordChangedNotification(user.getEmail(), user.getFirstName());
    }

    private void sendVerificationEmail(User user) {
        String token = UUID.randomUUID().toString();

        EmailVerification verification = EmailVerification.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusSeconds(emailVerificationExpiry / 1000))
                .build();

        emailVerificationRepository.save(verification);
        emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), token);
    }

    private void handleFailedLogin(String email, String ipAddress, String userAgent) {
        userRepository.findByEmail(email).ifPresent(user -> {
            userRepository.incrementFailedLoginAttempts(user.getId());

            if (user.getFailedLoginAttempts() + 1 >= 5) {
                LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(15);
                userRepository.lockAccount(user.getId(), lockUntil);
            }
        });
    }

    private AuthResponse buildAuthResponse(String accessToken, String refreshToken, User user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresIn(accessTokenExpiration)
                .user(mapToUserResponse(user))
                .build();
    }

    public UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .enabled(user.getIsEnabled())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .permissions(user.getRoles().stream()
                        .flatMap(role -> role.getPermissions().stream())
                        .map(permission -> permission.getName())
                        .collect(Collectors.toSet()))
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}