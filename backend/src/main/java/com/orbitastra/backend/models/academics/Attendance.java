package com.orbitastra.backend.models.academics;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.academics.enums.AttendanceStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "attendance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attendance {

    @Id
    private String id;

    @Indexed
    private String schoolId;

    @Indexed
    private String studentId;

    private LocalDate date;

    private AttendanceStatus status;

    private String presentBy;

    private LocalDateTime presentTime;
}
