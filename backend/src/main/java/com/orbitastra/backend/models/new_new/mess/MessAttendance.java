package com.orbitastra.backend.models.new_new.mess;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.AcademicStudentSchoolBase;
import com.orbitastra.backend.models.new_new.common.enums.IdentificationMethod;
import com.orbitastra.backend.models.new_new.mess.enums.MealAttendanceStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One child at one meal.
 *
 * <p>**This does not drive billing.** Mess charges are a fixed monthly amount on the
 * hostel allocation, so a child who skips breakfast is not refunded and a child who eats
 * twice is not charged twice. This collection exists so the kitchen knows how much to
 * cook and so a warden can see that a child has been missing meals.
 *
 * <p>That is a deliberate choice. Per-meal billing means every single meal has to be
 * captured accurately or a family is overcharged, and a card reader that misses a tap
 * becomes a billing dispute rather than a rough headcount. A fixed monthly charge with an
 * approximate count is the trade almost every boarding school actually makes.
 *
 * <p>A child missing several meals in a row is worth a warden looking at, which is the
 * second reason to keep this. It is often the first sign that something is wrong, and it
 * shows up here before it shows up anywhere else.
 *
 * <p>{@code identificationMethod} says how much the row is worth, the same as at a gate
 * or on a bus. A card tap at the mess door happened; a warden ticking a list afterwards
 * is somebody's memory.
 *
 * <p>The service checks that the child had an ACTIVE hostel allocation that day, that a
 * child on approved leave is recorded ON_LEAVE rather than absent, and that the meal
 * matches a MessMealType whose serving window contains the tap.
 */
@Document(collection = "mess_attendance")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_mess_attendance_uniq",
                def = "{'schoolId': 1, 'mealDate': 1, 'messMealTypeDocsId': 1, 'studentDocsId': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_mess_attendance_count_idx",
                def = "{'schoolId': 1, 'mealDate': 1, 'messHallDocsId': 1, 'status': 1}"),
        @CompoundIndex(
                name = "school_year_student_mess_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'studentDocsId': 1, 'mealDate': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MessAttendance extends AcademicStudentSchoolBase {

    // The day of the meal. Example: 2026-08-19
    @NotNull
    private LocalDate mealDate;

    // Links to MessMealType.id. Example: "67bb1122dc3f7d0011223344"
    @NotBlank
    private String messMealTypeDocsId;

    // Links to MessHall.id. Example: "67bb1123dc3f7d0022334455"
    @NotBlank
    private String messHallDocsId;

    // Links to HostelAllocation.id this row was built from.
    // Example: "67ba1126dc3f7d0055667788"
    @NotBlank
    private String hostelAllocationDocsId;

    // Example: MealAttendanceStatus.PRESENT
    @NotNull
    @Builder.Default
    private MealAttendanceStatus status = MealAttendanceStatus.ABSENT;

    // When they were served. Example: 2026-08-19T02:05:00Z
    private Instant servedAt;

    // How it was recorded, and therefore how much this row is worth.
    // Example: IdentificationMethod.RFID_TAP
    private IdentificationMethod identificationMethod;

    // Links to Staff.id for whoever marked it, when it was done by hand.
    // Example: "67aa15d9dc3f7d0044444444"
    private String recordedByStaffDocsId;

    // Example: "Took a packed breakfast before the 6am match bus."
    private String remarks;
}
