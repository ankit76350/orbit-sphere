package com.orbitastra.backend.models.undone.a_latter.security;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.undone.a_latter.security.enums.CameraStatus;
import com.orbitastra.backend.models.undone.a_latter.security.enums.CameraType;

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
