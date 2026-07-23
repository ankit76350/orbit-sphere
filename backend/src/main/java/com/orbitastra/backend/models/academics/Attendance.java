package com.orbitastra.backend.models.academics;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.academics.enums.AttendanceStatus;
import com.orbitastra.backend.models.base.AcadmicStudentBaseDocs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "attendance")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Attendance extends AcadmicStudentBaseDocs {

    private LocalDate date;

    private AttendanceStatus status;

    private String presentBy;

    private LocalDateTime presentTime;
}
