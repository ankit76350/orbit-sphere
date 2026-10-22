package com.orbitastra.backend.services.academics.utils;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.models.academics.examination.ExamCandidate;
import com.orbitastra.backend.models.academics.examination.ExamSchedule;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.repositories.academics.examcandidate.ExamCandidateRepository;
import com.orbitastra.backend.repositories.student.academicrecord.StudentAcademicRecordRepository;

import lombok.RequiredArgsConstructor;

/**
 * What the hall endpoints share. A method here never calls another method here.
 */
@Component
@RequiredArgsConstructor
public class ExamAttendanceServiceUtils {

    private final StudentAcademicRecordRepository academicRecords;
    private final ExamCandidateRepository examCandidates;

    /**
     * Who a paper's hall is about.
     *
     * @param sectionRecords the children in the paper's section on its date, one record each
     * @param candidates     the exam's candidates among them, plus anyone already marked at this
     *                       paper even if they have left the section — keyed by child
     */
    public record HallRoster(Map<String, StudentAcademicRecord> sectionRecords,
            Map<String, ExamCandidate> candidates) {
    }

    /**
     * The section on the paper's date, from academic records, and which of those children are
     * candidates of the exam — plus anyone with a hall entry on this paper already. On the day of a
     * transfer a child can hold two records; the later one is kept.
     *
     * Used by:
     * - ExamScheduleService.getPaper()
     * - ExamAttendanceService.getHallRegister()
     */
    public HallRoster rosterOf(School school, String academicYear, ExamSchedule paper) {
        Map<String, StudentAcademicRecord> sectionRecords = new LinkedHashMap<>();
        // TODO: read student academic records (who was in this section on this date)
        for (StudentAcademicRecord record : academicRecords.findInSectionOn(school.getId(),
                academicYear, paper.getClassDocsId(), paper.getSectionNo(), paper.getExamDate())) {
            StudentAcademicRecord kept = sectionRecords.get(record.getStudentDocsId());
            if (kept == null || record.getEffectiveFrom().isAfter(kept.getEffectiveFrom())) {
                sectionRecords.put(record.getStudentDocsId(), record);
            }
        }

        Map<String, ExamCandidate> candidates = new LinkedHashMap<>();
        if (!sectionRecords.isEmpty()) {
            // TODO: read exam candidates (the section's children)
            for (ExamCandidate one : examCandidates.findBySchoolIdAndExamDocsIdAndStudentDocsIdIn(
                    school.getId(), paper.getExamDocsId(), sectionRecords.keySet())) {
                candidates.put(one.getStudentDocsId(), one);
            }
        }
        //! Anyone already marked at this paper counts too, even if they have left the section.
        // TODO: read exam candidates (marked at this paper)
        for (ExamCandidate one : examCandidates.findMarkedAtPaper(school.getId(), paper.getId())) {
            candidates.putIfAbsent(one.getStudentDocsId(), one);
        }

        return new HallRoster(sectionRecords, candidates);
    }
}
