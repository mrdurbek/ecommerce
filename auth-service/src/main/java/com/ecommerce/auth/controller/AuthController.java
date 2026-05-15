package com.ecommerce.auth.controller;

import com.ecommerce.auth.domain.entity.User;
import com.ecommerce.auth.dto.request.*;
import com.ecommerce.auth.dto.response.ApiResponse;
import com.ecommerce.auth.dto.response.AuthResponse;
import com.ecommerce.auth.dto.response.UserResponse;
import com.ecommerce.auth.exception.RateLimitException;
import com.ecommerce.auth.service.AuthService;
import com.ecommerce.auth.service.RateLimitingService;
import com.ecommerce.auth.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Auth endpoints")
public class AuthController {

    private final AuthService authService;
    private final RateLimitingService rateLimitingService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        String ip = getClientIp(httpRequest);

        if (!rateLimitingService.isRegisterAllowed(ip)) {
            throw new RateLimitException("Too many registration attempts. Please try again later.");
        }

        UserResponse userResponse = authService.register(request, ip, httpRequest.getHeader("User-Agent"));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful. Please verify your email.", userResponse));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String ip = getClientIp(httpRequest);

        if (!rateLimitingService.isLoginAllowed(ip)) {
            long remaining = rateLimitingService.getLoginRemainingAttempts(ip);
            throw new RateLimitException(
                    "Too many login attempts. Please try again in 15 minutes. Remaining attempts: " + remaining
            );
        }

        AuthResponse authResponse = authService.login(request, ip, httpRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {
        String ip = getClientIp(httpRequest);
        AuthResponse authResponse = authService.refreshToken(
                request.getRefreshToken(), ip, httpRequest.getHeader("User-Agent")
        );
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", authResponse));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout current session")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            @AuthenticationPrincipal User user,
            HttpServletRequest httpRequest
    ) {
        String accessToken = extractTokenFromRequest(httpRequest);
        String refreshToken = request != null ? request.getRefreshToken() : null;
        String ip = getClientIp(httpRequest);

        authService.logout(accessToken, refreshToken, user.getId(), ip, httpRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout from all devices")
    public ResponseEntity<ApiResponse<Void>> logoutAll(
            @AuthenticationPrincipal User user,
            HttpServletRequest httpRequest
    ) {
        String ip = getClientIp(httpRequest);
        authService.logoutAll(user.getId(), ip, httpRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(ApiResponse.success("Logged out from all devices"));
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify email address")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam(name = "token") String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully. You can now login."));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend verification email")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@RequestParam(name = "email") String email) {
        authService.resendVerificationEmail(email);
        return ResponseEntity.ok(ApiResponse.success("Verification email sent if account exists."));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password (authenticated)")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal User user,
            HttpServletRequest httpRequest
    ) {
        authService.changePassword(request, user.getId(), getClientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully. Please login again."));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user info")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success("User info retrieved", authService.mapToUserResponse(user)));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}