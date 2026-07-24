package com.orbitastra.backend.dto.crm;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Adds one document name, URL, or storage reference to an existing admission. */
@Data
public class AddAdmissionDocumentRequest {

    @NotBlank(message = "document is required")
    private String document;
}
