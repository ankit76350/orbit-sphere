package com.orbitastra.backend.repositories.academics.examcandidate;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.orbitastra.backend.models.academics.examination.ExamCandidate;

/**
 * An exam's candidates — the children allowed to sit it, each with their hall attendance inside.
 *
 * <p>Every query carries {@code schoolId}.
 */
public interface ExamCandidateRepository extends MongoRepository<ExamCandidate, String> {

    /** One candidate, scoped to the school and the year. */
    Optional<ExamCandidate> findByIdAndSchoolIdAndAcademicYear(String id, String schoolId,
            String academicYear);

    /** These children's candidacies across several exams — one term's, for its report cards. */
    List<ExamCandidate> findBySchoolIdAndExamDocsIdInAndStudentDocsIdIn(String schoolId,
            Collection<String> examDocsIds, Collection<String> studentDocsIds);

    /** Every candidate of one exam. */
    List<ExamCandidate> findBySchoolIdAndExamDocsId(String schoolId, String examDocsId);

    /** The candidates of one exam among these children. */
    List<ExamCandidate> findBySchoolIdAndExamDocsIdAndStudentDocsIdIn(String schoolId,
            String examDocsId, Collection<String> studentDocsIds);

    /**
     * The candidates of one exam holding any of these booklets, as a main or a supplementary
     * booklet, on any paper. A booklet is used once in the whole exam.
     */
    @Query("{ 'schoolId': ?0, 'examDocsId': ?1, '$or': [ { 'papers.answerCopyNo': { '$in': ?2 } }, "
            + "{ 'papers.additionalAnswerCopyNos': { '$in': ?2 } } ] }")
    List<ExamCandidate> findHoldingBooklets(String schoolId, String examDocsId,
            Collection<String> booklets);

    /** The candidates who have a hall entry for one paper — whoever sat it, or was marked absent. */
    @Query("{ 'schoolId': ?0, 'papers.examScheduleDocsId': ?1 }")
    List<ExamCandidate> findMarkedAtPaper(String schoolId, String examScheduleDocsId);
}
