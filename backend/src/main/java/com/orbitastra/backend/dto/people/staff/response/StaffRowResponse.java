package com.orbitastra.backend.dto.people.staff.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.common.enums.CountryCode;
import com.orbitastra.backend.models.common.enums.Gender;
import com.orbitastra.backend.models.people.staff.Staff;

/**
 * One person, as #7 lists them.
 *
 * <h2>The row is deliberately thin</h2>
 *
 * <p><b>No date of birth, no address, no emergency contact.</b> Those are on #8, one call away.
 * A list endpoint returning them puts every employee's personal data into the network tab of every
 * dropdown that reads it — and this module has no authorization yet, so "only the staff screen
 * calls it" is not a control, it is a hope.
 *
 * <p><b>The phone and the email ARE here</b>, and that is a deliberate line rather than an
 * inconsistency. A staff list is a contact list: the office reading it is looking for somebody to
 * ring. A date of birth is never what a picker needs.
 *
 * <h2>What is missing until #16</h2>
 *
 * <p>The plan's row also carries a position title, a department name and an employment type. All
 * three live on {@code EmploymentRecord}, which nothing writes yet. When #16 lands they join this
 * record rather than replace it — the fields below are facts about a person and do not move.
 */
public record StaffRowResponse(
        String staffDocsId,
        String employeeNo,
        String fullName,
        Gender gender,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String phoneNumber,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String emailAddress,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        CountryCode nationalityCode) {

    public static StaffRowResponse fromStaff(Staff staff) {
        return new StaffRowResponse(
                staff.getId(),
                staff.getEmployeeNo(),
                staff.getFullName(),
                staff.getGender(),
                staff.getPhoneNumber(),
                staff.getEmailAddress(),
                staff.getNationalityCode());
    }
}
