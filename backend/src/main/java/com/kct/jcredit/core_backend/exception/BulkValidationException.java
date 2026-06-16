package com.kct.jcredit.core_backend.exception;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.kct.jcredit.core_backend.constant.ResponseConstants;
import com.kct.jcredit.core_backend.dto.response.details.ValidationErrorDetail;

/**
 * 
 */
public class BulkValidationException extends CustomException {

    public BulkValidationException(List<ValidationErrorDetail> errors) {
        super(
            ResponseConstants.VALIDATION_ERROR,
            "入力内容に不備があります。",
            errors.stream().collect(Collectors.toMap(
                d -> d.getTempId() + "::" + d.getField(),
                ValidationErrorDetail::getMessage,
                (first, second) -> first
            )),
            null
        );
    }
}