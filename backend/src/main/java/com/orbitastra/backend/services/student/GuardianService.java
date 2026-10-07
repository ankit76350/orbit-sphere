package com.orbitastra.backend.services.student;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
import com.orbitastra.backend.dto.student.guardian.request.GuardianUpdateRequest;
import com.orbitastra.backend.dto.student.guardian.response.GuardianDetailResponse;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.student.Guardian;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.embedded.GuardianLink;
import com.orbitastra.backend.repositories.student.guardian.GuardianRepository;
import com.orbitastra.backend.repositories.student.student.StudentRepository;
import com.orbitastra.backend.services.student.helper.StudentHelper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The people a school contacts about its children. Endpoints #7, #8, #9 and #10 of the plan in
 * {@code controllers/student/README.md}; #11 to #13 are not built.
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
    //! #10 ONLY, and it is the one read in this service that leaves `guardians`. The link lives on
    //! the STUDENT — GuardianLink is embedded there, not here — so "what is this person to their
    //! children" cannot be answered without it.
    private final StudentRepository students;
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

    /**
     * Endpoint #10 — <b>one guardian, and every child they are attached to</b>.
     *
     * <p><b>The flags are the whole reason this endpoint is not just a read of one document.</b>
     * "Father", "primary contact", "may collect", "portal" live on {@code GuardianLink}, which is
     * embedded in the <b>student</b> rather than on the guardian — because the same man is all
     * four to one child and only an emergency number for their cousin. So they come back
     * <i>per child</i>, and a guardian's page that printed one set of them would be printing a
     * fiction.
     *
     * <p><b>Two queries, and the second is one index seek.</b>
     * {@code school_guardian_students_idx} is keyed {@code {schoolId, guardians.guardianDocsId}}
     * and exists for exactly this — not one read per child.
     *
     * <p><b>An empty list is a real answer here</b>, unlike on #7 and #9 where the field is absent
     * altogether: it means a guardian #7 created and #11 has never attached to anybody. That is
     * the normal state of an emergency number put on file before the child arrives.
     *
     * <p><b>The children are read second, so a guardian with none is still a 200.</b> The only
     * 404 is the guardian themselves.
     *
     * <p><b>No gates.</b> A read.
     */
    public GuardianDetailResponse getGuardian(String guardianDocsId) {

        //! step 1 - who is asking. require, not requireUsable: this is a read.
        School school = currentSchool.require();
        String id = guardianDocsId == null ? "" : guardianDocsId.trim();
        log.info("[getGuardian] Step 1: Reading guardian {} of school {}", id, school.getId());

        //! step 2 - the guardian, scoped by school IN THE QUERY and never checked after. An id
        //! from another school is a real id, and reading it would hand over a family's phone
        //! number and home address.
        //!
        //! THE FIRST VERSION OF THIS LINE WAS findById(...).filter(...), which reads another
        //! school's document into memory and then decides not to use it. That is the same bug
        //! with extra steps: the read already happened, and the `if` that undoes it is one edit
        //! away from being dropped.
        //!
        //! A 404 rather than a 403, for the same reason it is everywhere else: a 403 would
        //! confirm they exist.
        // TODO: read guardian
        Guardian person = guardians.findByIdAndSchoolId(id, school.getId())
                .orElseThrow(() -> ApiException.notFound("GUARDIAN_NOT_FOUND",
                        "No guardian with id '" + id + "' in this school."));

        //! step 3 - their children. ONE QUERY for all of them, on the index that exists for this.
        // TODO: read students (which children is this guardian attached to)
        List<Student> family = students.findBySchoolIdAndGuardiansGuardianDocsId(
                school.getId(), person.getId());

        //! step 4 - pair each child with what this guardian is TO THEM. The link is found on the
        //! child rather than assumed: a student whose array does not actually name this guardian
        //! cannot be reached by the query above, so a missing link here would mean the index and
        //! the document disagree — skipped rather than drawn with empty flags.
        List<GuardianDetailResponse.AttachedChild> children = new ArrayList<>();
        for (Student child : family) {
            for (GuardianLink link : child.getGuardians() == null
                    ? List.<GuardianLink>of() : child.getGuardians()) {
                if (person.getId().equals(link.getGuardianDocsId())) {
                    children.add(GuardianDetailResponse.AttachedChild.of(child, link));
                }
            }
        }
        log.info("[getGuardian] Step 2: '{}' is attached to {} child(ren)",
                person.getFullName(), children.size());

        //! step 5 - no nextStep: a read changed nothing.
        return GuardianDetailResponse.of(person, children, null);
    }

    /**
     * Endpoint #8 — <b>correct a guardian</b>.
     *
     * <p><b>This changes the person for every child linked to them</b>, which is the point of the
     * shared row and the one thing worth knowing before using it. A guardian is one real person
     * per school, so correcting a mother's number corrects it on all four of her children at once.
     * There is no way to change it for one of them, and there should not be: the alternative is
     * four rows for one woman and no way to tell which is current.
     *
     * <p><b>So the answer says how many were affected.</b> A caller who did not expect that finds
     * out from the response rather than from a parent.
     *
     * <p><b>Not the relation and not the flags.</b> Those are facts about a person <i>and a
     * child</i> — #12 changes them, one child at a time. Putting them here would mean changing
     * somebody's relation to all their children at once, which is not a thing that happens.
     *
     * <p><b>The uniqueness check skips the guardian being corrected.</b> Otherwise editing
     * somebody's name would refuse on their own phone number.
     *
     * <p><b>Gates 1 and 2.</b> No gate 4.
     */
    public GuardianDetailResponse updateGuardian(String guardianDocsId,
            GuardianUpdateRequest request) {

        //! step 1 - who is asking. requireUsable, because this writes.
        School school = currentSchool.requireUsable();

        //! step 2 - refuse a request that asks for nothing, BEFORE reading anything. A PATCH that
        //! changes nothing and answers 200 lets a client with a broken form look healthy, and this
        //! check costs no round trip.
        if (request.isEmpty()) {
            throw ApiException.badRequest("NOTHING_TO_UPDATE",
                    "Send a field to change. The relation and the flags are not here — they "
                            + "belong to one child rather than to this person, and #12 changes "
                            + "them.");
        }
        String id = guardianDocsId == null ? "" : guardianDocsId.trim();
        log.info("[updateGuardian] Step 1: Correcting guardian {} of school {}",
                id, school.getId());

        //! step 3 - the guardian, scoped by school IN THE QUERY and never checked after.
        // TODO: read guardian
        Guardian person = guardians.findByIdAndSchoolId(id, school.getId())
                .orElseThrow(() -> ApiException.notFound("GUARDIAN_NOT_FOUND",
                        "No guardian with id '" + id + "' in this school."));

        //! step 4 - somebody else may have corrected them while this caller was reading. It
        //! matters more here than on most writes: this row is SHARED, so two offices correcting
        //! one mother's number are genuinely likely to collide.
        if (!request.version().equals(person.getVersion())) {
            throw ApiException.conflict("CONCURRENT_MODIFICATION",
                    "'" + person.getFullName() + "' changed since you read them. Read the "
                            + "guardian again before correcting them, or you will overwrite what "
                            + "somebody else just wrote — and this row is shared by every child "
                            + "they belong to.");
        }

        //! step 5 - the name. BLANK IS REFUSED rather than clearing: the model requires one, and a
        //! guardian with no name is a row nobody can find.
        if (request.fullName() != null) {
            String newName = request.fullName().trim();
            if (newName.isEmpty()) {
                throw ApiException.badRequest("GUARDIAN_NAME_REQUIRED",
                        "A guardian's name cannot be removed. Send a new one, or leave the field "
                                + "out to keep '" + person.getFullName() + "'.");
            }
            person.setFullName(newName);
        }

        //! step 6 - the phone. NORMALISED BEFORE IT IS CHECKED, or the check is a lie.
        //!
        //! THE DUPLICATE CHECK SKIPS THEIR OWN NUMBER. Re-sending somebody the number they already
        //! have is not a conflict — without this, correcting a guardian's NAME would refuse on
        //! their own phone, which is the most ordinary use of this endpoint there is.
        //!
        //! LOOSE, the same as #7, and for the same reason: it only refuses, and refusing a number
        //! that is probably already somebody else's saves a duplicate human. #9 compares digits
        //! too, so the check a caller makes first and the refusal they get here agree.
        if (request.phoneNumber() != null) {
            String wanted = helper.normalisePhone(request.phoneNumber());

            if (wanted != null) {
                String digits = helper.digitsOf(wanted);
                boolean wholeNumber = digits.length() < FULL_PHONE_DIGITS;
                String needle = wholeNumber ? digits
                        : digits.substring(digits.length() - FULL_PHONE_DIGITS);

                // TODO: read guardian (is this number somebody ELSE's)
                Guardian holder = guardians
                        .findByLoosePhone(school.getId(), needle, wholeNumber, false, 2)
                        .stream()
                        .filter(other -> !other.getId().equals(person.getId()))
                        .findFirst().orElse(null);

                if (holder != null) {
                    throw ApiException.conflict("GUARDIAN_PHONE_TAKEN",
                            "'" + holder.getFullName() + "' already has the phone number "
                                    + holder.getPhoneNumber() + " in this school, which is the "
                                    + "same number as " + wanted + ".");
                }
            }
            //! "" REMOVES IT, and that is allowed: a guardian nothing identifies is a state #7
            //! can create too.
            person.setPhoneNumber(wanted);
        }

        //! step 7 - the email, the same way. WHOLE and lowercased, and theirs is skipped too.
        if (request.emailAddress() != null) {
            String wanted = TextHelper.lowercaseOrNull(request.emailAddress());

            if (wanted != null) {
                // TODO: read guardian (is this address somebody ELSE's)
                Guardian holder = guardians
                        .findBySchoolIdAndEmailAddress(school.getId(), wanted)
                        .filter(other -> !other.getId().equals(person.getId()))
                        .orElse(null);

                if (holder != null) {
                    throw ApiException.conflict("GUARDIAN_EMAIL_TAKEN",
                            "'" + holder.getFullName() + "' already has the email address "
                                    + wanted + " in this school.");
                }
            }
            person.setEmailAddress(wanted);
        }

        //! step 8 - the rest. "" clears, absent leaves alone — which is the whole reason this is a
        //! PATCH rather than a PUT.
        //!
        //! THE ALTERNATE NUMBER IS NORMALISED BUT NOT CHECKED. It is deliberately shared, a family
        //! landline, so two guardians holding it is the ordinary case rather than a mistake.
        if (request.alternatePhoneNumber() != null) {
            person.setAlternatePhoneNumber(helper.normalisePhone(request.alternatePhoneNumber()));
        }
        if (request.address() != null) {
            person.setAddress(TextHelper.blankToNull(request.address()));
        }
        if (request.occupation() != null) {
            person.setOccupation(TextHelper.blankToNull(request.occupation()));
        }
        //! CORRECTABLE BUT NOT REMOVABLE: "" is not a value an enum takes, and null already means
        //! "leave it alone". Recorded on the request record so a caller reads it before trying.
        if (request.preferredLanguage() != null) {
            person.setPreferredLanguage(request.preferredLanguage());
        }

        //! step 9 - save. Built above, written here.
        // TODO: update guardian
        Guardian saved = guardians.save(person);

        //! step 10 - and say how many children that just changed. A COUNT, not the documents:
        //! reading four whole students to print the number 4 would carry their dates of birth
        //! across to report a digit. #10 is where the children are listed.
        // TODO: read students (how many children does this correction reach)
        long affected = students.countBySchoolIdAndGuardiansGuardianDocsId(
                school.getId(), saved.getId());
        log.info("[updateGuardian] Step 2: Corrected '{}', which changes {} child(ren)",
                saved.getFullName(), affected);

        return GuardianDetailResponse.corrected(saved, affected,
                affected == 0
                        ? "'" + saved.getFullName() + "' is corrected. They are attached to no "
                                + "child, so this changed nothing else. " + NO_AUTHORIZATION_YET
                        : "'" + saved.getFullName() + "' is corrected, and that changed them for "
                                + affected + " child(ren) — this row is shared. "
                                + NO_AUTHORIZATION_YET);
    }
}
