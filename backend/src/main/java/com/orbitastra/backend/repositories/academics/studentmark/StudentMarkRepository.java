package com.orbitastra.backend.repositories.academics.studentmark;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.academics.examination.StudentMark;

/**
 * Marks — one per child per paper they sat.
 *
 * <p>Every query carries {@code schoolId}.
 */
public interface StudentMarkRepository
        extends MongoRepository<StudentMark, String>, StudentMarkRepositoryCustom {
}
