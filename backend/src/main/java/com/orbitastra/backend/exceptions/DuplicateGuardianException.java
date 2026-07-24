package com.orbitastra.backend.exceptions;

/**
 * Thrown when a guardian being created already exists for the school (matched by
 * name + phone, or name + email). Carries the existing guardian's id/name so the
 * client can offer to link that guardian instead of creating a duplicate.
 */
public class DuplicateGuardianException extends RuntimeException {

    private final String existingGuardianDocsId;
    private final String existingGuardianName;

    public DuplicateGuardianException(String message, String existingGuardianDocsId, String existingGuardianName) {
        super(message);
        this.existingGuardianDocsId = existingGuardianDocsId;
        this.existingGuardianName = existingGuardianName;
    }

    public String getExistingGuardianDocsId() {
        return existingGuardianDocsId;
    }

    public String getExistingGuardianName() {
        return existingGuardianName;
    }
}
