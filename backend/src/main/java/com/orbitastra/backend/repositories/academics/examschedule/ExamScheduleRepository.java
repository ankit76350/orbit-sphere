package com.orbitastra.backend.repositories.academics.examschedule;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.academics.examination.ExamSchedule;

/**
 * The papers of an exam — one per subject, component and section. The datesheet.
 *
 * <p>Every query carries {@code schoolId}.
 */
public interface ExamScheduleRepository
        extends MongoRepository<ExamSchedule, String>, ExamScheduleRepositoryCustom {
}
