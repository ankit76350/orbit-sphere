package com.orbitastra.backend.models.new_new.documents;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.documents.enums.DocumentHolderType;
import com.orbitastra.backend.models.new_new.documents.enums.DocumentType;
import com.orbitastra.backend.models.new_new.documents.enums.IssuedDocumentStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A piece of paper the school has actually given somebody.
 *
 * <p>This is a record of something that left the building, so it is never edited
 * and never deleted. A mistake on a certificate is fixed by issuing a corrected
 * one and marking this one SUPERSEDED, with {@code supersededByDocumentDocsId}
 * pointing at the replacement. The same rule FeeInvoice follows.
 *
 * <p>{@code verificationCode} is what makes a certificate worth more than a PDF.
 * An employer or another school types it into a public page and finds out whether
 * this document is real and still stands. It is stored in plain text on purpose,
 * unlike a password: it is printed on the paper, it is a lookup key rather than a
 * secret, and hashing it would mean the school could never reprint the same
 * certificate again. What protects it is that it is long and random rather than
 * counted upwards, and that the public page gives back only enough to answer the
 * question. Nothing about marks, fees, address or family may appear there.
 *
 * <p>{@code documentNo} is different. It is the school's own reference, counted in
 * order from NumberSequence, and it is what the office quotes internally. Guessing
 * one must not reveal anything, which is why it is not the thing the public check
 * uses.
 *
 * <p>The template key and version are copied in rather than being read through the
 * link, so a reprint years later comes out word for word as it did on the day,
 * even after the template has been reworded twice.
 *
 * <p>{@code dataSnapshotJson} keeps the values that were put into the template.
 * Without it a reprint would rebuild the certificate from today's data, and a
 * transfer certificate issued when a child was in Class VIII would quietly reprint
 * saying Class X.
 *
 * <p>The service checks that an issued document is never edited, that revoking one
 * carries a reason, that a superseded one points at its replacement, and that the
 * public check reveals nothing beyond the holder's name, the document type, the
 * issue date and the status.
 */
@Document(collection = "issued_documents")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_issued_document_no_uniq",
                def = "{'schoolId': 1, 'documentNo': 1}",
                unique = true),
        @CompoundIndex(
                name = "issued_document_verification_uniq",
                def = "{'verificationCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_issued_holder_idx",
                def = "{'schoolId': 1, 'holderType': 1, 'holderDocsId': 1, 'issuedAt': -1}"),
        @CompoundIndex(
                name = "school_issued_type_status_idx",
                def = "{'schoolId': 1, 'documentType': 1, 'status': 1, 'issuedAt': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class IssuedDocument extends SchoolBase {

    // School-scoped number from NumberSequence type CERTIFICATE, counted in
    // order. The office's own reference. Example: "CERT/2026/000742"
    @NotBlank
    private String documentNo;

    // Long random code printed on the paper, used by anybody outside the school
    // to check the document is real. Never counted upwards.
    // Example: "K7M2-9QXP-4T8B-LZ3R"
    @NotBlank
    private String verificationCode;

    // Example: DocumentType.TRANSFER_CERTIFICATE
    @NotNull
    private DocumentType documentType;

    // Who the paper is about. Example: DocumentHolderType.STUDENT
    @NotNull
    private DocumentHolderType holderType;

    // Links to Student.id, Staff.id or Guardian.id, depending on holderType.
    // Example: "67aa15d9dc3f7d0055555555"
    @NotBlank
    private String holderDocsId;

    // Name as it was printed on the paper, copied in so a reprint and the public
    // check both still show what was actually issued. Example: "Arjun Sharma"
    @NotBlank
    private String holderNameSnapshot;

    // Links to AcademicYear.name when the paper is about one year. Null for a
    // document that spans the whole time the person was here.
    // Example: "2026-2027"
    private String academicYear;

    // Links to DocumentTemplate.id it was made from.
    // Example: "67b41122dc3f7d0011223344"
    @NotBlank
    private String documentTemplateDocsId;

    // Template key and version copied in, so a reprint years later matches the
    // original even after the template is reworded. Example: "TC_STANDARD"
    @NotBlank
    private String templateKeySnapshot;

    // Example: 2
    @NotNull
    private Integer templateVersionSnapshot;

    // The values that were put into the template, kept as JSON. Without this a
    // reprint would rebuild the paper from today's data and quietly say something
    // different. Example: "{\"studentName\":\"Arjun Sharma\",\"class\":\"VIII\"}"
    private String dataSnapshotJson;

    // Links to DocumentRecord.id for the finished PDF.
    // Example: "67b41124dc3f7d0033445566"
    private String documentRecordDocsId;

    // Links to DocumentRequest.id when somebody asked for it. Null when the
    // office made it directly. Example: "67b41125dc3f7d0044556677"
    private String documentRequestDocsId;

    // Example: IssuedDocumentStatus.VALID
    @NotNull
    @Builder.Default
    private IssuedDocumentStatus status = IssuedDocumentStatus.VALID;

    // Example: 2026-08-19T10:15:00Z
    @NotNull
    private Instant issuedAt;

    // Links to the staff identity that issued it.
    // Example: "67aa15d9dc3f7d0055555555"
    @NotBlank
    private String issuedByDocsId;

    // Last day the paper is good for. Null when it never runs out, which is
    // normal for a transfer certificate. Example: 2027-02-15
    private LocalDate validUntil;

    // How many printed copies have been handed over. Goes up on a reprint, which
    // never makes a new record. Example: 2
    @NotNull
    @Builder.Default
    private Integer printCount = 1;

    // Links to the IssuedDocument.id that replaced this one.
    // Example: "67b41126dc3f7d0055667788"
    private String supersededByDocumentDocsId;

    // Links to the IssuedDocument.id this one replaced.
    // Example: "67b41127dc3f7d0066778899"
    private String supersedesDocumentDocsId;

    // Example: 2026-09-02T11:00:00Z
    private Instant revokedAt;

    // Links to the staff identity that revoked it.
    // Example: "67aa15d9dc3f7d0055555555"
    private String revokedByDocsId;

    // Needed whenever the status becomes REVOKED or SUPERSEDED.
    // Example: "Date of leaving was wrong; corrected copy issued."
    private String statusReason;
}
