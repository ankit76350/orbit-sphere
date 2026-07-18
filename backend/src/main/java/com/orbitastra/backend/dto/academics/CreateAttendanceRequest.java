package com.orbitastra.backend.dto.academics;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.orbitastra.backend.models.academics.enums.AttendanceStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Client payload for marking attendance. Server-owned fields ({@code id} and
 * audit timestamps) are not accepted from the request body.
 */
@Data
public class CreateAttendanceRequest {

    @NotBlank(message = "schoolId is required")
    private String schoolId;

    private String academicYear;

    @NotBlank(message = "studentId is required")
    private String studentId;

    @NotNull(message = "date is required")
    private LocalDate date;

    @NotNull(message = "status is required")
    private AttendanceStatus status;

    private String presentBy;

    private LocalDateTime presentTime;
}
