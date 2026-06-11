package com.orbitastra.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "student_wallets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentWallet {

    @Id
    private String id;

    private String schoolId;

    private String studentId;

    private BigDecimal balance;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
