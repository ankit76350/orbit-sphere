package com.orbitastra.backend.dto.core.platform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Changes a school's subdomain. Endpoint #10, platform surface only.
 *
 * <p><b>Two fields, and the first one is the point.</b> {@code currentSubdomain} has to match
 * what the school answers to today or the request is refused. It is not a value anything reads —
 * it exists so that changing the key that resolves every request cannot be done by pasting the
 * wrong id into a URL.
 *
 * <p>That guard is worth the extra field here and nowhere else in this package. Every other
 * platform endpoint acts on one field of one school; this one moves the school's address, and
 * doing it to the wrong tenant takes that tenant off the air.
 *
 * <p><b>Why the school cannot do this itself.</b> The subdomain is not a detail — it is how
 * requests find the tenant at all. Changing it breaks every bookmark, saved link and stored
 * login, and the old label has to stay reserved afterwards. Endpoint #6 edits the school's
 * details; this is a platform operation with routing consequences, so the field is deliberately
 * absent from #6's request.
 */
public record SchoolSubdomainRequest(

        /**
         * What the school answers to right now, exactly. Example: "st-marys"
         *
         * <p>Compared after the same normalization the new one gets, so casing and stray spaces
         * are forgiven — a typed confirmation is a check on intent, not on typing.
         */
        @NotBlank @Size(max = 63) String currentSubdomain,

        /** What it should answer to instead. Example: "st-marys-pune" */
        @NotBlank @Size(max = 63) String newSubdomain) {
}
