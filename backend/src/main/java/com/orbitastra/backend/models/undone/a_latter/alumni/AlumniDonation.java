package com.orbitastra.backend.models.undone.a_latter.alumni;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "alumni_donations")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlumniDonation extends SchoolBase {

    private String alumniDocsId;

    private String campaignDocsId;

    private BigDecimal amount;

    private LocalDate donationDate;

    private String paymentReference;
}
