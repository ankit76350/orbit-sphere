package com.orbitastra.backend.models.undone.security;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.security.enums.CameraStatus;
import com.orbitastra.backend.models.undone.security.enums.CameraType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "cameras")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Camera extends SchoolBase {

    private String name;

    private CameraType cameraType;

    private String location;

    private String streamUrl;

    private CameraStatus status;
}
