package com.orbitastra.backend.models.undone.alumni;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "mentorship_programs")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MentorshipProgram extends SchoolBase {

    private String mentorAlumniDocsId;

    private String studentDocsId;

    private String studentName;

    private String category;

    private String status;

    private LocalDate sessionDate;
}
