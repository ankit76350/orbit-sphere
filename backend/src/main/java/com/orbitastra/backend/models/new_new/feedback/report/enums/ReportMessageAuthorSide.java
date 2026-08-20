package com.orbitastra.backend.models.new_new.feedback.report.enums;

/**
 * Which side of a report conversation wrote a message.
 *
 * <p>Two values rather than a staff id being null or not, because the meaning has to survive
 * an anonymous reporter. A message from the school always has an author; a message from the
 * reporter may have none at all, and "author is null" would then be indistinguishable from
 * "author was not recorded properly".
 */
public enum ReportMessageAuthorSide {
    /** Written by the school: the recipient, or somebody they brought in. */
    SCHOOL,

    /** Written by whoever made the report, returning with their access code. */
    REPORTER
}
