package com.orbitastra.backend.model;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "drivers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Driver {

    @Id
    private String id;

    private String schoolId;

    private String name;

    private String phone;

    private String licenseNo;

    private LocalDate licenseExpiry;

    private String vehicleId;
}
