package com.orbitastra.backend.models.old.academics;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.AcademicStudentSchoolBase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "discipline_logs")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DisciplineLog extends AcademicStudentSchoolBase {

    private String violation;

    private BigDecimal fineAmount;

    private String actionTaken;

    private LocalDateTime incidentDate;
}
