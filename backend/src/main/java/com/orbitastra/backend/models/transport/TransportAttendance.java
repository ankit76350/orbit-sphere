package com.orbitastra.backend.models.transport;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "transport_attendance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportAttendance {

    @Id
    private String id;

    private String schoolId;

    private String studentId;

    private String studentName;

    private String routeId;

    private String stopName;

    private LocalDate date;

    private String type; // pickup, drop

    private String status; // boarded, missed, absent

    private LocalDateTime recordedAt;
}
