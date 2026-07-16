package com.orbitastra.backend.models.student;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single guardian/contact person (father, mother, grandparent, legal guardian…).
 *
 * Unlike the legacy {@link Parent} (which crammed both parents into one row), a
 * Guardian is one real person. The student ↔ guardian relationship is many-to-many:
 * one guardian can be linked to several students (siblings), and one student can
 * have several guardians — the link, with its role and flags, lives in
 * {@link GuardianLink} embedded on the {@link Student}.
 */
@Document(collection = "guardians")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Guardian {
    @org.springframework.data.annotation.CreatedDate
    private java.time.LocalDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    private java.time.LocalDateTime updatedAt;


    @Id
    private String id;

    @Indexed
    private String schoolId;

    private String name;

    private String phone;

    private String alternatePhone;

    private String email;

    private String address;

    private String occupation;
}
