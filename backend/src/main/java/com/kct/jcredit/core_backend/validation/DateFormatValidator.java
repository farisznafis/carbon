package com.kct.jcredit.core_backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public class DateFormatValidator implements ConstraintValidator<DateFormat, String> {

    private DateTimeFormatter formatter;

    @Override
    public void initialize(DateFormat annotation) {
        this.formatter = DateTimeFormatter.ofPattern(annotation.pattern())
                .withResolverStyle(ResolverStyle.SMART);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        try {
            LocalDate.parse(value, formatter);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
