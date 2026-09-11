package com.orbitastra.backend.dto.academics.schoolclass.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Edits to a class. Endpoint #13.
 *
 * <p><b>Every field is optional, and absent means "leave it alone".</b> A request that sends
 * nothing is a {@code 400 NOTHING_TO_UPDATE} rather than a no-op success, so a client with a bug
 * in its form finds out.
 *
 * <p><b>The name is editable, and that is the point of addressing a class by id.</b> An academic
 * year <i>is</i> its name to every other collection and can never be renamed; a class is its
 * document id — twelve documents store {@code classDocsId} — so nothing joins on this and a
 * rename breaks nothing. It only has to stay unique inside the year.
 *
 * <h2>What can be cleared, and what cannot</h2>
 *
 * <p>The two are not the same, and the difference is what JSON can express:
 *
 * <pre>
 * "affiliationProgrammeDocsId": ""     clears it
 * "affiliationProgrammeDocsId": null   leaves it            (same as absent)
 * "name": ""                           400 CLASS_NAME_REQUIRED
 * "displayOrder": null                 leaves it            (same as absent)
 * </pre>
 *
 * <p><b>So {@code displayOrder} cannot be cleared through this endpoint</b>, and that is a
 * limitation rather than a decision. A record field cannot tell an absent JSON key from an
 * explicit {@code null}, and a number has no equivalent of the empty string to mean "remove
 * this". Sending {@code 0} sets it to 0, which sorts first — not the same as having no order.
 * If clearing it ever matters, it needs a wrapper type that can hold the distinction, not a
 * sentinel value.
 *
 * <p><b>There is no field for sections or subjects</b>, and none may be added. Both are their own
 * resources — #17 to #21 and #22 to #27 — and an edit that could silently replace forty embedded
 * rows while looking like a rename is the shape this deliberately avoids.
 */
public record SchoolClassUpdateRequest(

        /** A new display name, unique within the year. Blank is refused, not treated as a clear. */
        @Size(max = 120) String name,

        /** A new sort order. Cannot be cleared — see the note above. */
        @Min(0) Integer displayOrder,

        /** A new programme, or {@code ""} to detach the class from the one it has. */
        @Size(max = 60) String affiliationProgrammeDocsId) {

    /** Whether the request asks for nothing at all. */
    public boolean isEmpty() {
        return name == null && displayOrder == null && affiliationProgrammeDocsId == null;
    }
}
