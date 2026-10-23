package com.orbitastra.backend.models.documents;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.documents.enums.DocumentHolderType;
import com.orbitastra.backend.models.documents.enums.DocumentType;
import com.orbitastra.backend.models.documents.enums.IssuedDocumentStatus;

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
 * <p>{@code scanPayload} is the same idea made easier to use. It holds exactly
 * what the QR code on the paper encodes, which is normally the verification web
 * address with {@code verificationCode} on the end. Somebody points a phone at the
 * paper instead of typing sixteen characters, and lands on the same check.
 *
 * <p>It is saved rather than worked out each time, because it records what was
 * physically printed. If the school later moves to a different web address, the
 * QR codes on papers already handed out still point at the old one, and that is
 * only knowable if the old string was kept. The QR picture itself is never saved:
 * it can be drawn again from this string in a moment, and storing an image of
 * something you already hold as text is storing it twice.
 *
 * <p>The template key and version are copied in rather than being read through the
 * link, so a reprint years later comes out word for word as it did on the day,
 * even after the template has been reworded twice.
 *
 * <p>The finished PDF itself is kept, named by {@code documentRecordDocsId}. A
 * reprint hands out that same file again rather than making the paper afresh, so
 * there is nothing to rebuild and no stored copy of the values that went into it.
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

    // Exactly what the QR code on the paper encodes, saved as it was printed.
    // Normally the verification web address with the code on the end. Holds no
    // personal details: whoever scans it still has to ask the school what the
    // document says. The QR picture itself is not saved anywhere, because it can
    // be drawn again from this string whenever it is needed.
    // Example: "https://verify.orbitastra.edu.in/d/K7M2-9QXP-4T8B-LZ3R"
    private String scanPayload;

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
    //! A staff member gets an experience letter in 2026 as Priya Nair. She marries, and the Staff record becomes Priya Menon. In 2028 an employer verifies her 2026 letter.
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

    // Example: 2026-09-02T11:00:00Z
    private Instant revokedAt;

    // Links to the staff identity that revoked it.
    // Example: "67aa15d9dc3f7d0055555555"
    private String revokedByDocsId;

    // Needed whenever the status becomes REVOKED or SUPERSEDED.
    // Example: "Date of leaving was wrong; corrected copy issued."
    private String statusReason;
}
