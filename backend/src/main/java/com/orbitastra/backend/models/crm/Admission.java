package com.orbitastra.backend.models.crm;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.crm.enums.AdmissionStatus;
import com.orbitastra.backend.models.student.enums.Gender;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "admissions")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Admission extends SchoolBase {

    // Business identifier carried forward to the Student on enrolment. Sparse
    // keeps legacy admission documents (created before this field existed) valid.
    @Indexed(unique = true, sparse = true)
    private String admissionNo;

    // from / same as inquiry
    @Indexed(unique = true, sparse = true)
    private String inquiryDocsId;

    private String studentName;

    @Builder.Default
    private List<InquiryGuardian> guardians = new java.util.ArrayList<>();

    // not in inquiry
    private AdmissionStatus status;
    private LocalDate admissionDate;
    private LocalDate dob;
    private Gender gender;
    private List<String> documents;

    //when become student
    private String studentDocsId;
}
