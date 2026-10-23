package com.orbitastra.backend.repositories.crm.admissionapplication;

import com.orbitastra.backend.models.crm.enums.AdmissionApplicationStatus;

/**
 * One row of #7's aggregation: how many applications a class has in a status.
 *
 * <p><b>A projection, not a document.</b> It exists so the grouped count can come back typed
 * instead of as a {@code Document} the service has to pick apart — the same reason a repository
 * returns a model rather than a map.
 *
 * <p><b>{@code classDocsId} can be null</b>, and that is not a defect. An application's
 * {@code appliedClassDocsId} is required, so in practice it will not be — but a grouped count
 * reports what is in the collection rather than what should be, and a row that turns up with no
 * class is a real problem somebody should see rather than one this quietly drops.
 */
public record ClassStatusCount(
        String classDocsId,
        AdmissionApplicationStatus status,
        long count) {
}
