package com.orbitastra.backend.models.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.finance.FeeStatus;
import com.orbitastra.backend.models.finance.FeeType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "fees")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Fee {

    @Id
    private String id;

    private String schoolId;

    private String studentId;

    private FeeType type;

    private BigDecimal amount;

    private BigDecimal paidAmount;

    private LocalDate dueDate;

    private FeeStatus status;
}
