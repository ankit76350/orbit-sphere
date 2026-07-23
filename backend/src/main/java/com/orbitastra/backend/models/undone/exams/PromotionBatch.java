package com.orbitastra.backend.models.undone.exams;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;

import lombok.AllArgsConstructor;
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
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionBatch extends SchoolBase {

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
