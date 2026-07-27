package com.orbitastra.backend.models.undone.gate;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.undone.gate.enums.OutPassStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "outpasses")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OutPass extends SchoolBase {

    // Parent

    // ↓

    // Requests OutPass

    // ↓

    // Principal

    // ↓

    // APPROVED

    // ↓

    // Student reaches gate

    // ↓

    // EXITED

    // ↓

    // Returns

    // ↓

    // RETURNED

    private String studentDocsId;

    private String guardianDocsId;

    private String approvedByDocsId;

    private String reason;

    private LocalDate passDate;

    private LocalTime expectedExitTime;

    private LocalTime expectedReturnTime;

    private LocalTime actualExitTime;

    private LocalTime actualReturnTime;

    private OutPassStatus status;

    private Boolean emergency;

    private String remarks;
}
