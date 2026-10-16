package com.orbitastra.backend.repositories.academics.studentmark;

import java.util.Map;

/** The mark reads that need a query built by hand. */
public interface StudentMarkRepositoryCustom {

    /** How many marks one exam has in each status. A status with no marks is not in the map. */
    Map<String, Long> countByStatusForExam(String schoolId, String examDocsId);
}
