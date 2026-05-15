package com.ecommerce.payment.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Random;

@Slf4j
@Component
public class MockPaymentGateway {

    @Value("${payment.mock.success-rate:0.9}")
    private double successRate;

    private static final String[] FAILURE_REASONS = {
            "Insufficient funds",
            "Card declined",
            "Transaction limit exceeded",
            "Invalid card details",
            "Bank connection timeout"

    };

    private final Random random = new Random();

    public GatewayResult process(String paymentRef, BigDecimal amount, String method) {
        log.info("Processing payment {} via {}, amount: {}", paymentRef, method, amount);

        boolean success = random.nextDouble() < successRate;
        if(success) {
            log.info("Payment {} completed successfully", paymentRef);
            return GatewayResult.success("T-" + System.currentTimeMillis());
        } else {
            String reason = FAILURE_REASONS[random.nextInt(FAILURE_REASONS.length)];
            log.warn("Payment {} failed: {}", paymentRef, reason);
            return GatewayResult.error(reason);
        }
    }

    public GatewayResult refund(String paymentRef, BigDecimal amount) {
        return GatewayResult.success("REF-" + System.currentTimeMillis());
    }

    public record GatewayResult(boolean success, String transactionId, String errorMessage){
        public static GatewayResult success(String transactionId) {
            return new GatewayResult(true, transactionId, null);
        }

        public static  GatewayResult error(String message) {
            return new GatewayResult(false, null, message);
        }
    }
}
