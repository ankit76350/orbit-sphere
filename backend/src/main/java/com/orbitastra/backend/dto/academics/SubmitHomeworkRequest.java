package com.orbitastra.backend.dto.academics;

import lombok.Data;

/**
 * Typed replacement for the raw map previously accepted by the submit endpoint.
 */
@Data
public class SubmitHomeworkRequest {

    private String submissionText;

    private String submissionFileUrl;
}
