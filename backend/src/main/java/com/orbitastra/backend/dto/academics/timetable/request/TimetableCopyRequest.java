package com.orbitastra.backend.dto.academics.timetable.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Build one day from another. Endpoint #6.
 *
 * <h2>What a school actually does</h2>
 *
 * <p>Nobody types five days. Monday is built once and Tuesday through Friday are copied from it and
 * then corrected. Without this, a week of a 400-period school is 2,000 periods typed by hand, and
 * the typing is where the mistakes come from.
 *
 * <p><b>The target date is in the path; the source is here.</b> The day being <i>built</i> is the
 * thing this endpoint acts on, so it is the address — the same reading that puts {@code {date}} in
 * the path on #2 and #7.
 *
 * <h2>The filters copy part of a day</h2>
 *
 * <p>A school that adds a section mid-term wants that section's pattern carried across, not the
 * whole school's. {@code classDocsId} alone copies one class; with {@code sectionNo} it copies one
 * section. <b>{@code sectionNo} alone is refused</b> — "section A" is not one thing across a
 * school, which is the same reason #10 matches the two as one period rather than as two
 * conditions.
 */
public record TimetableCopyRequest(

        /**
         * The day to copy from. Required.
         *
         * <p>It must already have a timetable, and it must belong to the same academic year as the
         * target: a class belongs to one year, so last year's Monday names classes this year does
         * not have.
         */
        @NotNull LocalDate sourceDate,

        /** Copy only this class's periods. Absent copies every class. */
        @Size(max = 60) String classDocsId,

        /**
         * Copy only this section's periods. Requires {@code classDocsId}.
         *
         * <p>Alone it is {@code 400 SECTION_WITHOUT_CLASS}: "section A" is not one thing across a
         * school, and a filter that silently matched every class's A would copy three classes when
         * the caller meant one.
         */
        @Size(max = 20) String sectionNo,

        /**
         * What to do when the target date already has a timetable.
         *
         * <p><b>Absent or false is {@code 409 TIMETABLE_ALREADY_EXISTS}</b> — the caller is
         * rebuilding something and should say so. True <b>adds</b> the copied periods to the ones
         * already there, and every conflict check then runs against the <i>combined</i> list: a
         * merge is exactly when a teacher ends up in two places at once.
         *
         * <p>It never replaces. Replacing a day whole is #2, which requires the version for the
         * reason this one does not need it: merging only ever adds.
         */
        Boolean merge) {

    /** Absent means "do not merge" — the safe reading, since merging writes into somebody's day. */
    public boolean mergeRequested() {
        return Boolean.TRUE.equals(merge);
    }
}
