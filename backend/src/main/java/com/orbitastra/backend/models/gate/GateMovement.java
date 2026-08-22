package com.orbitastra.backend.models.gate;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.gate.enums.MovementDirection;
import com.orbitastra.backend.models.gate.enums.MovementExceptionType;
import com.orbitastra.backend.models.gate.enums.MovementSubjectType;
import com.orbitastra.backend.models.common.enums.IdentificationMethod;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One person or vehicle going in or out through one gate, at one moment.
 *
 * <p>This is a log. Rows are added and never changed, because a record of who was
 * where at what time is worth nothing if it can be tidied up afterwards. A wrong
 * entry is corrected by adding the right one and explaining it in
 * {@code remarks}, never by editing the wrong one away.
 *
 * <p>One log covers students, staff, visitors and vehicles, told apart by
 * {@code subjectType}. Separate logs per kind of person would make the one
 * question that matters impossible to answer: who is inside the school right now.
 * That answer needs everybody in one place, ordered by time.
 *
 * <p>{@code verificationMethod} says how much the row is worth. A card tap
 * happened at a moment the system saw for itself. MANUAL is a guard typing a name,
 * which is fine but is somebody's word. When a parent argues about what time their
 * child left, this field is the difference between evidence and a recollection.
 *
 * <p>{@code visitorPassDocsId} and {@code studentOutPassDocsId} link a movement
 * back to the permission that allowed it. A child going out through the gate on a
 * school day should have an out pass behind it, and a movement with neither is
 * worth a look.
 *
 * <p>This is not attendance. A child scanning in at the gate has arrived at the
 * school; whether they were in the classroom is a different question with a
 * different answer, and it lives in the attendance models. The gate log may feed
 * attendance, but it must never quietly become it: a child can be inside the
 * building and still absent from a lesson.
 *
 * <p>{@code movementDate} repeats the date part of {@code occurredAt} on purpose,
 * so a day's movements can be read with a plain equality match instead of a range,
 * which is the query the gate screen runs all day long.
 *
 * <p>{@code exceptionType} is how the log stays honest. A child who walks out with
 * no pass is written down and marked, never refused: refusing the row would leave
 * no record that they left, which is the opposite of what this collection is for.
 * The mark is what puts the row on a list somebody reads.
 *
 * <p>The service checks that the gate is active, that a subject going OUT has a
 * matching IN earlier the same day where the school expects one, and that rows are
 * never edited or deleted.
 */
@Document(collection = "gate_movements")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_movement_day_idx",
                def = "{'schoolId': 1, 'movementDate': -1, 'occurredAt': -1}"),
        @CompoundIndex(
                name = "school_movement_subject_idx",
                def = "{'schoolId': 1, 'subjectType': 1, 'subjectDocsId': 1, 'occurredAt': -1}"),
        @CompoundIndex(
                name = "school_movement_gate_idx",
                def = "{'schoolId': 1, 'gateDocsId': 1, 'occurredAt': -1}"),
        @CompoundIndex(
                name = "school_movement_direction_idx",
                def = "{'schoolId': 1, 'movementDate': -1, 'direction': 1, 'subjectType': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class GateMovement extends SchoolBase {

    // Which way they went. Example: MovementDirection.IN
    @NotNull
    private MovementDirection direction;

    // What kind of person or thing this was.
    // Example: MovementSubjectType.STUDENT
    @NotNull
    private MovementSubjectType subjectType;

    // Links to Student.id, Staff.id, Visitor.id or TransportVehicle.id, depending
    // on subjectType. Example: "67aa15d9dc3f7d0055555555"
    @NotBlank
    private String subjectDocsId;

    // Links to Gate.id. Example: "67b61124dc3f7d0033445566"
    @NotBlank
    private String gateDocsId;

    // The exact moment. Example: 2026-08-19T02:41:00Z
    @NotNull
    private Instant occurredAt;

    // The date part of occurredAt, repeated so a day's log reads with a plain
    // match instead of a range. Example: 2026-08-19
    @NotNull
    private LocalDate movementDate;

    // How the person was identified, and therefore how much this row is worth.
    // Example: IdentificationMethod.RFID_TAP
    @NotNull
    private IdentificationMethod verificationMethod;

    // Links to IdCard.id when a card was used. Example: "67b4112cdc3f7d0011223344"
    private String idCardDocsId;

    // Links to VisitorPass.id when this movement was a visitor arriving or
    // leaving. Example: "67b61125dc3f7d0044556677"
    private String visitorPassDocsId;

    // Links to StudentOutPass.id when a child was let out early on a pass.
    // Example: "67b61126dc3f7d0055667788"
    private String studentOutPassDocsId;

    // Links to the staff identity on the gate, when a person recorded it.
    // Example: "67aa15d9dc3f7d0066666666"
    private String recordedByDocsId;

    // Name of the reader or camera that recorded it, when a device did.
    // Example: "READER-MAIN-IN-01"
    private String deviceReference;

    // Set when something was not right, such as a card that did not match or a
    // child leaving with no pass. The movement is still written; it is marked so
    // it lands on a list somebody looks at.
    // Example: MovementExceptionType.NO_OUT_PASS
    private MovementExceptionType exceptionType;

    // Anything worth knowing, and required whenever exceptionType is OTHER or
    // MANUAL_CORRECTION.
    // Example: "Card left at home; identity confirmed by the class teacher."
    private String remarks;
}
