package com.orbitastra.backend.models.new_new.documents;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.documents.enums.DocumentHolderType;
import com.orbitastra.backend.models.new_new.documents.enums.DocumentRequestStatus;
import com.orbitastra.backend.models.new_new.documents.enums.DocumentRequesterType;
import com.orbitastra.backend.models.new_new.documents.enums.DocumentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Somebody asking the school for a piece of paper.
 *
 * <p>The person asking and the person the paper is about are not always the same.
 * A mother asks for a bonafide certificate; the certificate is about her son. So
 * the requester and the holder are two separate pairs of fields, and mixing them
 * up is the mistake this shape is meant to prevent.
 *
 * <p>Not every issued document starts here. The front office can print a
 * certificate on the spot without anybody filling in a request, and a bulk run of
 * four hundred ID cards is not four hundred requests. This model exists for the
 * cases where somebody has to ask and somebody else has to agree, which is why
 * {@code DocumentTemplate.requiresApproval} decides whether it is needed at all.
 *
 * <p>APPROVED and ISSUED are deliberately different. APPROVED means the head said
 * yes; ISSUED means the paper exists and has been handed over. A request sitting
 * at APPROVED for a week is a queue somebody needs to look at, and that would be
 * invisible if the two were one state.
 *
 * <p>{@code issuedDocumentDocsId} is filled in at the end and is the link from
 * "somebody asked" to "here is what they got".
 *
 * <p>The service checks that the requester is allowed to ask about that holder,
 * that a guardian may only ask about their own children, that a rejection carries
 * a reason, and that the approver is not the person who asked.
 */
@Document(collection = "document_requests")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_document_request_no_uniq",
                def = "{'schoolId': 1, 'requestNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_document_request_queue_idx",
                def = "{'schoolId': 1, 'status': 1, 'requestedAt': -1}"),
        @CompoundIndex(
                name = "school_document_request_holder_idx",
                def = "{'schoolId': 1, 'holderType': 1, 'holderDocsId': 1, 'requestedAt': -1}"),
        @CompoundIndex(
                name = "school_document_request_by_idx",
                def = "{'schoolId': 1, 'requestedByDocsId': 1, 'requestedAt': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentRequest extends SchoolBase {

    // School-scoped number from NumberSequence. Example: "DOC/2026/000318"
    @NotBlank
    private String requestNo;

    // What is being asked for. Example: DocumentType.BONAFIDE_CERTIFICATE
    @NotNull
    private DocumentType documentType;

    // Who the paper is about. Example: DocumentHolderType.STUDENT
    @NotNull
    private DocumentHolderType holderType;

    // Links to Student.id, Staff.id or Guardian.id, depending on holderType.
    // Example: "67aa15d9dc3f7d0055555555"
    @NotBlank
    private String holderDocsId;

    // Whether a parent, a student or the office asked.
    // Example: DocumentRequesterType.GUARDIAN
    @NotNull
    private DocumentRequesterType requestedByType;

    // Links to Guardian.id, Student.id or the staff identity, depending on
    // requestedByType. Example: "67aa15d9dc3f7d0066666666"
    @NotBlank
    private String requestedByDocsId;

    // Why they need it. Often required by the office before agreeing.
    // Example: "Needed for a passport application."
    private String purpose;

    // How many printed copies are wanted. Example: 2
    @NotNull
    @Builder.Default
    private Integer copiesRequested = 1;

    // Example: DocumentRequestStatus.SUBMITTED
    @NotNull
    @Builder.Default
    private DocumentRequestStatus status = DocumentRequestStatus.DRAFT;

    // Example: 2026-08-18T05:20:00Z
    @NotNull
    private Instant requestedAt;

    // Date the family needs it by, when they have said. Example: 2026-08-25
    private Instant neededBy;

    // Links to the staff identity that agreed or refused.
    // Example: "67aa15d9dc3f7d0055555555"
    private String reviewedByDocsId;

    // Example: 2026-08-19T09:10:00Z
    private Instant reviewedAt;

    // Note left by the reviewer, and the reason when it is turned down.
    // Example: "Fees for the term are still outstanding."
    private String reviewRemarks;

    // Links to IssuedDocument.id once the paper has been made.
    // Example: "67b51122dc3f7d0011223344"
    private String issuedDocumentDocsId;
}
