package com.orbitastra.backend.repositories.academics.reportcard;

import java.util.Collection;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.academics.examination.ReportCard;

/**
 * Report cards — one versioned snapshot per child per term.
 *
 * <p>Every query carries {@code schoolId}.
 */
public interface ReportCardRepository extends MongoRepository<ReportCard, String> {

    /** Every version of these children's cards for one term — generate keeps the latest of each. */
    List<ReportCard> findBySchoolIdAndAcademicYearAndTermDocsIdAndStudentDocsIdIn(String schoolId,
            String academicYear, String termDocsId, Collection<String> studentDocsIds);
}
