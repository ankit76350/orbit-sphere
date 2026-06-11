package com.orbitastra.backend.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.model.enums.Gender;
import com.orbitastra.backend.model.enums.StudentStatus;

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

    @Id
    private String id;

    private String schoolId;

    @Indexed(unique = true)
    private String admissionNo;

    private String studentId;

    private String rollNo;

    private String firstName;

    private String lastName;

    private LocalDate dob;

    private Gender gender;

    private String bloodGroup;

    private String photoUrl;

    private String classId;

    private String sectionId;

    private String hostelRoomId;

    private String walletId;

    private String parentId;

    private String medicalRecordId;

    @Builder.Default
    private StudentStatus status = StudentStatus.ACTIVE;

    private LocalDate admissionDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}