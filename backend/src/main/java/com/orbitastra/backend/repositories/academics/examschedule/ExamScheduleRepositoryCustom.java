package com.orbitastra.backend.repositories.academics.examschedule;

import java.util.Collection;
import java.util.Map;

/** The paper reads that need a query built by hand. */
public interface ExamScheduleRepositoryCustom {

    /**
     * How many papers each exam has, cancelled papers left out. An exam with no papers is not in
     * the map.
     */
    Map<String, Long> countPapersByExam(String schoolId, Collection<String> examDocsIds);
}
