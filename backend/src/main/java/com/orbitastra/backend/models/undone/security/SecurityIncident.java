package com.orbitastra.backend.models.undone.security;

import com.orbitastra.backend.models.BaseDocument;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.security.enums.IncidentStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "security_incidents")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityIncident extends BaseDocument {

    private String cameraId;

    private String incidentType;

    private String severity;

    private String description;

    private IncidentStatus status;

    private String detectedAt;
}
