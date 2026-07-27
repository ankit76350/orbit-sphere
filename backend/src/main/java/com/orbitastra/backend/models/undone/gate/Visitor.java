package com.orbitastra.backend.models.undone.gate;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.gate.enums.VisitorStatus;
import com.orbitastra.backend.models.undone.gate.enums.VisitorType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "visitors")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Visitor extends SchoolBase {

    // Visitor

    // ↓

    // Parent

    // ↓

    // Meeting Rahul

    // ↓

    // Checked In

    // ↓

    // Checked Out

    private String visitorName;

    private String mobileNumber;

    private String email;

    private String identityType;

    private String identityNumber;

    private String organization;

    private VisitorType visitorType;

    private String personToMeetDocsId;

    private String studentDocsId;

    private String purpose;

    private LocalDate visitDate;

    private LocalDateTime checkInTime;

    private LocalDateTime checkOutTime;

    private String vehicleNumber;

    private String visitorPhotoUrl;

    private VisitorStatus status;

    private String remarks;
}
