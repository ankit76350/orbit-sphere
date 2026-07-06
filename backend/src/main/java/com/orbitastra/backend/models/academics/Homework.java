package com.orbitastra.backend.models.academics;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.academics.enums.AssignmentScope;
import com.orbitastra.backend.models.academics.enums.HomeworkStatus;

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

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Id
    private String id;

    @Indexed
    private String schoolId;

    @Indexed
    private String classId;

    private String subject;

    private String title;

    private String instructions;

    private LocalDate dueDate;

    @Builder.Default
    private Integer submittedCount = 0;

    private AssignmentScope assignmentScope;

    private Integer maxMarks;

    private String teacherId;

    @Builder.Default
    private List<StudentAssignment> studentAssignments = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentAssignment {

        private String studentId;

        private String customInstructions;

        private String submissionText;

        private String submissionFileUrl;

        private LocalDateTime submittedAt;

        @Builder.Default
        private HomeworkStatus status = HomeworkStatus.ASSIGNED;

        private Integer obtainedMarks;

        private String feedback;
    }
}