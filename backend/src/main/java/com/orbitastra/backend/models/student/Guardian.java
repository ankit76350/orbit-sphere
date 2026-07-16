package com.orbitastra.backend.models.student;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single guardian/contact person (father, mother, grandparent, legal guardian…).
 *
 * A Guardian is one real person, deduplicated per school. The student ↔ guardian
 * relationship is many-to-many: one guardian can be linked to several students
 * (siblings), and one student can have several guardians — the link, with its role
 * and flags, lives in {@link GuardianLink} embedded on the {@link Student}.
 *
 * Uniqueness is enforced two ways so the same person is never stored twice:
 * the service does find-or-reuse (name + phone, or name + email), and these
 * partial unique indexes are the DB backstop.
 */
@Document(collection = "guardians")
@CompoundIndexes({
    @CompoundIndex(name = "guardian_school_name_phone_uniq", def = "{'schoolId': 1, 'name': 1, 'phone': 1}",
            unique = true, partialFilter = "{'phone': {'$type': 'string'}}"),
    @CompoundIndex(name = "guardian_school_name_email_uniq", def = "{'schoolId': 1, 'name': 1, 'email': 1}",
            unique = true, partialFilter = "{'email': {'$type': 'string'}}")
})
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
