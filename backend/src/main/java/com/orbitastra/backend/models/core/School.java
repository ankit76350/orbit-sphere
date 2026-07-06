package com.orbitastra.backend.models.core;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.core.enums.SubscriptionTier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "schools")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class School {
    @org.springframework.data.annotation.CreatedDate
    private java.time.LocalDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    private java.time.LocalDateTime updatedAt;


    @Id
    private String id;

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