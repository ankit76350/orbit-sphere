package com.orbitastra.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.model.enums.CameraStatus;
import com.orbitastra.backend.model.enums.CameraType;

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

    @Id
    private String id;

    private String schoolId;

    private String name;

    private CameraType cameraType;

    private String location;

    private String streamUrl;

    private CameraStatus status;
}
