package com.orbitastra.backend.models.student;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.BaseDocument;
import com.orbitastra.backend.models.student.enums.Gender;
import com.orbitastra.backend.models.student.enums.StudentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "students")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Student extends BaseDocument {

    @Indexed(unique = true)
    private String admissionNo;

    /** MongoDB document id of the admission this student was converted from. */
    @Indexed(unique = true, sparse = true)
    private String admissionDocsId;

    private String name;
    private LocalDate dob;
    private Gender gender;

    // Many-to-many family links: each entry references a Guardian plus this
    // student's relationship to them (role, primary, emergency, pickup, portal).
    @Builder.Default
    private List<GuardianLink> guardians = new java.util.ArrayList<>();

    @Builder.Default
    private List<String> documents = new java.util.ArrayList<>();

    private LocalDate admissionDate;

    @Builder.Default
    private StudentStatus status = StudentStatus.ACTIVE;

    private String photoUrl;
    private String bloodGroup;
    private String medicalRecordDocsId;

    @Builder.Default
    private List<String> medicalRemark = new java.util.ArrayList<>();

    private String walletDocsId;

    // Denormalised pointer to this student's StudentAcademicRecord (in the
    // "student_academic_records" collection) for the current — i.e. most recent —
    // academic year. Kept in sync by StudentService whenever an academic record is
    // created, assigned or promoted, so the student's active enrolment is one lookup away.
    // The full record itself is exposed only on the API view (StudentResponse), not the entity.
    private String currentAcademicRecordDocsId;
}
