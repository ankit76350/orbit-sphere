package com.orbitastra.backend.models.virtualclass;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "ai_notes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiNote {

    @Id
    private String id;

    private String schoolId;

    private String classId;

    private String notesContent;

    private String generatedAt;
}
