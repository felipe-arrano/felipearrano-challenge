package com.felipearrano.challenge.infrastructure.adapter.out.external.exception;

public class PercentageServiceUnavailableException extends RuntimeException{

    public PercentageServiceUnavailableException(String message) {
        super(message);
    }

    public PercentageServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
