package com.orbitastra.backend.models.undone.document;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.document.enums.IdCardStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "id_cards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdCard {
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

    private String cardType; // Student, Staff, Parent

    private String holderId;

    private String holderName;

    private String details;

    private String qrCode;

    private String barcode;

    private LocalDate issuedDate;

    private LocalDate expiryDate;

    private IdCardStatus status;
}
