package com.orbitastra.backend.dto.crm.admissionreview.request;

/**
 * What #27c carries — the reviewer is finished, and this is what they concluded.
 *
 * <h2>The version, and nothing else — narrowed 2026-09-30</h2>
 *
 * <p><b>It carried a score, criterion scores, notes and a recommendation.</b> All four are gone.
 * The findings are #27's and the verdict is #27e's; what is left of finishing is saying it is
 * finished, and the only thing a caller can add to that is the version they decided against.
 *
 * <p><b>The recommendation went last, and it is the one worth explaining.</b> Keeping it let a
 * reviewer conclude and finish in one call, which reads like a kindness — but it left two places
 * setting the module's most consequential field, which is the thing splitting #27e out was meant
 * to end. Concluding is now always #27e and finishing is always this.
 *
 * <p><b>The status was never here.</b> The status is the endpoint: a call to {@code /complete} is
 * the move, so a body naming one would be a second way to say the same thing and a way to disagree
 * with the path. Every other field has now joined it.
 *
 * <p><b>A review still cannot be finished without a recommendation</b> — the rule did not go with
 * the field. It is now read off the REVIEW rather than the body: a review that has never been given
 * one is {@code 400 RECOMMENDATION_REQUIRED}, and the message says to record it with #27e first.
 *
 * <p><b>{@code version} is optional.</b> Send what you read for
 * {@code 409 CONCURRENT_MODIFICATION} when somebody recorded on the review while you were looking;
 * leave it out and the last write wins.
 */
public record AdmissionReviewCompleteRequest(Long version) {
}
