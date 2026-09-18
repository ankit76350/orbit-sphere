package com.orbitastra.backend.dto.people.department.response;

import com.orbitastra.backend.models.people.staff.Staff;

/**
 * Just enough of a person to name them as a department's head.
 *
 * <p><b>Two fields, and that is a boundary rather than laziness.</b> A {@code Staff} document
 * carries an address, a date of birth and a national identity number; a department page needs a
 * name. Returning the record would leak the most sensitive data this product holds through an
 * endpoint nobody would think to check — and the module plan says authorization matters more here
 * than anywhere and does not exist yet.
 */
public record StaffSummaryResponse(
        String staffDocsId,
        String fullName) {

    public static StaffSummaryResponse of(Staff staff) {
        return new StaffSummaryResponse(staff.getId(), staff.getFullName());
    }
}
