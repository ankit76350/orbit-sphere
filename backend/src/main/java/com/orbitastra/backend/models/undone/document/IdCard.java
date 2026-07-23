package com.orbitastra.backend.models.undone.document;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.document.enums.IdCardStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "id_cards")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class IdCard extends SchoolBase {

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
