package com.orbitastra.backend.repositories.academics.studentmark;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.academics.examination.StudentMark;

/**
 * Marks — one per child per paper they sat.
 *
 * <p>Every query carries {@code schoolId}.
 */
public interface StudentMarkRepository
        extends MongoRepository<StudentMark, String>, StudentMarkRepositoryCustom {

    /** Every mark on one paper. */
    List<StudentMark> findBySchoolIdAndExamScheduleDocsId(String schoolId, String examScheduleDocsId);
}
