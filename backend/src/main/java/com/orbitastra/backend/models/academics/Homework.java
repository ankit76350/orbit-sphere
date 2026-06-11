package com.orbitastra.backend.models.academics;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "homework")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Homework {

    @Id
    private String id;

    private String schoolId;

    private String classId;

    private String className;

    private String subject;

    private String title;

    private String instructions;

    private String dueDate;

    private Integer submittedCount;
}
