package com.kct.jcredit.core_backend.dto.response.details;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
 
@Getter
@Builder
@AllArgsConstructor
public class ValidationErrorDetail {

    private final String tempId;
    private final String field;
    private final String message;
    
}