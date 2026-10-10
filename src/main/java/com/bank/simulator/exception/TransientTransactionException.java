package com.bank.simulator.exception;

public class TransientTransactionException extends RuntimeException {
    public TransientTransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
