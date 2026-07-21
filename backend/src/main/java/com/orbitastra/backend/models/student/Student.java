package com.orbitastra.backend.models.student;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.student.enums.Gender;
import com.orbitastra.backend.models.student.enums.StudentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "students")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Student {
    @org.springframework.data.annotation.CreatedDate
    private java.time.LocalDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    private java.time.LocalDateTime updatedAt;


    @Id
    private String id;

    @Indexed
    private String schoolId;

    @Indexed(unique = true)
    private String admissionNo;



    private String name;

    private LocalDate dob;

    private Gender gender;

    private String bloodGroup;

    private String photoUrl; 

    private String walletId;

    // Many-to-many family links: each entry references a Guardian plus this
    // student's relationship to them (role, primary, emergency, pickup, portal).
    @Builder.Default
    private java.util.List<GuardianLink> guardians = new java.util.ArrayList<>();

    private String medicalRecordId;

    @Builder.Default
    private StudentStatus status = StudentStatus.ACTIVE;

    private LocalDate admissionDate;

    // Denormalised pointer to this student's StudentAcademicRecord (in the
    // "student_academic_records" collection) for the current — i.e. most recent —
    // academic year. Kept in sync by StudentService whenever an academic record is
    // created, assigned or promoted, so the student's active enrolment is one lookup away.
    // The full record itself is exposed only on the API view (StudentResponse), not the entity.
    private String currentAcademicRecordId;


}