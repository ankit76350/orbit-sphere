package com.orbitastra.backend.dto.core;

import jakarta.validation.constraints.Size;

/**
 * The school's logo. Endpoint #9.
 *
 * <p>A PUT because a logo is one whole thing — there is one, or there is not. Sending
 * {@code {"logoUrl": ""}} or {@code {}} removes it, which is why there is no separate DELETE.
 *
 * <p><b>This takes a URL, and a file upload would be better.</b> The plan says so, and the
 * reason is worth keeping in view: a school-supplied URL can rot, can be changed to something
 * unwanted after approval, and can point at a tracker on a page parents load. Accepting the
 * file, storing it and returning our own URL removes all three. There is no storage service
 * yet, so this is the honest interim — and the service validates the scheme and host rather
 * than taking any string.
 *
 * <p>The host allow-list lives in the service, not here. A regex on the field could check the
 * shape but not the policy, and the policy is the part that matters.
 */
public record SchoolLogoRequest(

        /** Example: "https://cdn.example.com/schools/orbit/logo.png". Send "" to remove. */
        @Size(max = 500) String logoUrl) {
}
