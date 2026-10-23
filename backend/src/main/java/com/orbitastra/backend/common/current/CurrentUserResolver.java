package com.orbitastra.backend.common.current;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.orbitastra.backend.common.error.exception.ApiException;
import com.orbitastra.backend.models.people.staff.Staff;
import com.orbitastra.backend.repositories.people.staff.StaffRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Works out which staff member the caller is acting as, from the {@code idtoken} cookie.
 *
 * <p>The companion to {@link CurrentSchoolResolver}, reading the same verified token: that one
 * answers <i>which school</i>, this one answers <i>who</i>. Until now there was no answer to the
 * second question at all — the header only ever carried a subdomain — so this is new, not a
 * conversion.
 *
 * <h2>Nothing calls it yet, and that is the point</h2>
 *
 * <p>It exists so that the first thing needing an actor — {@code createdBy} on a write, "my
 * classes", a teacher-scoped list — has one place to ask rather than inventing its own. It is
 * deliberately small: two questions, each in a "null when absent" and a "refuse when absent" form,
 * because a caller that can carry on without an actor and a caller that cannot are both real and
 * should not share an answer.
 *
 * <h2>The staff member is loaded scoped to the token's own school</h2>
 *
 * <p>{@code findByIdAndSchoolId}, never {@code findById}. A staff document id belongs to exactly
 * one tenant, and a token pairing school A with a staff member of school B must read as "not
 * found", not as school B's person. <b>The school for that scoping comes from the same token</b>,
 * not from {@link CurrentSchoolResolver} — the pair was signed together, and re-resolving the
 * school would both cost a second read and open the gap where the two disagree.
 *
 * <h2>What a token says is still not what is true</h2>
 *
 * <p>As with the school: {@code POST /local-user} issues a token naming any staff member to
 * anybody who asks. {@link #require()} proves the person named exists in the named school. It does
 * not prove the holder is that person. <b>Do not use this for permissions</b> until signing in
 * means something — the moment this decides what somebody may do, "who are you" becomes a question
 * the caller answers about themselves.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserResolver {

    /** The claim the cookie carries. Written by {@code LocalUserController}. */
    public static final String STAFF_CLAIM = "staffDocsId";

    private final StaffRepository staff;
    private final IdTokenCookie idToken;
    private final HttpServletRequest request; //! ← injected ONCE, at startup this is helping to pass the request http here..

    /**
     * The staff document id the caller is acting as, or null when nobody was chosen.
     *
     * <p>Null covers all three of no cookie, no {@code staffDocsId} claim, and the "— nobody —"
     * choice in the tester, which sends a blank. They are one answer to every caller: there is no
     * actor. A cookie that is present but invalid still throws.
     */
    public String staffDocsIdOrNull() {
        return idToken.claim(request, STAFF_CLAIM);
    }

    /** The staff document id, refusing when there is none. */
    public String requireStaffDocsId() {
        String staffDocsId = staffDocsIdOrNull();
        if (staffDocsId == null) {
            throw ApiException.badRequest("ACTOR_NOT_RESOLVED",
                    "This request needs to know which staff member is acting. Choose one and sign "
                            + "in, so the " + IdTokenCookie.COOKIE_NAME + " cookie carries a "
                            + STAFF_CLAIM + ".");
        }
        return staffDocsId;
    }

    /**
     * The staff document, or empty when no staff member was chosen.
     *
     * <p><b>Empty means "nobody was named". It does not mean "the named person is missing"</b> —
     * that is a refusal, because a token naming a staff member who is not in its school is wrong in
     * a way the caller needs told, not a quiet nobody.
     */
    public Optional<Staff> optional() {
        String staffDocsId = staffDocsIdOrNull();
        return staffDocsId == null ? Optional.empty() : Optional.of(load(staffDocsId));
    }

    /** The staff document, refusing when none was chosen or the one named cannot be found. */
    public Staff require() {
        return load(requireStaffDocsId());
    }

    /** Reads the staff member within the token's own school. See the class note on scoping. */
    private Staff load(String staffDocsId) {
        String schoolId = idToken.claim(request, CurrentSchoolResolver.SCHOOL_CLAIM);
        if (schoolId == null) {
            //! A STAFF ID WITHOUT A SCHOOL CANNOT BE READ SAFELY. Dropping the scope to satisfy the
            //! lookup would let a token naming only a staff member reach into any tenant, which is
            //! the whole bug findByIdAndSchoolId exists to prevent.
            throw ApiException.badRequest("TENANT_NOT_RESOLVED",
                    "The " + IdTokenCookie.COOKIE_NAME + " cookie names a staff member but no "
                            + "school, and a staff member can only be read within one. Sign in "
                            + "again with a school chosen.");
        }
        return staff.findByIdAndSchoolId(staffDocsId, schoolId)
                .orElseThrow(() -> ApiException.notFound("STAFF_NOT_FOUND",
                        "No staff member found for the id in the " + IdTokenCookie.COOKIE_NAME
                                + " cookie. They may have been removed since you signed in, or "
                                + "they belong to another school — sign in again."));
    }
}
