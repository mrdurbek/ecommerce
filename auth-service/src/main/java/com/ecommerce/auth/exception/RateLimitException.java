package com.ecommerce.auth.exception;

public class RateLimitException extends RuntimeException {
    public RateLimitException(String message) { super(message); }
}