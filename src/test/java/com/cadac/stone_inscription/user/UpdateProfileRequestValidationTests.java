package com.cadac.stone_inscription.user;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.cadac.stone_inscription.user.dto.UpdateProfileRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class UpdateProfileRequestValidationTests {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        validator = null;
    }

    @Test
    void whenUsernameContainsNonAlphanumericCharacters_thenValidationFails() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername("alert(1)");
        request.setBio("Epigraphy researcher");

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> "username".equals(v.getPropertyPath().toString())));
    }

    @Test
    void whenUsernameIsValid_thenValidationPasses() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername("inscription_scholar");
        request.setBio("Epigraphy researcher.");

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void whenUsernameContainsSpaces_thenValidationPasses() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername("Nayanmoni Baruah");
        request.setBio("Epigraphy researcher.");

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void whenUsernameHasLeadingOrTrailingSpaces_thenValidationPasses() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername("  Nayanmoni Baruah  ");
        request.setBio("Epigraphy researcher.");

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void whenBioContainsHtmlTags_thenValidationFails() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setBio("<script>alert(1)</script>");

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> "bio".equals(v.getPropertyPath().toString())));
    }

    @Test
    void whenUsernameIsMissingButBioIsValid_thenValidationPasses() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setBio("Epigraphy researcher");

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }
}
