package com.orbitastra.backend.dto.academics.schoolclass.request;

import com.orbitastra.backend.models.academics.enums.SubjectType;

import jakarta.validation.constraints.Size;

/**
 * Edits to one subject assignment. Endpoint #24.
 *
 * <p><b>Every field is optional, and absent means "leave it alone".</b> A request that sends
 * nothing is a {@code 400 NOTHING_TO_UPDATE} rather than a no-op success, so a client with a bug
 * in its form finds out.
 *
 * <h2>What this deliberately cannot change</h2>
 *
 * <p><b>Not {@code subjectCode} and not {@code sectionNo} — together they are the key.</b> Seven
 * collections store {@code subjectCode} as a plain string and a subject row has no id of its own,
 * so a rename would not fail, would not cascade, and would leave every one of those strings
 * naming an assignment that no longer answers to it.
 *
 * <p>Moving an assignment to another section is <b>#26 on the old row plus #22 on the new one</b>.
 * That reads correctly as history — the section stopped teaching it, another section started —
 * where a silent key edit would rewrite the past.
 *
 * <p><b>Not {@code teacherDocsIds}</b> — that is #25. It replaces a list rather than setting a
 * field, and two endpoints writing one array is how a duplicate id gets in.
 *
 * <p><b>Not {@code active}</b> — that is #26 and #27. Retiring a subject is an event with a
 * meaning, not a field to toggle.
 *
 * <h2>What can be cleared, and what cannot</h2>
 *
 * <pre>
 * "shortName": ""               clears it
 * "gradingSchemeDocsId": ""     clears it, falling back to the exam's scheme
 * "name": ""                    400 SUBJECT_NAME_REQUIRED
 * any field: null               leaves it            (same as absent)
 * </pre>
 *
 * <p>Clearing {@code gradingSchemeDocsId} is not "no grading". The resolution order in the model
 * README is the subject's scheme, then {@code Exam.gradingSchemeDocsId}, then none — so clearing
 * it hands the decision back to the exam.
 */
public record SubjectUpdateRequest(

        /** A new display name. Blank is refused, not treated as a clear. */
        @Size(max = 120) String name,

        /** A new short name, or {@code ""} to remove the one it has. */
        @Size(max = 40) String shortName,

        /** A new type. One of the five, and it cannot be cleared — the model requires it. */
        SubjectType subjectType,

        /** A new grading scheme, or {@code ""} to fall back to the exam's. */
        @Size(max = 60) String gradingSchemeDocsId) {

    /** Whether the request asks for nothing at all. */
    public boolean isEmpty() {
        return name == null && shortName == null && subjectType == null
                && gradingSchemeDocsId == null;
    }
}
