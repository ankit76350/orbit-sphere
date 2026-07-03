package com.orbitastra.backend.models.transport;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "transport_allocations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportAllocation {
    @org.springframework.data.annotation.CreatedDate
    private java.time.LocalDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    private java.time.LocalDateTime updatedAt;


    @Id
    private String id;

    private String schoolId;

    private String studentId;

    private String studentName;

    private String routeId;

    private String pickupStopName;

    private String dropStopName;

    private BigDecimal feeAmount;

    private LocalDate startDate;

    private String status;
}
