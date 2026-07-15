package com.orbitastra.backend.models.undone.crm;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.crm.enums.AdmissionStatus;

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

    private String studentId;

    private AdmissionStatus status;

    private List<String> documents;

    private LocalDate admissionDate;
}