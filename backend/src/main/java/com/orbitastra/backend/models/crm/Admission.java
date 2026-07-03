package com.orbitastra.backend.models.crm;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.crm.enums.AdmissionStatus;

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

    private String inquiryId;

    private String studentId;

    private AdmissionStatus status;

    private List<String> documents;

    private LocalDate admissionDate;
}