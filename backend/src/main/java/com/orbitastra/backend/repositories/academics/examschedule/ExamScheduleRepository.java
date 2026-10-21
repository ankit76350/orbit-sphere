package com.orbitastra.backend.repositories.academics.examschedule;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.orbitastra.backend.models.academics.enums.ExamScheduleStatus;
import com.orbitastra.backend.models.academics.examination.ExamSchedule;

/**
 * The papers of an exam — one per subject, component and section. The datesheet.
 *
 * <p>Every query carries {@code schoolId}.
 */
public interface ExamScheduleRepository extends MongoRepository<ExamSchedule, String>, ExamScheduleRepositoryCustom {

    /** Whether an exam has any paper at all, cancelled ones included. */
    boolean existsBySchoolIdAndExamDocsId(String schoolId, String examDocsId);

    /**
     * Whether a section already has this subject and part in this exam, ignoring case — cancelled
     * papers included, because the unique index counts them too.
     */
    boolean existsBySchoolIdAndExamDocsIdAndClassDocsIdAndSectionNoAndSubjectCodeAndComponentNameIgnoreCase(
            String schoolId, String examDocsId, String classDocsId, String sectionNo,
            String subjectCode, String componentName);

    /**
     * Whether a section already has this subject as one whole paper (no part named) in this exam —
     * cancelled papers included.
     */
    boolean existsBySchoolIdAndExamDocsIdAndClassDocsIdAndSectionNoAndSubjectCodeAndComponentNameIsNull(
            String schoolId, String examDocsId, String classDocsId, String sectionNo,
            String subjectCode);

    /** The papers held in one room on one day, across every exam — cancelled ones left out. */
    List<ExamSchedule> findBySchoolIdAndRoomDocsIdAndExamDateAndStatusNot(
            String schoolId, String roomDocsId, LocalDate examDate,
            ExamScheduleStatus status);

    /**
     * The papers on one day where any of these staff are on duty, across every exam — cancelled
     * ones left out.
     */
    List<ExamSchedule> findBySchoolIdAndInvigilatorDocsIdsInAndExamDateAndStatusNot(
            String schoolId, Collection<String> invigilatorDocsIds, LocalDate examDate,
            ExamScheduleStatus status);

    /**
     * The exam's papers dated before {@code from} or after {@code to}, cancelled ones left out —
     * the papers a new exam window would leave outside it.
     */
    @Query("{ 'schoolId': ?0, 'examDocsId': ?1, 'status': { '$ne': ?2 }, "
            + "'$or': [ { 'examDate': { '$lt': ?3 } }, { 'examDate': { '$gt': ?4 } } ] }")
    List<ExamSchedule> findPapersOutside(String schoolId, String examDocsId,
            ExamScheduleStatus status, LocalDate from, LocalDate to);
}
