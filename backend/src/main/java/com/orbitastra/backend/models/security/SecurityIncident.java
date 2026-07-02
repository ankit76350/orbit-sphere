package com.orbitastra.backend.models.security;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.security.enums.IncidentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "security_incidents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityIncident {

    @Id
    private String id;

    private String schoolId;

    private String cameraId;

    private String incidentType;

    private String severity;

    private String description;

    private IncidentStatus status;

    private String detectedAt;
}
