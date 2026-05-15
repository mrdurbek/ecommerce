package com.ecommerce.auth.exception;

public class AccountNotVerifiedException extends RuntimeException {
    public AccountNotVerifiedException(String message) { super(message); }
}