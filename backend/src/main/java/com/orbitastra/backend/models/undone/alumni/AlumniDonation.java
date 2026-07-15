package com.orbitastra.backend.models.undone.alumni;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "alumni_donations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlumniDonation {
    @org.springframework.data.annotation.CreatedDate
    private java.time.LocalDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    private java.time.LocalDateTime updatedAt;


    @Id
    private String id;

    private String schoolId;

    private String alumniId;

    private String campaignId;

    private BigDecimal amount;

    private LocalDate donationDate;

    private String paymentReference;
}
