package com.orbitastra.backend.models.new_new.documents;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.documents.enums.DocumentTemplateStatus;
import com.orbitastra.backend.models.new_new.documents.enums.DocumentType;
import com.orbitastra.backend.models.new_new.documents.enums.TemplateRendererType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The wording and layout the school uses for one kind of paper.
 *
 * <p>A template is never edited once a document has been made from it. A change
 * makes {@code templateVersion + 1} and marks the old one SUPERSEDED, the same way
 * FeeStructure handles a mid-year fee change. This matters because a transfer
 * certificate reprinted in 2030 has to come out word for word as it did in 2026,
 * and it will not if somebody reworded the template in between.
 *
 * <p>Where the wording lives depends on {@code rendererType}. HTML keeps it inline
 * in {@code bodyTemplate}, which is the easy case and the one a school can edit
 * for itself. DOCX and PDF_FORM keep an uploaded file instead, named by
 * {@code templateFileDocsId}, because those are designed in Word or a PDF editor
 * and cannot sensibly be typed into a text box.
 *
 * <p>{@code placeholderKeys} lists what the template expects to be given, such as
 * the student's name or their date of admission. It is written down so the school
 * can be told which placeholder is missing before a batch of four hundred
 * certificates comes out with a blank in the middle.
 *
 * <p>{@code requiresApproval} is the difference between a bonafide certificate,
 * which the front office can hand over on the spot, and a transfer certificate,
 * which usually needs the head to agree first.
 *
 * <p>The service checks that a template with documents issued from it is never
 * edited, that only one version per key is ACTIVE at a time, and that the file or
 * the body is present to match the renderer.
 */
@Document(collection = "document_templates")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_template_key_version_uniq",
                def = "{'schoolId': 1, 'templateKey': 1, 'templateVersion': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_template_active_idx",
                def = "{'schoolId': 1, 'documentType': 1, 'status': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTemplate extends SchoolBase {

    // Stable key that stays the same across versions. Example: "TC_STANDARD"
    @NotBlank
    private String templateKey;

    // Goes up by one each time the wording changes. Example: 2
    @NotNull
    @Builder.Default
    private Integer templateVersion = 1;

    // Name staff pick from. Example: "Transfer Certificate (CBSE format)"
    @NotBlank
    private String name;

    // What kind of paper this makes. Example: DocumentType.TRANSFER_CERTIFICATE
    @NotNull
    private DocumentType documentType;

    // How the finished file is made. Example: TemplateRendererType.HTML
    @NotNull
    private TemplateRendererType rendererType;

    // The wording itself, with placeholders in it. Used when the renderer is
    // HTML. Example: "<h1>Transfer Certificate</h1><p>{{studentName}} ...</p>"
    private String bodyTemplate;

    // Links to DocumentRecord.id for an uploaded Word or PDF design. Used when
    // the renderer is DOCX or PDF_FORM. Example: "67b41122dc3f7d0011223344"
    private String templateFileDocsId;

    // What the template expects to be given, so a missing one can be caught
    // before printing. Example: "studentName,admissionNo,dateOfLeaving"
    private String placeholderKeys;

    // Language this version is worded in. Example: "en-IN"
    private String locale;

    // Whether somebody has to approve each request before the paper is made.
    // Example: true
    @NotNull
    @Builder.Default
    private Boolean requiresApproval = false;

    // Whether a document made from this template gets a code somebody outside
    // the school can check. Example: true
    @NotNull
    @Builder.Default
    private Boolean verifiable = true;

    // How long the paper is good for, in days. Null means it never runs out,
    // which is normal for a transfer certificate. Example: 180
    private Integer validityDays;

    // Example: DocumentTemplateStatus.ACTIVE
    @NotNull
    @Builder.Default
    private DocumentTemplateStatus status = DocumentTemplateStatus.DRAFT;

    // First day this version may be used. Example: 2026-04-01
    private LocalDate effectiveFrom;

    // Last day it may be used. Example: 2027-03-31
    private LocalDate effectiveTo;

    // Links to the DocumentTemplate.id this version replaced.
    // Example: "67b41123dc3f7d0022334455"
    private String supersedesTemplateDocsId;

    // Links to the staff identity that approved the wording.
    // Example: "67aa15d9dc3f7d0055555555"
    private String approvedByDocsId;

    // Example: "Reworded in 2026 to match the new board format."
    private String remarks;
}
