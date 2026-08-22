package com.orbitastra.backend.models.new_new.gate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.gate.enums.VisitorPassStatus;
import com.orbitastra.backend.models.new_new.gate.enums.VisitorType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One visit by one person on one day.
 *
 * <p>The Visitor is the person; this is the visit. A vendor who comes weekly has
 * one Visitor record and one of these for every trip.
 *
 * <p>Every pass sitting at CHECKED_IN is somebody still inside the school. That
 * list is the reason this model exists, and it is what gets read out during a fire
 * drill. A pass left at CHECKED_IN overnight is either somebody nobody checked out
 * or a guard who forgot, and either way it needs looking at.
 *
 * <p>{@code hostStaffDocsId} is who agreed to see them, and it is required. A
 * visitor with nobody expecting them is somebody who talked their way in. When a
 * visit is about children, {@code studentDocsIds} says which ones, so "who came to
 * see my son and when" has an answer.
 *
 * <p>{@code studentDocsIds} is a list because one visitor often comes about more
 * than one child at a time. A parent with three children here attends one parents'
 * evening, not three, and an uncle collecting two brothers makes one trip. Making
 * the guard issue a pass per child would mean three badges for one person standing
 * at the gate, and three rows to check out afterwards.
 *
 * <p>{@code scanPayload} works the same way as the one on an ID card, and for the
 * same reason: a meaningless random token, never a web address, so a badge dropped
 * in a car park tells a stranger nothing. Only a signed-in member of staff can turn
 * it into a name. The QR picture is drawn from this string and never saved.
 *
 * <p>A pass may be booked ahead as EXPECTED, which is how a school handles an
 * interview day or a parents' evening without a queue at the gate. It may also be
 * created on the spot when somebody simply arrives.
 *
 * <p>The service checks that a blocked visitor is never given a pass, that the host
 * is a current member of staff, that a pass is not checked out before it is checked
 * in, and that the gate used allows visitors.
 */
@Document(collection = "visitor_passes")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_visitor_pass_no_uniq",
                def = "{'schoolId': 1, 'passNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "visitor_pass_scan_payload_uniq",
                def = "{'scanPayload': 1}",
                unique = true,
                partialFilter = "{'scanPayload': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_visitor_pass_inside_idx",
                def = "{'schoolId': 1, 'status': 1, 'visitDate': -1}"),
        @CompoundIndex(
                name = "school_visitor_pass_visitor_idx",
                def = "{'schoolId': 1, 'visitorDocsId': 1, 'visitDate': -1}"),
        @CompoundIndex(
                name = "school_visitor_pass_host_idx",
                def = "{'schoolId': 1, 'hostStaffDocsId': 1, 'visitDate': -1}"),
        @CompoundIndex(
                name = "school_visitor_pass_student_idx",
                def = "{'schoolId': 1, 'studentDocsIds': 1, 'visitDate': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class VisitorPass extends SchoolBase {

    // School-scoped number from NumberSequence type VISITOR_PASS, printed on the
    // badge. Example: "VP/2026/004821"
    @NotBlank
    private String passNo;

    // Links to Visitor.id. Example: "67b61123dc3f7d0022334455"
    @NotBlank
    private String visitorDocsId;

    // What sort of visit this one is. May differ from the visitor's usual type.
    // Example: VisitorType.VENDOR
    @NotNull
    private VisitorType visitorType;

    // Why they are here, in the visitor's own words or the guard's.
    // Example: "Delivering the term's exam stationery."
    @NotBlank
    private String purpose;

    // Links to Staff.id for the person who agreed to see them. Required: a
    // visitor nobody is expecting is a visitor who talked their way in.
    // Example: "67aa15d9dc3f7d0044444444"
    @NotBlank
    private String hostStaffDocsId;

    // Links to Student.id for every child this visit is about. A list because one
    // visitor often comes about more than one child at once, such as a parent of
    // three attending one parents' evening. Empty when the visit has nothing to do
    // with any particular child, which is normal for a vendor or a courier.
    // Example: ["67aa15d9dc3f7d0055555555", "67aa15d9dc3f7d0055555666"]
    @Builder.Default
    private List<String> studentDocsIds = new ArrayList<>();

    // The day of the visit. Example: 2026-08-19
    @NotNull
    private LocalDate visitDate;

    // Example: VisitorPassStatus.CHECKED_IN
    @NotNull
    @Builder.Default
    private VisitorPassStatus status = VisitorPassStatus.EXPECTED;

    // Random token the badge's QR code encodes. Never a web address and never the
    // pass number, so a dropped badge tells a stranger nothing. The QR picture is
    // drawn from this and not saved. Example: "b3d9c7f1a24e86055ac2"
    private String scanPayload;

    // When they were expected, for a visit booked in advance.
    // Example: 2026-08-19T04:30:00Z
    private Instant expectedArrivalAt;

    // When they actually arrived. Example: 2026-08-19T04:42:00Z
    private Instant checkedInAt;

    // When they left. Null while they are still inside.
    // Example: 2026-08-19T05:20:00Z
    private Instant checkedOutAt;

    // Links to Gate.id they came in through.
    // Example: "67b61124dc3f7d0033445566"
    private String entryGateDocsId;

    // Links to Gate.id they left through, which may be a different one.
    // Example: "67b61124dc3f7d0033445566"
    private String exitGateDocsId;

    // Vehicle they came in, written down so it can be matched at the gate.
    // Example: "MH 02 CD 5678"
    private String vehicleNumber;

    // Where in the school they are allowed to go, written for the guard.
    // Example: "Reception and the stationery store only."
    private String accessNote;

    // Links to the staff identity that let them in.
    // Example: "67aa15d9dc3f7d0066666666"
    private String checkedInByDocsId;

    // Links to the staff identity that checked them out.
    // Example: "67aa15d9dc3f7d0066666666"
    private String checkedOutByDocsId;

    // Anything worth knowing about this visit.
    // Example: "Asked to wait in reception; head was in a meeting."
    private String remarks;
}
