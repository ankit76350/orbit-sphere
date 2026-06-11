package com.orbitastra.backend.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.model.enums.Role;

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

    @Id
    private String id;

    private String schoolId;

    private String employeeId;

    private String name;

    private String department;

    private String designation;

    private BigDecimal salary;

    private LocalDate joiningDate;

    private Role role;

    private LocalDate dob;
}