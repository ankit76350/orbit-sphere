package com.orbitastra.backend.dto.academics;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.orbitastra.backend.models.academics.enums.AttendanceStatus;

import lombok.Data;

/**
 * Partial-update payload for an attendance record (PATCH). All fields optional;
 * only non-null fields are applied. {@code academicYear} is accepted only so the
 * service can reject an attempt to change it.
 */
@Data
public class UpdateAttendanceRequest {

    private String schoolId;

    private String academicYear;

    private String studentId;

    private LocalDate date;

    private AttendanceStatus status;

    private String presentBy;

    private LocalDateTime presentTime;
}
