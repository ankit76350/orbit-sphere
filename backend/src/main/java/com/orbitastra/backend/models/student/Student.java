package com.orbitastra.backend.models.student;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
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

    @Transient
    private StudentAcademicRecord currentAcademicRecord;

    private String firstName;

    private String lastName;

    private LocalDate dob;

    private Gender gender;

    private String bloodGroup;

    private String photoUrl; 

    private String walletId;

    private String parentId;

    private String medicalRecordId;

    @Builder.Default
    private StudentStatus status = StudentStatus.ACTIVE;

    private LocalDate admissionDate;


}