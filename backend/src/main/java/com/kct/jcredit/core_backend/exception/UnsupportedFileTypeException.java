package com.kct.jcredit.core_backend.exception;

import java.util.List;

public class UnsupportedFileTypeException extends RuntimeException {
    private final List<String> allowedTypes;
    private final String receivedType;
    
        public UnsupportedFileTypeException(List<String> allowedTypes, String receivedType) {
        // allowedTypes / receivedType hold file extensions (e.g. "xlsx"), not MIME types
        super(String.format("このファイル形式（%s）はアップロードできません。アップロード可能な形式：%s",
                (receivedType == null || receivedType.isEmpty()) ? "拡張子なし" : receivedType,
                String.join(", ", allowedTypes)));
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