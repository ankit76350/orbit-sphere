package com.orbitastra.backend.model;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.model.enums.IdCardStatus;

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

    @Id
    private String id;

    private String schoolId;

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
