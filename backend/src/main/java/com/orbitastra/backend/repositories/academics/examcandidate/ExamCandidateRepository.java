package com.orbitastra.backend.repositories.academics.examcandidate;

import java.util.Collection;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.orbitastra.backend.models.academics.examination.ExamCandidate;

/**
 * An exam's candidates — the children allowed to sit it, each with their hall attendance inside.
 *
 * <p>Every query carries {@code schoolId}.
 */
public interface ExamCandidateRepository extends MongoRepository<ExamCandidate, String> {

    /** Every candidate of one exam. */
    List<ExamCandidate> findBySchoolIdAndExamDocsId(String schoolId, String examDocsId);

    /** The candidates of one exam among these children. */
    List<ExamCandidate> findBySchoolIdAndExamDocsIdAndStudentDocsIdIn(String schoolId,
            String examDocsId, Collection<String> studentDocsIds);

    /** The candidates who have a hall entry for one paper — whoever sat it, or was marked absent. */
    @Query("{ 'schoolId': ?0, 'papers.examScheduleDocsId': ?1 }")
    List<ExamCandidate> findMarkedAtPaper(String schoolId, String examScheduleDocsId);
}
