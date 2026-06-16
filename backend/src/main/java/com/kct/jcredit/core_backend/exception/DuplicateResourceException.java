package com.kct.jcredit.core_backend.exception;

import com.kct.jcredit.core_backend.constant.ResponseConstants;

/**
 * Thrown when attempting to create a resource that already exists.
 */
public class DuplicateResourceException extends CustomException {

    public DuplicateResourceException(String message) {
        super(ResponseConstants.BAD_REQUEST, message);
    }

    public DuplicateResourceException(String errorCode, String message) {
        super(errorCode, message);
    }

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(ResponseConstants.BAD_REQUEST,
              String.format("%s already exists with %s: %s", resourceName, fieldName, fieldValue));
    }
}
