package com.orbitastra.backend.models.staff;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "staffs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Staff {
    @org.springframework.data.annotation.CreatedDate
    private java.time.LocalDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    private java.time.LocalDateTime updatedAt;


    @Id
    private String id;

    private String schoolId;

    private String employeeId;

    private String name;

    private String department;

    private String designation;

    private BigDecimal salary;

    private LocalDate joiningDate;

    private LocalDate dob;
}