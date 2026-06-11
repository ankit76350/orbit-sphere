package com.orbitastra.backend.models.security;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "camera_assignments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CameraAssignment {

    @Id
    private String id;

    private String schoolId;

    private String cameraId;

    private String gradeId;

    private String classId;

    private String sectionId;
}
