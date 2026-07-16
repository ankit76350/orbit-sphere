package com.orbitastra.backend.models.crm;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.crm.enums.AdmissionStatus;
import com.orbitastra.backend.models.student.enums.Gender;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "admissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Admission {
    @org.springframework.data.annotation.CreatedDate
    private java.time.LocalDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    private java.time.LocalDateTime updatedAt;


    @Id
    private String id;

    private String schoolId;

    // References AcademicYear.name (unique per school), e.g. "2026-2027" —
    // scopes this record to one academic year of the school (SaaS: school -> year -> data)
    @Indexed
    private String academicYear;

    private String inquiryId;

    // Applicant snapshot — copied from the linked inquiry, or filled directly for a
    // walk-in/direct admission. Materialised into the Student + Guardians on convert.
    private String studentName;

    private LocalDate dob;

    private Gender gender;

    @Builder.Default
    private List<InquiryGuardian> guardians = new java.util.ArrayList<>();

    // Set only when the admission is converted into an enrolled student.
    private String studentId;

    private AdmissionStatus status;

    private List<String> documents;

    private LocalDate admissionDate;
}