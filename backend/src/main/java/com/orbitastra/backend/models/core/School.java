package com.orbitastra.backend.models.core;

import com.orbitastra.backend.models.BaseDocument;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.core.enums.SubscriptionTier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "schools")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class School extends BaseDocument {

    private String schoolName;

    @Indexed(unique = true)
    private String subdomain;

    private String logo;

    private String address;

    private String phone;

    private String email;

    private SubscriptionTier subscriptionTier;

    private Integer maxStudents;

    private Integer maxUsers;

    @Builder.Default
    private Boolean active = true;

}