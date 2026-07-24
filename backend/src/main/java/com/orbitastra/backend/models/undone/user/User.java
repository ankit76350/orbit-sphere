package com.orbitastra.backend.models.undone.user;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;


import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.user.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "users")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class User extends SchoolBase {

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    private Role role;


    // References the ID in the corresponding collection:
    // e.g. Staff ID, Student ID, Parent ID, Driver ID
    private String referenceDocsId;

    @Builder.Default
    private boolean active = true;
}
