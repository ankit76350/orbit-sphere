package com.orbitastra.backend.dto.academics.examcandidate.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * What examination #43 needs to add the children who may sit an exam — one section at a time.
 *
 * @param classDocsId    the class. Must be a class of the exam's year.
 * @param sectionNo      the section of that class.
 * @param studentDocsIds optional. Left out, the whole section is added — everyone in it on the
 *                       exam's first day. Sent, only these children, each of whom must be in the
 *                       section on some day of the exam.
 */
public record ExamCandidatesAddRequest(
        @NotBlank @Size(max = 60) String classDocsId,
        @NotBlank @Size(max = 20) String sectionNo,
        @Size(max = 200) List<@NotBlank @Size(max = 60) String> studentDocsIds) {
}
