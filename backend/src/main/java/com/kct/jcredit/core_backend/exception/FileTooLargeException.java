package com.kct.jcredit.core_backend.exception;

public class FileTooLargeException extends RuntimeException {
    private final int maxSizeMB;
    private final double actualSizeMB;
    
    public FileTooLargeException(int maxSizeMB, double actualSizeMB) {
        super("アップロードされたファイルは、許可されている最大サイズを超えています。");
        this.maxSizeMB = maxSizeMB;
        this.actualSizeMB = actualSizeMB;
    }
    
    public int getMaxSizeMB() { 
        return maxSizeMB; 
    }
    
    public double getActualSizeMB() { 
        return actualSizeMB; 
    }
}
