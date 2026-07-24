package com.orbitastra.backend.models.undone.transport;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "transport_attendance")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TransportAttendance extends SchoolBase {

    private String studentDocsId;

    private String studentName;

    private String routeDocsId;

    private String stopName;

    private LocalDate date;

    private String type; // pickup, drop

    private String status; // boarded, missed, absent

    private LocalDateTime recordedAt;
}
