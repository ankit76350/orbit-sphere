package com.orbitastra.backend.dto.crm;

import com.orbitastra.backend.models.crm.Admission;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Client payload for creating an admission. Direct admissions require
 * {@code schoolId} and {@code studentName}; inquiry-backed admissions require only
 * {@code inquiryDocsId}, with {@code schoolId} inferred from the inquiry. Any other
 * supplied fields override or augment the inquiry snapshot.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CreateAdmissionRequest extends AdmissionDetailsRequest {

    private String schoolId;

    private String inquiryDocsId;

    @Override
    public Admission toModel() {
        Admission admission = super.toModel();
        admission.setSchoolId(schoolId);
        admission.setInquiryDocsId(inquiryDocsId);
        return admission;
    }
}
