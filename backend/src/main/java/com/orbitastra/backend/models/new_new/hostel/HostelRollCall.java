package com.orbitastra.backend.models.new_new.hostel;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.AcademicStudentSchoolBase;
import com.orbitastra.backend.models.new_new.hostel.enums.RollCallStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One child, at one headcount, on one night.
 *
 * <p>A boarding school counts its children every evening, and this is the record of
 * that. It is the single most safety-critical collection in the hostel package: the
 * question it answers is whether every child the school is responsible for overnight is
 * where the school thinks they are.
 *
 * <p>One row per child per roll call, made **in advance** from the ACTIVE allocations,
 * at status UNACCOUNTED. That is deliberate and it is the opposite of how it feels
 * natural to build. If rows were only written when a child answered, a child who never
 * answered would leave no row at all, and the list of children nobody has seen would be
 * empty — which is exactly the list the whole exercise exists to produce.
 *
 * <p>So the warden's job is to turn UNACCOUNTED rows into something else. Anything still
 * UNACCOUNTED when the roll call closes is a child nobody can find, and that has to
 * reach a person within minutes.
 *
 * <p>ON_APPROVED_LEAVE, IN_CLINIC and EXCUSED are kept apart from a plain absence for
 * the same reason. A child who is somewhere the school put them is not missing, and
 * lumping the three together is how a genuinely missing child disappears into a column
 * of absences.
 *
 * <p>{@code sessionCode} names which count this was, because a school may take more than
 * one in a day: an evening count and a lights-out count are different events with
 * different answers.
 *
 * <p>Rows are never edited to tidy up. A child found later is a new status with a time
 * against it, so how long they were unaccounted for stays on the record.
 *
 * <p>The service checks that rows are created for every ACTIVE allocation, that a child
 * on APPROVED leave is pre-marked ON_APPROVED_LEAVE rather than left to be chased, and
 * that a roll call cannot be closed while any row is still UNACCOUNTED without a warden
 * recording what is being done about it.
 */
@Document(collection = "hostel_roll_calls")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_roll_call_student_uniq",
                def = "{'schoolId': 1, 'rollCallDate': 1, 'sessionCode': 1, 'studentDocsId': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_roll_call_unaccounted_idx",
                def = "{'schoolId': 1, 'rollCallDate': -1, 'status': 1}"),
        @CompoundIndex(
                name = "school_roll_call_building_idx",
                def = "{'schoolId': 1, 'hostelBuildingDocsId': 1, 'rollCallDate': -1, 'sessionCode': 1}"),
        @CompoundIndex(
                name = "school_year_student_roll_call_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'studentDocsId': 1, 'rollCallDate': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class HostelRollCall extends AcademicStudentSchoolBase {

    // The evening this count was taken. Example: 2026-08-19
    @NotNull
    private LocalDate rollCallDate;

    // Which count of the day. Example: "LIGHTS_OUT"
    @NotBlank
    private String sessionCode;

    // Links to HostelAllocation.id the row was built from.
    // Example: "67ba1126dc3f7d0055667788"
    @NotBlank
    private String hostelAllocationDocsId;

    // Links to HostelBuilding.id, copied in so a warden's list is one query.
    // Example: "67ba1122dc3f7d0011223344"
    @NotBlank
    private String hostelBuildingDocsId;

    // Links to HostelRoom.id, copied in so the warden knows where to look.
    // Example: "67ba1123dc3f7d0022334455"
    @NotBlank
    private String hostelRoomDocsId;

    // Starts as UNACCOUNTED and is changed as each child is found.
    // Example: RollCallStatus.PRESENT
    @NotNull
    @Builder.Default
    private RollCallStatus status = RollCallStatus.UNACCOUNTED;

    // When this child was accounted for. Null while still UNACCOUNTED.
    // Example: 2026-08-19T15:35:00Z
    private Instant accountedAt;

    // Links to Staff.id for whoever took the count.
    // Example: "67aa15d9dc3f7d0044444444"
    private String recordedByStaffDocsId;

    // Links to HostelLeaveRequest.id when the child is away on approved leave.
    // Example: "67ba1127dc3f7d0066778899"
    private String hostelLeaveRequestDocsId;

    // Links to ClinicVisit.id when the child is in the clinic.
    // Example: "67b71128dc3f7d0077889900"
    private String clinicVisitDocsId;

    // Where the child is, when the status is EXCUSED or the reason needs saying.
    // Example: "At the inter-school match in Pune with the sports teacher."
    private String explanation;

    // What is being done about a child still unaccounted for.
    // Example: "Warden ringing the mother; checked the library and the field."
    private String actionTaken;
}
