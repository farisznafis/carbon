package com.kct.jcredit.core_backend.handler;
 
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.exception.AuthenticationException;
import com.kct.jcredit.core_backend.exception.BusinessRuleViolationException;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.exception.DuplicateResourceException;
import com.kct.jcredit.core_backend.exception.FileTooLargeException;
import com.kct.jcredit.core_backend.exception.ResourceNotFoundException;
import com.kct.jcredit.core_backend.exception.UnsupportedFileTypeException;
import com.kct.jcredit.core_backend.util.ResponseUtil;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalErrorHandler {

    // ---- Most specific custom exceptions first ----

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<BaseResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());

        BaseResponse response = BaseResponse.error(ex.getErrorCode(), ex.getMessage());
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<BaseResponse> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());

        BaseResponse response = BaseResponse.error(ex.getErrorCode(), ex.getMessage());
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<BaseResponse> handleBusinessRuleViolationException(BusinessRuleViolationException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());

        BaseResponse response = BaseResponse.error(ex.getErrorCode(), ex.getMessage());
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<BaseResponse> handleDuplicateResourceException(DuplicateResourceException ex) {
        log.warn("Duplicate resource: {}", ex.getMessage());

        BaseResponse response = BaseResponse.error(ex.getErrorCode(), ex.getMessage());
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @ExceptionHandler(FileTooLargeException.class)
    public ResponseEntity<BaseResponse> handleFileTooLargeException(FileTooLargeException ex) {
        log.warn("File too large: maxSizeMB={}, actualSizeMB={}", ex.getMaxSizeMB(), ex.getActualSizeMB());

        BaseResponse response = BaseResponse.error(ResponseConstants.FILE_TOO_LARGE, ResponseConstants.MSG_FILE_TOO_LARGE);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnsupportedFileTypeException.class)
    public ResponseEntity<BaseResponse> handleUnsupportedFileTypeException(UnsupportedFileTypeException ex) {
        log.warn("Unsupported file type: received={}, allowed={}", ex.getReceivedType(), ex.getAllowedTypes());

        BaseResponse response = BaseResponse.error(ResponseConstants.UNSUPPORTED_FILE_TYPE, ResponseConstants.MSG_UNSUPPORTED_FILE_TYPE);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // ---- Base custom exception catch-all (catches any CustomException subclass not handled above) ----

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<BaseResponse> handleCustomException(CustomException ex) {
        log.warn("Custom exception [{}]: {}", ex.getErrorCode(), ex.getMessage());

        BaseResponse response = BaseResponse.error(ex.getErrorCode(), ex.getMessage());
        if (ex.getFields() != null) {
            response.setErrors(ex.getFields());
        }
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    // ---- Spring Security / Framework exceptions ----

    @ExceptionHandler(AuthorizationDeniedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<BaseResponse> handleAuthorizationException(AuthorizationDeniedException ex) {
        log.warn("Authorization denied: {}", ex.getMessage());

        BaseResponse response = new BaseResponse();
        response.setStatusCode(ResponseConstants.FORBIDDEN);
        response.setMessage(ResponseConstants.MSG_UNAUTHORIZED);

        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {}", ex.getMessage());

        BaseResponse response = new BaseResponse();
        // Join all validation error messages
        Map<String, String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                FieldError::getDefaultMessage,
                (first, second) -> first
            ));
        response.setStatusCode(ResponseConstants.VALIDATION_ERROR);
        response.setErrors(errors);

        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<BaseResponse> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        log.warn("Missing request parameter: {}", ex.getParameterName());

        BaseResponse response = BaseResponse.error(ResponseConstants.BAD_REQUEST, ex.getMessage());
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    // ---- Catch-all for any unhandled exception ----

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse> handleDefaultException(Exception ex) {

        log.error("Default exception handler: {}", ex.getClass().getName(), ex);

        BaseResponse response = new BaseResponse();
        response.setStatusCode(ResponseConstants.INTERNAL_SERVER_ERROR); // or BAD_REQUEST if you define it
        response.setMessage(ResponseConstants.MSG_INTERNAL_ERROR);

        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }
}

