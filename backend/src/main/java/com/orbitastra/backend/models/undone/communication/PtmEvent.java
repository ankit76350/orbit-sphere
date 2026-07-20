package com.orbitastra.backend.models.undone.communication;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A parent-teacher meeting event with bookable time slots. Parents reserve a
 * per-teacher slot, captured in the embedded {@link Booking} list.
 */
@Document(collection = "ptm_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PtmEvent {

    @CreatedDate
    private java.time.LocalDateTime createdAt;

    @LastModifiedDate
    private java.time.LocalDateTime updatedAt;

    @Id
    private String id;

    @Indexed
    private String schoolId;

    private String title;

    private LocalDate date;

    private LocalTime startTime;

    // Length of each bookable slot, in minutes.
    private Integer slotMinutes;

    private Integer slotCount;

    @Builder.Default
    private List<Booking> bookings = new java.util.ArrayList<>();

    /** A single reserved parent-teacher slot. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Booking {
        private String teacherId;   // references Staff.id
        private Integer slotIndex;  // 0-based slot within the event
        private String studentName;
        private String parentName;
    }
}
