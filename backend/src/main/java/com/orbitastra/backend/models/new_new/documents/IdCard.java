package com.orbitastra.backend.models.new_new.documents;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.documents.enums.DocumentHolderType;
import com.orbitastra.backend.models.new_new.documents.enums.IdCardStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One identity card the school has printed for somebody.
 *
 * <p>A card is not a certificate and is kept apart from IssuedDocument on purpose.
 * A certificate is a statement about the past that never changes once given. A
 * card is a thing somebody carries: it has a photo, it expires, it gets lost, it
 * gets replaced, and it opens gates. None of that fits a certificate, and forcing
 * both into one model would leave most of the fields empty most of the time.
 *
 * <p>{@code rfidNumber} is what makes the card work on a bus. Transport already
 * expects it: BoardingCaptureMethod.RFID_CARD assumes something issued the card
 * that gets tapped. This is that something. A card that is not ACTIVE must stop
 * being accepted straight away, which is why the status is checked at the reader
 * rather than the card simply being trusted.
 *
 * <p>A lost card is the case that matters most. The old row stays LOST rather than
 * being edited into the new one, so a card somebody else is holding is on record
 * as no longer valid. The replacement is a new row pointing back through
 * {@code replacesCardDocsId}, and the chain of replacements is then readable.
 *
 * <p>{@code issueNumber} counts how many cards this person has had. A child on
 * their fourth card in a year is a conversation, and a school that charges for
 * replacements needs the count to charge from.
 *
 * <p>Only one card per person may be ACTIVE at a time, which the unique index
 * enforces.
 *
 * <p>The service checks that a lost or damaged card is closed before a
 * replacement is issued, that the photo belongs to the person on the card, and
 * that an expired card is not accepted by a reader whatever its status says.
 */
@Document(collection = "id_cards")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_id_card_no_uniq",
                def = "{'schoolId': 1, 'cardNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_id_card_holder_active_uniq",
                def = "{'schoolId': 1, 'holderType': 1, 'holderDocsId': 1}",
                unique = true,
                partialFilter = "{'status': 'ACTIVE'}"),
        @CompoundIndex(
                name = "school_id_card_rfid_uniq",
                def = "{'schoolId': 1, 'rfidNumber': 1}",
                unique = true,
                partialFilter = "{'rfidNumber': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_id_card_status_idx",
                def = "{'schoolId': 1, 'status': 1, 'validUntil': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class IdCard extends SchoolBase {

    // School-scoped number from NumberSequence type ID_CARD, printed on the card.
    // Example: "IDC/2026/001204"
    @NotBlank
    private String cardNo;

    // Who the card is for. Example: DocumentHolderType.STUDENT
    @NotNull
    private DocumentHolderType holderType;

    // Links to Student.id, Staff.id or Guardian.id, depending on holderType.
    // Example: "67aa15d9dc3f7d0055555555"
    @NotBlank
    private String holderDocsId;

    // Links to AcademicYear.name the card was issued for. Example: "2026-2027"
    private String academicYear;

    // Links to DocumentTemplate.id for the card design.
    // Example: "67b41128dc3f7d0077889900"
    private String documentTemplateDocsId;

    // Links to DocumentRecord.id for the photo printed on the card.
    // Example: "67b41129dc3f7d0088990011"
    private String photoDocumentDocsId;

    // Number in the card's chip or magnetic strip, tapped on a bus or at a gate.
    // Null for a printed card with no chip. Example: "04A2B7C9D1"
    private String rfidNumber;

    // What a scanner reads from the code printed on the card. Holds no personal
    // details, only enough to look the card up. Example: "IDC/2026/001204"
    private String scanPayload;

    // Day the card was handed over. Example: 2026-04-10
    @NotNull
    private LocalDate issuedDate;

    // Last day the card works. Example: 2027-03-31
    private LocalDate validUntil;

    // Example: IdCardStatus.ACTIVE
    @NotNull
    @Builder.Default
    private IdCardStatus status = IdCardStatus.ACTIVE;

    // How many cards this person has had, starting at 1. A high number is worth
    // somebody looking at, and is what a replacement charge is based on.
    // Example: 2
    @NotNull
    @Builder.Default
    private Integer issueNumber = 1;

    // Links to the IdCard.id this one replaced.
    // Example: "67b4112adc3f7d0099001122"
    private String replacesCardDocsId;

    // Links to DocumentRecord.id for the printed card file.
    // Example: "67b4112bdc3f7d0000112233"
    private String documentRecordDocsId;

    // Links to the staff identity that issued the card.
    // Example: "67aa15d9dc3f7d0055555555"
    private String issuedByDocsId;

    // When the card was reported lost, damaged or taken back.
    // Example: 2026-11-02T07:30:00Z
    private Instant statusChangedAt;

    // Needed whenever the status is not ACTIVE.
    // Example: "Reported lost on the school bus on 2 November."
    private String statusReason;
}
