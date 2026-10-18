package com.kct.jcredit.core_backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AnyCharacterValidator implements ConstraintValidator<AnyCharacter, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return true;
    }
}
