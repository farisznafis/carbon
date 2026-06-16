package com.kct.jcredit.core_backend.exception;

public class AlreadySubmittedException extends RuntimeException {
    
    public AlreadySubmittedException(String message) {
        super(message);
    }
}