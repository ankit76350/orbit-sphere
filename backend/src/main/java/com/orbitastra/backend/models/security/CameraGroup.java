package com.orbitastra.backend.models.security;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "camera_groups")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CameraGroup {

    @Id
    private String id;

    private String schoolId;

    private String groupName;

    private String groupType;
}
