package com.orbitastra.backend.dto.academics.schoolclass.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.orbitastra.backend.models.academics.structure.embedded.ClassSubject;
import com.orbitastra.backend.models.academics.enums.SubjectType;

/**
 * One subject assignment, as every endpoint that returns one describes it.
 *
 * <p><b>The row key is the pair {@code (subjectCode, sectionNo)}</b>, not the code alone. A
 * class-wide assignment has a null {@code sectionNo}; a per-section one names it. So the same
 * {@code subjectCode} can appear more than once in a class's list, and a client keying rows by
 * code alone will lose all but one.
 *
 * <p><b>{@code sectionNo} absent means every section of the class</b>, which is the ordinary
 * case. It is left out of the JSON rather than sent as null, so "all sections" and "a section
 * that was not recorded" cannot be confused — there is no second meaning for absent here.
 *
 * <p><b>{@code teacherDocsIds} and {@code gradingSchemeDocsId} are raw ids.</b> Staff could be
 * resolved now that {@code StaffRepository} exists; grading schemes could not — that repository
 * is the one still missing. Neither is resolved, for the same reason as
 * {@link SectionView#classTeacherDocsId()}: one place should decide how a teacher is presented,
 * and it is not two response records.
 */
public record SubjectView(
        String subjectCode,
        String name,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String shortName,

        SubjectType subjectType,

        /** Absent means the whole class. */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String sectionNo,

        List<String> teacherDocsIds,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        String gradingSchemeDocsId,

        Boolean active) {

    public static SubjectView of(ClassSubject subject) {
        return new SubjectView(
                subject.getSubjectCode(),
                subject.getName(),
                subject.getShortName(),
                subject.getSubjectType(),
                subject.getSectionNo(),
                // Never null in the JSON: a subject with no teacher assigned is an empty list,
                // which a client can iterate. Null would need a second check at every call site.
                subject.getTeacherDocsIds() == null ? List.of() : subject.getTeacherDocsIds(),
                subject.getGradingSchemeDocsId(),
                subject.getActive());
    }
}
