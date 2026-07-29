package com.orbitastra.backend.models.undone.a_latter.transport;


import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.academics.enums.AttendanceStatus;
import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.a_latter.transport.enums.AttendanceType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Student transport attendance.
 *
 * One record represents one Pickup or Drop event.
 *
 * Examples:
 *
 * Rahul
 * Morning Pickup
 * BOARDED
 *
 * Rahul
 * Evening Drop
 * DROPPED
 */
@Document(collection = "transport_attendance")
@CompoundIndex(
        name = "student_attendance_idx",
        def = "{'studentDocsId':1,'attendanceDate':1,'attendanceType':1}",
        unique = true
)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TransportAttendance extends SchoolBase {

// Student
//     │
//     ▼
// TransportAttendance
//         │
//         ├────────► RouteAssignment
//         │
//         ├────────► TransportVehicle
//         │
//         ├────────► TransportRoute
//         │
//         └────────► RouteStop

    /**
     * Student.
     */
    @Indexed
    private String studentDocsId;

    /**
     * Route Assignment.
     */
    @Indexed
    private String routeAssignmentDocsId;

    /**
     * Vehicle.
     */
    @Indexed
    private String vehicleDocsId;

    /**
     * Route.
     */
    @Indexed
    private String routeDocsId;

    /**
     * Stop.
     */
    private String stopDocsId;

    /**
     * Pickup or Drop.
     */
    private AttendanceType attendanceType;

    /**
     * Boarding status.
     */
    @Indexed
    private AttendanceStatus status;

    /**
     * Attendance date.
     */
    @Indexed
    private LocalDate attendanceDate;

    /**
     * GPS timestamp when attendance was recorded.
     */
    private LocalDateTime recordedAt;

    /**
     * Driver/Staff who recorded attendance.
     */
    private String recordedByDocsId;

    /**
     * Optional remarks.
     */
    private String remarks;

}