package com.orbitastra.backend.models.old.academics;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.AcademicStudentSchoolBase;
import com.orbitastra.backend.models.old.academics.enums.AttendanceStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "attendance")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Attendance extends AcademicStudentSchoolBase {

    /** MongoDB id of the student's current year-specific academic record. */
    private String currentAcademicRecordDocsId;

    private LocalDate date;

    private AttendanceStatus status;

    /** MongoDB id of the staff member who recorded the attendance. */
    private String presentByDocsId;

    private LocalDateTime presentTime;
}
