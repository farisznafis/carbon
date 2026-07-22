package com.kct.jcredit.core_backend.handler;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.response.FileUploadResponse;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;
import com.kct.jcredit.core_backend.dto.response.details.FileErrorDetailsResponse;
import com.kct.jcredit.core_backend.exception.AuthenticationException;
import com.kct.jcredit.core_backend.exception.BusinessRuleViolationException;
import com.kct.jcredit.core_backend.exception.CustomException;
import com.kct.jcredit.core_backend.exception.DuplicateResourceException;
import com.kct.jcredit.core_backend.exception.FileTooLargeException;
import com.kct.jcredit.core_backend.exception.ResourceNotFoundException;
import com.kct.jcredit.core_backend.exception.UnsupportedFileTypeException;
import com.kct.jcredit.core_backend.util.ResponseUtil;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalErrorHandler {

    private static final int MAX_UPLOAD_SIZE_MB = 10;

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

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<FileUploadResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        log.warn("Multipart upload exceeded max size: maxUploadSize={}", ex.getMaxUploadSize());

        return new ResponseEntity<>(fileTooLargeUploadResponse(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<BaseResponse> handleMultipartException(MultipartException ex) {
        log.warn("Multipart request failed: {}", ex.getMessage());

        if (containsCause(ex, MaxUploadSizeExceededException.class)) {
            BaseResponse response = fileTooLargeUploadResponse();
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        BaseResponse response = BaseResponse.error(ResponseConstants.BAD_REQUEST, ResponseConstants.MSG_BAD_REQUEST);
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
        Map<String, String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                fieldError -> resolveJsonFieldName(fieldError.getField(), fieldError.getObjectName()),
                FieldError::getDefaultMessage,
                (first, second) -> first,
                LinkedHashMap::new
            ));
        response.setStatusCode(ResponseConstants.VALIDATION_ERROR);
        response.setMessage(ResponseConstants.MSG_VALIDATION_ERROR);
        response.setErrors(errors);

        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<BaseResponse> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        log.warn("Missing request parameter: {}", ex.getParameterName());

        BaseResponse response = BaseResponse.error(ResponseConstants.BAD_REQUEST, ex.getMessage());
        return new ResponseEntity<>(response, ResponseUtil.getHttpStatus(response.getStatusCode()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("Invalid request body: {}", ex.getMessage());

        BaseResponse response = BaseResponse.error(ResponseConstants.BAD_REQUEST, ResponseConstants.MSG_BAD_REQUEST);
        Map<String, String> errors = resolveJsonParseErrors(ex);
        if (!errors.isEmpty()) {
            response.setErrors(errors);
        }
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

    private String resolveJsonFieldName(String fieldPath, String objectName) {
        Class<?> targetClass = resolveClassFromObjectName(objectName);
        if (targetClass == null) {
            return fieldPath;
        }

        String[] pathParts = fieldPath.split("\\.");
        Class<?> currentClass = targetClass;
        StringBuilder jsonPath = new StringBuilder();

        for (String pathPart : pathParts) {
            String collectionSuffix = "";
            String fieldName = pathPart;
            int collectionStart = pathPart.indexOf('[');
            if (collectionStart >= 0) {
                collectionSuffix = pathPart.substring(collectionStart);
                fieldName = pathPart.substring(0, collectionStart);
            }

            String jsonFieldName = resolveJsonFieldName(currentClass, fieldName);
            if (jsonPath.length() > 0) {
                jsonPath.append(".");
            }
            jsonPath.append(jsonFieldName).append(collectionSuffix);

            Field nestedField = findField(currentClass, fieldName);
            if (nestedField != null) {
                currentClass = nestedField.getType();
            }
        }

        return jsonPath.toString();
    }

    private Class<?> resolveClassFromObjectName(String objectName) {
        String basePackage = "com.kct.jcredit.core_backend.dto.request.";
        String className = Character.toUpperCase(objectName.charAt(0)) + objectName.substring(1);
        try {
            return Class.forName(basePackage + className);
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }

    private String resolveJsonFieldName(Class<?> targetClass, String fieldName) {
        Field field = findField(targetClass, fieldName);
        if (field == null) {
            return fieldName;
        }

        JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
        if (jsonProperty != null && !jsonProperty.value().isBlank()) {
            return jsonProperty.value();
        }

        return fieldName;
    }

    private Field findField(Class<?> targetClass, String fieldName) {
        Class<?> currentClass = targetClass;
        while (currentClass != null && currentClass != Object.class) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ex) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }

    private FileUploadResponse fileTooLargeUploadResponse() {
        FileUploadResponse response = new FileUploadResponse();
        response.setStatusCode(ResponseConstants.FILE_TOO_LARGE);
        response.setMessage(ResponseConstants.MSG_FILE_TOO_LARGE);
        response.setDetails(FileErrorDetailsResponse.builder()
                .maxSizeMB(MAX_UPLOAD_SIZE_MB)
                .build());
        return response;
    }

    private boolean containsCause(Throwable throwable, Class<? extends Throwable> targetClass) {
        Throwable current = throwable;
        while (current != null) {
            if (targetClass.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private Map<String, String> resolveJsonParseErrors(HttpMessageNotReadableException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        Throwable cause = ex.getCause();
        if (!(cause instanceof JsonMappingException mappingException)) {
            return errors;
        }

        String fieldName = mappingException.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .filter(name -> name != null && !name.isBlank())
                .reduce((first, second) -> second)
                .orElse(null);

        if (fieldName == null) {
            return errors;
        }

        if (mappingException instanceof InvalidFormatException) {
            errors.put(fieldName, "入力形式が正しくありません。");
        } else if (mappingException instanceof MismatchedInputException) {
            errors.put(fieldName, "入力値の型が正しくありません。");
        } else {
            errors.put(fieldName, ResponseConstants.MSG_BAD_REQUEST);
        }
        return errors;
    }

    
}

