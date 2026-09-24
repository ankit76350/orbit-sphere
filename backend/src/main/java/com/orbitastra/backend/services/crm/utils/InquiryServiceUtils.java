package com.orbitastra.backend.services.crm.utils;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.crm.Inquiry;
import com.orbitastra.backend.repositories.crm.inquiry.InquiryRepository;

import lombok.RequiredArgsConstructor;

/**
 * The read {@link com.orbitastra.backend.services.crm.InquiryService} makes more than once.
 *
 * <p>Per the service folder rules: a main service has its own {@code utils}, and <b>a method here
 * never calls another method here</b>. Only the service calls these.
 *
 * <p><b>This file did not exist while the service had three endpoints</b>, and that was right and
 * recorded as such: #8 writes, #13 pages, #14 reads one — and nothing in that set repeated. #9 is
 * what changed it, because correcting a lead starts exactly where opening one does.
 *
 * <p><b>One method, and that is the file being honest rather than thin.</b> Three other things in
 * that service look extractable and are not. {@code overdueNow}, {@code contactNumberOf} and
 * {@code nextStepFor} read no repository at all — they are sentences about a document already in
 * hand, and a {@code utils} full of those buys a longer import list and a jump to nowhere. The
 * year and class checks #8 and #9 share look like the better candidates and are worse ones: they
 * are four lines of guard that read in the order they happen, and pulling them out would hide the
 * one thing about them that matters, which is that #9 runs them <i>conditionally</i> and #8 does
 * not.
 */
@Component
@RequiredArgsConstructor
public class InquiryServiceUtils {

    private final InquiryRepository inquiries;

    /**
     * One lead, or {@code 404 INQUIRY_NOT_FOUND}.
     *
     * <p><b>Scoped by school in the QUERY, never checked after the read.</b> An id from another
     * school is a real id; looking it up without the school would find it, and a check afterwards
     * is one {@code if} away from being forgotten. The difference is another tenant's family
     * details.
     *
     * <p><b>A 404 rather than a 403</b>, for the same reason it is everywhere else in this module:
     * a 403 would confirm that the lead exists.
     *
     * <p><b>It trims and tolerates null</b>, so a blank path segment is a 404 about an empty id
     * rather than a 500 about a null.
     *
     * Used by:
     * - updateInquiry()
     * - getInquiry()
     */
    public Inquiry loadInquiry(School school, String inquiryId) {
        String id = inquiryId == null ? "" : inquiryId.trim();

        // TODO: read inquiry
        return inquiries.findByIdAndSchoolId(id, school.getId())
                .orElseThrow(() -> ApiException.notFound("INQUIRY_NOT_FOUND",
                        "No inquiry with id '" + id + "' in this school."));
    }
}
