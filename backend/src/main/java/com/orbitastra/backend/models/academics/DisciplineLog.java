package com.orbitastra.backend.models.academics;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "discipline_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisciplineLog {
    @org.springframework.data.annotation.CreatedDate
    private java.time.LocalDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    private java.time.LocalDateTime updatedAt;


    @Id
    private String id;

    @Indexed
    private String schoolId;

    @Indexed
    private String studentId;

    private String violation;

    private BigDecimal fineAmount;

    private String actionTaken;

    private LocalDateTime incidentDate;
}
