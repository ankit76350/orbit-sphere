package com.orbitastra.backend.models.undone.transport;

import com.orbitastra.backend.models.BaseDocument;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "transport_allocations")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TransportAllocation extends BaseDocument {

    // References AcademicYear.name (unique per school), e.g. "2026-2027" —
    // scopes this record to one academic year of the school (SaaS: school -> year -> data)
    @Indexed
    private String academicYear;

    private String studentId;

    private String studentName;

    private String routeId;

    private String pickupStopName;

    private String dropStopName;

    private BigDecimal feeAmount;

    private LocalDate startDate;

    private String status;
}
