package com.orbitastra.backend.services.student;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.orbitastra.backend.common.current.CurrentSchoolResolver;
import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.common.text.TextHelper;
import com.orbitastra.backend.common.web.PageResponse;
import com.orbitastra.backend.dto.student.guardian.request.GuardianCreateRequest;
import com.orbitastra.backend.dto.student.guardian.request.GuardianSearchRequest;
import com.orbitastra.backend.dto.student.guardian.response.GuardianDetailResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.student.Guardian;
import com.orbitastra.backend.repositories.student.guardian.GuardianRepository;
import com.orbitastra.backend.services.student.helper.StudentHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The people a school contacts about its children. Endpoints #7 and #9 of the plan in
 * {@code controllers/student/README.md}; #8 and #10 to #13 are not built.
 *
 * <p><b>Its own service because {@code guardians} is its own collection</b>, and because a guardian
 * outlives any one child: the same person belongs to siblings, and correcting their number changes
 * it for all of them. Folding this into {@code StudentService} would put a shared document's rules
 * inside the thing that merely happens to create most of them.
 *
 * <p><b>A guardian is one real person per school, and the database decided that</b>, not this
 * service. {@code school_guardian_phone_uniq} and {@code school_guardian_email_uniq} are unique and
 * partial, so a value that is there has to be the only one. Everything interesting in this module
 * follows from that single fact.
 *
 * <p><b>#1 and #7 obey it in opposite ways, deliberately.</b> #1 is describing a family and so
 * matches an existing contact and links them; #7 is asserting a new person and so refuses. See
 * {@code GuardianCreateRequest} for the full reasoning — it is the one thing about this module
 * worth reading before using it.
 *
 * <p><b>No gate 4.</b> A contact has nothing to do with which academic year is running.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GuardianService {

    /** Repeated on every response until permissions exist. Deliberately hard to miss. */
    private static final String NO_AUTHORIZATION_YET =
            "NOTE: nothing checks who is asking yet.";

    /**
     * Ten digits is a whole Indian mobile number.
     *
     * <p>A query at least this long is compared on its <b>last ten</b>, so a country code or a
     * trunk 0 on either side stops mattering. Shorter than this has to match the whole number:
     * comparing by tail would match every number ending in those digits.
     *
     * <p>The same constant {@code StudentService} keeps, and deliberately the same rule — #9 is
     * the check somebody makes before #7, so the two have to find the same people.
     */
    private static final int FULL_PHONE_DIGITS = 10;

    /**
     * What #9 may be ordered by, and nothing else.
     *
     * <p><b>An allowlist is a security control, not a convenience.</b> An open sort field lets a
     * caller order the school's families by anything the document holds — an address, a phone
     * number — and read the values back out of the ordering without the endpoint returning them.
     */
    private static final Map<String, String> SORTABLE_GUARDIAN_FIELDS = new LinkedHashMap<>();

    static {
        SORTABLE_GUARDIAN_FIELDS.put("fullname", "fullName");
        SORTABLE_GUARDIAN_FIELDS.put("createdat", "createdAt");
        SORTABLE_GUARDIAN_FIELDS.put("updatedat", "updatedAt");
    }

    /** The same set written out, so the refusal can list what is allowed. */
    private static final String SORTABLE_GUARDIAN_FIELD_NAMES =
            SORTABLE_GUARDIAN_FIELDS.values().stream().collect(Collectors.joining(", "));

    /**
     * The default order, and the tiebreaker under every other one.
     *
     * <p><b>Two keys, because the first is not unique.</b> Two guardians genuinely share a name,
     * and a tie with no tiebreaker puts one on two pages while another appears on none. There is
     * no second natural key here — a guardian has no number of their own — so {@code createdAt}
     * settles it, and the id behind that.
     */
    private static final Sort GUARDIAN_ORDER =
            Sort.by(Sort.Order.asc("fullName"), Sort.Order.asc("createdAt"), Sort.Order.asc("id"));

    private final GuardianRepository guardians;
    private final CurrentSchoolResolver currentSchool;
    private final StudentHelper helper;

    /**
     * Endpoint #7 — <b>add a guardian who is not being created with a child</b>.
     *
     * <p><b>For the guardian who turns up on their own:</b> a grandmother added before the child
     * she will collect, an emergency number the office wants on file, a family entered ahead of an
     * admission.
     *
     * <p><b>No relation and no flags.</b> "Father", "primary contact", "may collect" are facts
     * about a person <i>and a child together</i>, so they live on the link — #11. A guardian made
     * here belongs to nobody yet, which is a normal state rather than a half-finished one.
     *
     * <p><b>It refuses a taken number where #1 matches one.</b> The difference is what the caller
     * is saying: #1 describes a family, this asserts a new person. Handing back an existing row
     * would look like a successful create and leave somebody believing a guardian exists that
     * does not.
     *
     * <p><b>Gates 1 and 2.</b> No gate 4.
     */
    public GuardianDetailResponse createGuardian(GuardianCreateRequest request) {

        //! step 1 - who is asking. requireUsable, because this writes.
        School school = currentSchool.requireUsable();
        log.info("[createGuardian] Step 1: Adding guardian '{}' to school {}",
                request.fullName(), school.getId());

        //! step 2 - the phone and the email, normalised BEFORE they are checked.
        //!
        //! NORMALISE FIRST OR THE CHECK IS A LIE. "+91 98765-43210" and "+919876543210" are one
        //! number and "Anita@X.com" and "anita@x.com" are one address — checking the raw strings
        //! would pass both, store both, and leave the school with a duplicate the index was
        //! supposed to stop. Through the module helper, because #1 has to normalise the same way
        //! or the two disagree about who is already here.
        String phoneNumber = helper.normalisePhone(request.phoneNumber());
        String emailAddress = TextHelper.lowercaseOrNull(request.emailAddress());

        //! step 3 - and each has to be free.
        //!
        //! THESE CHECKS ARE THE ENFORCEMENT, not a nicety in front of the indexes. Both are
        //! declared on the model and built on demand (app.mongo.sync-indexes), so a database that
        //! has never synced carries no such constraint at all — measured 2026-10-06, the
        //! guardians collection has only _id_. Where they ARE built, this turns a duplicate-key
        //! 500 into a 409 that says which field and who holds it.
        //!
        //! THE REFUSAL NAMES THE PERSON, so the caller can go and look at them: either they have
        //! the wrong number, or they meant to edit that guardian rather than add one.
        if (phoneNumber != null) {

            //! COMPARED ON THE DIGITS, NOT ON THE STORED STRING, and that was a bug until
            //! 2026-10-07. An exact comparison made "098765 11111" and "+919876511111" two
            //! people — so #9, which has always compared digits, FOUND the guardian and this
            //! endpoint created them again anyway. Measured: #9 answered 1 and #7 answered 201
            //! for one number, which is the check and the refusal telling a caller two different
            //! things about the same person.
            //!
            //! BEING STRICTER THAN THE INDEX IS SAFE HERE, which is why this differs from #1.
            //! #1 MATCHES AND LINKS, so a loose match risks attaching the wrong man to a child —
            //! it stays exact. #7 only REFUSES, and refusing a number that is probably already
            //! somebody's costs the caller one message and saves a duplicate human.
            //!
            //! THE ALTERNATE NUMBER IS NOT COUNTED — the `false` below. It is deliberately
            //! shared, a family landline, so refusing on it would make a mother impossible to add
            //! once the father listed it as his second number. The unique index is on
            //! phoneNumber alone and this refusal follows it.
            String digits = helper.digitsOf(phoneNumber);
            boolean wholeNumber = digits.length() < FULL_PHONE_DIGITS;
            String needle = wholeNumber ? digits
                    : digits.substring(digits.length() - FULL_PHONE_DIGITS);

            // TODO: read guardian (is this number already somebody's)
            Guardian holder = guardians
                    .findByLoosePhone(school.getId(), needle, wholeNumber, false, 1)
                    .stream().findFirst().orElse(null);
            if (holder != null) {
                throw ApiException.conflict("GUARDIAN_PHONE_TAKEN",
                        "'" + holder.getFullName() + "' already has the phone number "
                                + holder.getPhoneNumber() + " in this school, which is the same "
                                + "number as " + phoneNumber + ". A number identifies one person "
                                + "here — correct that guardian with #8, or attach them to a "
                                + "child with #11, neither of which is built yet.");
            }
        }

        if (emailAddress != null) {
            // TODO: read guardian (is this address already somebody's)
            Guardian holder = guardians.findBySchoolIdAndEmailAddress(school.getId(), emailAddress)
                    .orElse(null);
            if (holder != null) {
                throw ApiException.conflict("GUARDIAN_EMAIL_TAKEN",
                        "'" + holder.getFullName() + "' already has the email address "
                                + emailAddress + " in this school. An address identifies one "
                                + "person here, the same as a number.");
            }
        }

        //! step 4 - build the person.
        //!
        //! schoolId IS SET BY HAND, and that is not boilerplate. SchoolBase marks it required but
        //! nothing checks a document on the way to the database: a contact saved without it is
        //! stored, invisible to every query this school makes, and found only by reading the raw
        //! collection.
        Guardian person = Guardian.builder()
                .schoolId(school.getId())
                .fullName(request.fullName().trim())
                .phoneNumber(phoneNumber)
                .alternatePhoneNumber(helper.normalisePhone(request.alternatePhoneNumber()))
                .emailAddress(emailAddress)
                .address(TextHelper.blankToNull(request.address()))
                .occupation(TextHelper.blankToNull(request.occupation()))
                .preferredLanguage(request.preferredLanguage())
                .build();

        //! step 5 - save it. Built above, written here: two steps, so what is being stored can be
        //! read before the line that stores it.
        // TODO: insert guardian
        Guardian saved = guardians.save(person);
        log.info("[createGuardian] Step 2: Saved the guardian (id={})", saved.getId());

        return GuardianDetailResponse.of(saved,
                "'" + saved.getFullName() + "' is on file and attached to nobody. Linking them to "
                        + "a child is #11, which is not built — until then this guardian is "
                        + "findable and unused. " + NO_AUTHORIZATION_YET);
    }

    /**
     * Endpoint #9 — <b>the school's guardians</b>.
     *
     * <p><b>It is the list and the search at once, and it had to be.</b> The plan describes it as
     * "find the existing one before making a second", which is a search — but it is also the
     * <i>only</i> read of this collection, so refusing an empty query would mean a school could
     * never see its own contacts at all. {@code student} #6 and {@code crm} #15 do refuse one,
     * because a list endpoint sits beside each of them. Nothing sits beside this.
     *
     * <p><b>So no filters means everybody</b>, paged and in name order.
     *
     * <p><b>The phone filter has to find what #7 refuses on.</b> Same digits rule, same two
     * fields — a caller who checks here, sees nothing and is then refused by #7 would have been
     * told two different things about one number.
     *
     * <p><b>The three filters narrow rather than widen.</b> They are AND-ed, unlike #6, where
     * several ways of naming one child are OR-ed because any of them identifies them. Here the
     * caller is filtering a list.
     *
     * <p><b>No gates.</b> A read — and a suspended school still needs to ring a parent.
     */
    public PageResponse<GuardianDetailResponse> listGuardians(GuardianSearchRequest request) {

        //! step 1 - the paging and the order, checked before anything is read. A cheap check with
        //! no database behind it goes first, so a malformed request costs no round trip.
        Pageable pageable = PageResponse.pageableOf(request.page(), request.size(), request.sort(),
                SORTABLE_GUARDIAN_FIELDS, SORTABLE_GUARDIAN_FIELD_NAMES, GUARDIAN_ORDER);

        //! step 2 - who is asking. require, not requireUsable: a suspended school still reads its
        //! own families, and still has to ring them.
        School school = currentSchool.require();

        //! step 3 - the phone question, worked out HERE rather than in the query, so #9 and the
        //! loose lookup behind #6 cannot disagree about what "the same number" means.
        //!
        //! A FULL-LENGTH NUMBER IS COMPARED ON ITS LAST TEN DIGITS, so a country code or a trunk 0
        //! on either side stops mattering. A shorter one has to match the whole number.
        String digits = helper.digitsOf(request.phone());
        boolean wholeNumber = digits.length() < FULL_PHONE_DIGITS;
        String needle = wholeNumber ? digits
                : digits.substring(digits.length() - FULL_PHONE_DIGITS);
        log.info("[listGuardians] Step 1: Reading the guardians of school {}", school.getId());

        //! step 4 - one page, filtered and ordered in the database rather than in Java.
        // TODO: read guardians
        return PageResponse.from(
                guardians.search(school.getId(), request, needle, wholeNumber, pageable),
                // The single-argument factory, so no nextStep appears on a row: a read changed
                // nothing, and a null on every row is noise a client has to decide about.
                person -> GuardianDetailResponse.of(person, null));
    }
}
