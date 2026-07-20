package com.orbitastra.backend.models.undone.exams;

import java.math.BigDecimal;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An audit record of a batch end-of-session promotion run (e.g. Grade 9 -> 10).
 * The per-student year placement itself lives in
 * {@code student.StudentAcademicRecord}; this captures the summary of one
 * promotion action for the register.
 */
@Document(collection = "promotion_batches")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionBatch {

    @CreatedDate
    private java.time.LocalDateTime createdAt;

    @LastModifiedDate
    private java.time.LocalDateTime updatedAt;

    @Id
    private String id;

    @Indexed
    private String schoolId;

    private String fromAcademicYear;

    private String toAcademicYear;

    private String fromGrade;

    private String toGrade;

    private Integer promotedCount;

    private Integer detainedCount;

    // Total outstanding dues carried forward with the promoted students.
    private BigDecimal duesCarried;

    private String executedBy; // references Staff.id / User.id
}
