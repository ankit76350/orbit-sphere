package com.orbitastra.backend.models.undone.security;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.security.enums.CameraStatus;
import com.orbitastra.backend.models.undone.security.enums.CameraType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "cameras")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Camera {
    @org.springframework.data.annotation.CreatedDate
    private java.time.LocalDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    private java.time.LocalDateTime updatedAt;


    @Id
    private String id;

    private String schoolId;

    private String name;

    private CameraType cameraType;

    private String location;

    private String streamUrl;

    private CameraStatus status;
}
