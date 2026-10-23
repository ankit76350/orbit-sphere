package com.orbitastra.backend.dto.student.guardian.request;

/**
 * What #9 filters a school's guardians by.
 *
 * <p><b>Every field is optional, and sending none of them lists everybody.</b> That is a
 * deliberate difference from the endpoint this otherwise resembles — {@code crm} #15 refuses an
 * empty search with {@code NOTHING_TO_SEARCH_FOR}.
 *
 * <p>It can, because a list endpoint exists beside it: #13 is the worklist, so "show me
 * everything" already has a home and a search for nothing is a caller mistake. <b>Guardians have
 * no such endpoint.</b> #9 is the only way to read the collection at all, so refusing an empty
 * query would mean a school could never see its own contacts — and the plan has no tenth read to
 * put that in.
 *
 * <p><b>Which means this is a list that happens to filter, not a search that happens to page.</b>
 * It is paged and sorted for that reason.
 *
 * <h2>The filters do different kinds of matching, on purpose</h2>
 *
 * <ul>
 *   <li><b>{@code phone} is matched on its digits</b>, across the main number <i>and</i> the
 *       alternate one. This is the question #7 refuses on, so it has to find the same people #7
 *       would — otherwise a caller checks, sees nothing, and is refused anyway.</li>
 *   <li><b>{@code email} is matched whole</b> and case-insensitively. A question about identity:
 *       {@code a@b.com} must not match {@code maria@b.com} because the letters appear in it.</li>
 *   <li><b>{@code name}, {@code occupation} and {@code address} are matched anywhere.</b> None of
 *       them is an identifier — somebody typing "sharma" wants every Sharma to look at, and
 *       anchoring it would answer "no" to a question that was really "show me who it might
 *       be".</li>
 * </ul>
 *
 * <p><b>Sending more than one narrows</b> — they are AND-ed. The caller is filtering a list rather
 * than asking "is this person here", and the one field that ORs ({@code search}) ORs <i>within
 * itself</i> and still narrows against the rest.
 *
 * <h2>There is no "attached to nobody" filter, and it is the one worth having</h2>
 *
 * <p>#7 creates guardians attached to no child, so "which of these has nobody" is the obvious
 * follow-up. It is not here because the answer lives in {@code students} —
 * {@code guardians.guardianDocsId} — and this endpoint reads one collection. Answering it would
 * mean collecting every attached guardian id in the school first and sending them back as an
 * {@code $in}, which is a list that grows with the roll rather than with the page. #10 already
 * crosses that boundary for one guardian; the filter belongs with whatever crosses it for many.
 */
public record GuardianSearchRequest(

        /**
         * One box for the three things a guardian is known by — <b>name, phone or email</b>.
         *
         * <p>For the caller who has <i>something</i> and does not want to decide which field it
         * is, which is what a single search box on a screen is. The precise filters below are for
         * when they do know: this one is deliberately loose, so "9876" finds any number containing
         * it and "gmail" finds everybody on gmail.
         *
         * <p><b>It ORs across the three and still ANDs with everything else</b>, the same shape
         * {@code student} #4's {@code search} has.
         */
        String search,

        /** Any shape. Matched on its digits, across both numbers a guardian can have. */
        String phone,

        /** Matched whole and case-insensitively. */
        String email,

        /** Matched anywhere in the name. */
        String name,

        /** Matched anywhere. "teacher" finds "Primary school teacher". */
        String occupation,

        /**
         * Matched anywhere.
         *
         * <p><b>The one a school actually asks</b>: everybody in one village or one apartment
         * block, so a bus route change or a flooded road can be rung round.
         */
        String address,

        Integer page,
        Integer size,
        String sort) {
}
