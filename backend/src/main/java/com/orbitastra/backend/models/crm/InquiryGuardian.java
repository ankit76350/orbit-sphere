package com.orbitastra.backend.models.crm;

import com.orbitastra.backend.models.student.enums.GuardianRelation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A prospective guardian captured on an {@link Inquiry}. This is raw lead data —
 * no Guardian document exists yet. When the inquiry is converted to a student
 * (via the admission), each of these becomes a real
 * {@link com.orbitastra.backend.models.student.Guardian} and is linked to the
 * student. Carried through the funnel as-is until then.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryGuardian {

    private String name;

    private GuardianRelation relation;

    private String phone;

    private String email;

    private String address;

    private String occupation;
}
