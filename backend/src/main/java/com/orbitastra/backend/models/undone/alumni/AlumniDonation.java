package com.orbitastra.backend.models.undone.alumni;

import com.orbitastra.backend.models.BaseDocument;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "alumni_donations")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlumniDonation extends BaseDocument {

    private String alumniId;

    private String campaignId;

    private BigDecimal amount;

    private LocalDate donationDate;

    private String paymentReference;
}
