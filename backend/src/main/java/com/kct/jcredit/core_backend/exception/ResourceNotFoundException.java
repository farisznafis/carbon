package com.kct.jcredit.core_backend.exception;

import com.kct.jcredit.core_backend.constant.ResponseConstants;

/**
 * Thrown when a requested resource cannot be found.
 */
public class ResourceNotFoundException extends CustomException {

    public ResourceNotFoundException(String message) {
        super(ResponseConstants.NOT_FOUND, message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(ResponseConstants.NOT_FOUND,
              String.format("%s not found with %s: %s", resourceName, fieldName, fieldValue));
    }
}
