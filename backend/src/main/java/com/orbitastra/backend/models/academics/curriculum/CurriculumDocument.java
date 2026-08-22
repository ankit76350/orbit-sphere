package com.orbitastra.backend.models.academics.curriculum;

import java.time.Instant;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.academics.enums.CurriculumDocumentStatus;
import com.orbitastra.backend.models.base.SchoolBase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Lightweight publishing record for one curriculum file prepared by a school
 * department for one class subject.
 *
 * <p>The PDF, image, DOCX, or other file is stored in object storage and
 * represented by DocumentRecord. This collection stores only the business link
 * and publishing state; it does not duplicate file contents or detailed
 * curriculum chapters in MongoDB.
 */
@Document(collection = "curriculum_documents")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_year_class_subject_curriculum_version_uniq",
                def = "{'schoolId': 1, 'academicYear': 1, 'classDocsId': 1, 'subjectCode': 1, 'documentVersion': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_year_class_subject_curriculum_status_idx",
                def = "{'schoolId': 1, 'academicYear': 1, 'classDocsId': 1, 'subjectCode': 1, 'status': 1, 'publishedAt': -1}"),
        @CompoundIndex(
                name = "school_department_curriculum_status_idx",
                def = "{'schoolId': 1, 'departmentDocsId': 1, 'status': 1, 'updatedAt': -1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CurriculumDocument extends SchoolBase {

    // Stores AcademicYear.name. Example: "2026-2027"
    @NotBlank
    private String academicYear;

    // Links to SchoolClass.id. Example: "67aa15d9dc3f7d0011111111"
    @NotBlank
    private String classDocsId;

    // References SchoolClass.subjects[].subjectCode. Example: "MATHEMATICS"
    @NotBlank
    private String subjectCode;

    // Links to the preparing Department.id. Example: "67aa15d9dc3f7d0022222222"
    @NotBlank
    private String departmentDocsId;

    // Display title. Example: "Grade 7 Mathematics Curriculum 2026-2027"
    @NotBlank
    private String title;

    // Links to DocumentRecord.id for the uploaded PDF, image, or DOCX file.
    // Example: "67aa15d9dc3f7d0033333333"
    @NotBlank
    private String documentDocsId;

    // Starts at 1; a replacement file creates the next version. Example: 1
    @NotNull
    @Builder.Default
    private Integer documentVersion = 1;

    // Example: CurriculumDocumentStatus.DRAFT
    @NotNull
    @Builder.Default
    private CurriculumDocumentStatus status = CurriculumDocumentStatus.DRAFT;

    // Set when status becomes PUBLISHED. Example: 2026-04-15T10:30:00Z
    private Instant publishedAt;

    // Links to the identity/account that published the document.
    // Example: "67aa15d9dc3f7d0044444444"
    private String publishedByDocsId;
}
