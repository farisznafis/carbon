package com.kct.jcredit.core_backend.dto.response.base;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kct.jcredit.core_backend.constant.ResponseConstants;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse {
    private String statusCode;
    private String message;
    private Map<String, String> errors;
    private LocalDateTime timestamp;

    /**
     * Legacy constructor for backward compatibility.
     */
    public BaseResponse(String statusCode, String message, Map<String, String> errors) {
        this.statusCode = statusCode;
        this.message = message;
        this.errors = errors;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Creates a success response with the given message.
     */
    public static BaseResponse success(String message) {
        BaseResponse response = new BaseResponse();
        response.setStatusCode(ResponseConstants.OK);
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    /**
     * Creates an error response with the given status code and message.
     */
    public static BaseResponse error(String statusCode, String message) {
        BaseResponse response = new BaseResponse();
        response.setStatusCode(statusCode);
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    /**
     * Creates an error response with field-level validation errors.
     */
    public static BaseResponse validationError(Map<String, String> fieldErrors) {
        BaseResponse response = new BaseResponse();
        response.setStatusCode(ResponseConstants.VALIDATION_ERROR);
        response.setErrors(fieldErrors);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
}
