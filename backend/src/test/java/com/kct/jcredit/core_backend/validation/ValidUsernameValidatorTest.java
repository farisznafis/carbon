package com.kct.jcredit.core_backend.validation;

import com.kct.jcredit.core_backend.dto.request.CreateUserRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidUsernameValidatorTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsUnderscoreAndEmoji() {
        assertTrue(usernameViolations("user_name😀").isEmpty());
        assertTrue(usernameViolations("dev👩🏽‍💻").isEmpty());
    }

    @Test
    void keepsPreviouslyAllowedSpaceAndAtSign() {
        assertTrue(usernameViolations("user name@1").isEmpty());
    }

    @Test
    void rejectsUnsupportedPunctuationAndControlCharacters() {
        assertFalse(usernameViolations("user-name").isEmpty());
        assertFalse(usernameViolations("user\nname").isEmpty());
    }

    private Set<ConstraintViolation<CreateUserRequest>> usernameViolations(String username) {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername(username);

        return validator.validate(request).stream()
                .filter(violation -> "username".equals(violation.getPropertyPath().toString()))
                .collect(java.util.stream.Collectors.toSet());
    }
}
