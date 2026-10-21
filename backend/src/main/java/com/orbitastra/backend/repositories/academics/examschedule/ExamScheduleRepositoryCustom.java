package com.orbitastra.backend.repositories.academics.examschedule;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.orbitastra.backend.models.academics.enums.ExamScheduleStatus;
import com.orbitastra.backend.models.academics.examination.ExamSchedule;

/** The paper reads that need a query built by hand. */
public interface ExamScheduleRepositoryCustom {

    /**
     * How many papers each exam has, cancelled papers left out. An exam with no papers is not in
     * the map.
     */
    Map<String, Long> countPapersByExam(String schoolId, Collection<String> examDocsIds);

    /** How many papers one exam has in each status. A status with no papers is not in the map. */
    Map<String, Long> countByStatusForExam(String schoolId, String examDocsId);

    /**
     * An exam's papers for its datesheet, by date then start time, with whichever filters were
     * sent: one class, one section (ignoring case), one day, one status.
     */
    List<ExamSchedule> findDatesheet(String schoolId, String examDocsId, String classDocsId,
            String sectionNo, LocalDate date, ExamScheduleStatus status);
}
