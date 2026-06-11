package com.orbitastra.backend.models.security;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "camera_recordings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CameraRecording {

    @Id
    private String id;

    private String schoolId;

    private String cameraId;

    private String recordingUrl;

    private String startTime;

    private String endTime;
}
