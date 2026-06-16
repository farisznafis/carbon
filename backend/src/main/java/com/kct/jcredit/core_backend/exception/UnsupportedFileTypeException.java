package com.kct.jcredit.core_backend.exception;

import java.util.List;

public class UnsupportedFileTypeException extends RuntimeException {
    private final List<String> allowedTypes;
    private final String receivedType;
    
    public UnsupportedFileTypeException(List<String> allowedTypes, String receivedType) {
        super("このファイル形式はアップロードできません。");
        this.allowedTypes = allowedTypes;
        this.receivedType = receivedType;
    }
    
    public List<String> getAllowedTypes() { 
        return allowedTypes; 
    }
    
    public String getReceivedType() { 
        return receivedType; 
    }
}