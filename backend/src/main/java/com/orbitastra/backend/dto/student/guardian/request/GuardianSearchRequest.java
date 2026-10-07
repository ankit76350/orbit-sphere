package com.orbitastra.backend.dto.student.guardian.request;

/**
 * What #9 filters a school's guardians by.
 *
 * <p><b>Every field is optional, and sending none of them lists everybody.</b> That is a
 * deliberate difference from the two endpoints this otherwise resembles — {@code student} #6 and
 * {@code crm} #15 both refuse an empty search with {@code NOTHING_TO_SEARCH_FOR}.
 *
 * <p>They can, because a list endpoint exists beside each of them: #4 is the roll and #13 is the
 * worklist, so "show me everything" already has a home and a search for nothing is a caller
 * mistake. <b>Guardians have no such endpoint.</b> #9 is the only way to read the collection at
 * all, so refusing an empty query would mean a school could never see its own contacts — and the
 * plan has no tenth read to put that in.
 *
 * <p><b>Which means this is a list that happens to filter, not a search that happens to page.</b>
 * It is paged and sorted for that reason.
 *
 * <h2>The three filters do different kinds of matching, on purpose</h2>
 *
 * <ul>
 *   <li><b>{@code phone} is matched on its digits</b>, across the main number <i>and</i> the
 *       alternate one. This is the question #7 refuses on, so it has to find the same people #7
 *       would — otherwise a caller checks, sees nothing, and is refused anyway.</li>
 *   <li><b>{@code email} is matched whole</b> and case-insensitively. A question about identity:
 *       {@code a@b.com} must not match {@code maria@b.com} because the letters appear in it.</li>
 *   <li><b>{@code name} is matched anywhere.</b> A name is not an identifier — somebody typing
 *       "sharma" wants every Sharma to look at, and anchoring it would answer "no" to a question
 *       that was really "show me who it might be".</li>
 * </ul>
 *
 * <p><b>Sending more than one narrows</b> — they are AND-ed. Unlike #6, where the several ways of
 * naming one child are OR-ed because any of them identifies them; here the caller is filtering a
 * list rather than asking "is this person here".
 *
 * <h2>There is no "attached to nobody" filter, and it is the one worth having</h2>
 *
 * <p>#7 creates guardians attached to no child, so "which of these has nobody" is the obvious
 * follow-up. It is not here because the answer lives in {@code students} —
 * {@code guardians.guardianDocsId} — and this endpoint reads one collection. It arrives with
 * #10, which already has to cross that boundary.
 */
public record GuardianSearchRequest(

        /** Any shape. Matched on its digits, across both numbers a guardian can have. */
        String phone,

        /** Matched whole and case-insensitively. */
        String email,

        /** Matched anywhere in the name. */
        String name,

        Integer page,
        Integer size,
        String sort) {
}
