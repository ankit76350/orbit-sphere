package com.orbitastra.backend.dto.crm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class CreateInquiryRequestTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void counselorDocsId_isRequiredAndCannotBeBlank() {
        CreateInquiryRequest request = new CreateInquiryRequest();
        request.setSchoolId("school-123");
        request.setCounselorDocsId("   ");

        Set<ConstraintViolation<CreateInquiryRequest>> violations =
                validator.validate(request);

        assertEquals(1, violations.size());
        ConstraintViolation<CreateInquiryRequest> violation =
                violations.iterator().next();
        assertEquals("counselorDocsId", violation.getPropertyPath().toString());
        assertEquals("counselorDocsId is required", violation.getMessage());
    }

    @Test
    void counselorDocsId_present_passesRequiredFieldValidation() {
        CreateInquiryRequest request = new CreateInquiryRequest();
        request.setSchoolId("school-123");
        request.setCounselorDocsId("staff-456");

        assertTrue(validator.validate(request).isEmpty());
    }
}
