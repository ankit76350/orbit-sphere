package com.orbitastra.backend.models.new_new.identity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.identity.enums.PersonType;
import com.orbitastra.backend.models.new_new.identity.enums.UserAccountStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One person's login to the school's system.
 *
 * <p>An account is never a person on its own. It always belongs to a Staff,
 * Guardian or Student record that already exists, named by {@code personType} and
 * {@code personDocsId}. The person's name, phone and address stay on that record
 * and are not copied here, so there is only ever one place to correct them.
 *
 * <p>One person gets one account, which the unique index enforces. A parent with
 * three children at the school still signs in once.
 *
 * <p>{@code roleDocsIds} is what the person may do. A person may hold more than
 * one role at a time, such as a teacher who is also a hostel warden, and what
 * they may do is everything their roles allow added together. Roles only ever
 * grant; none of them takes a permission away from another. An empty list means
 * the person can sign in and do nothing, which is where every new account starts.
 *
 * <p>The roles are kept as a plain list here rather than as their own collection
 * of grant records. That is a deliberate trade. It keeps a permission check to one
 * read, and it means the account document alone says everything about what
 * somebody may do. What it gives up is a per-role history: the account records who
 * last changed it through {@code updatedByDocsId}, but not who added one
 * particular role, when, or why, and a role cannot be set to end on a date by
 * itself. A short-term role has to be taken away by hand when it is no longer
 * needed.
 *
 * <p>Sign-in is by email or by phone number, and at least one of the two must be
 * filled in. Both are kept in a tidied-up form in {@code normalizedEmail} and
 * {@code normalizedPhone} so that "Priya@School.in" and "priya@school.in" are
 * recognised as the same person. Those tidied fields are for finding the account
 * only; what is shown on screen comes from the Staff or Guardian record.
 *
 * <p>The password is only ever kept as {@code passwordHash}, made with a slow
 * hashing algorithm such as bcrypt or argon2. The real password is never saved,
 * never logged and never sent back out, not even to an administrator. Anybody who
 * needs to get somebody back in resets the password instead of reading it.
 *
 * <p>This account is not tied to an academic year. A teacher signing in during
 * April 2027 uses the same account they used in 2026, so the model sits on
 * SchoolBase rather than on AcademicStudentSchoolBase.
 *
 * <p>{@code failedLoginCount} and {@code lockedUntil} are what hold the door shut
 * after too many wrong passwords. The count goes back to zero on any successful
 * sign-in. This is a delay, not a punishment, and it clears by itself.
 *
 * <p>{@code passwordChangedAt} matters more than it looks. Changing a password
 * must end every open session on every device, and comparing a session's start
 * time against this field is how that is done.
 *
 * <p>The service checks that at least one of email or phone is present, that the
 * person named actually exists in the school, that every role in
 * {@code roleDocsIds} belongs to the same school and is active, that the last
 * account able to manage user access is never suspended or stripped of that role,
 * and that a password is checked against the school's password rules before it is
 * hashed.
 */
@Document(collection = "user_accounts")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_account_email_uniq",
                def = "{'schoolId': 1, 'normalizedEmail': 1}",
                unique = true,
                partialFilter = "{'normalizedEmail': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_account_phone_uniq",
                def = "{'schoolId': 1, 'normalizedPhone': 1}",
                unique = true,
                partialFilter = "{'normalizedPhone': {'$type': 'string'}}"),
        @CompoundIndex(
                name = "school_account_person_uniq",
                def = "{'schoolId': 1, 'personType': 1, 'personDocsId': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_account_status_idx",
                def = "{'schoolId': 1, 'status': 1, 'personType': 1}"),
        @CompoundIndex(
                name = "school_account_role_idx",
                def = "{'schoolId': 1, 'roleDocsIds': 1, 'status': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccount extends SchoolBase {

    // What kind of person this login belongs to. Example: PersonType.STAFF
    @NotNull
    private PersonType personType;

    // Links to Staff.id, Guardian.id or Student.id, depending on personType.
    // Example: "67aa15d9dc3f7d0044444444"
    @NotBlank
    private String personDocsId;

    // Links to Role.id, one for each role this person holds. What they may do is
    // everything their roles allow added together, so holding more roles can only
    // ever allow more, never less. Empty means the person may sign in and do
    // nothing, which is what a new account starts as.
    // Example: ["67b11224dc3f7d0022334455"]
    @Builder.Default
    private List<String> roleDocsIds = new ArrayList<>();

    // Email tidied to lower case with spaces trimmed, used to find the account
    // at sign-in. Null when the person signs in by phone.
    // Example: "priya.sharma@orbitastra.edu.in"
    private String normalizedEmail;

    // Phone tidied to international form, used to find the account at sign-in.
    // Null when the person signs in by email. Example: "+919876543210"
    private String normalizedPhone;

    // The password after slow hashing. The real password is never saved.
    // Null while the account is still INVITED. Example: "$2b$12$Xy7f..."
    private String passwordHash;

    // Example: UserAccountStatus.ACTIVE
    @NotNull
    @Builder.Default
    private UserAccountStatus status = UserAccountStatus.INVITED;

    // True when the person has to pick a new password before doing anything
    // else, such as after an administrator reset it. Example: false
    @NotNull
    @Builder.Default
    private Boolean mustChangePassword = false;

    // When the password was last changed. Any session that started before this
    // moment is no longer valid. Example: 2026-08-14T06:20:00Z
    private Instant passwordChangedAt;

    // Wrong passwords in a row. Goes back to zero on any successful sign-in.
    // Example: 0
    @NotNull
    @Builder.Default
    private Integer failedLoginCount = 0;

    // Sign-in is refused until this moment. Null when the account is not held
    // shut. Example: 2026-08-14T07:05:00Z
    private Instant lockedUntil;

    // Last successful sign-in. Example: 2026-08-14T05:40:00Z
    private Instant lastLoginAt;

    // Language this person wants the app and their messages in.
    // Example: "en-IN"
    private String preferredLanguage;

    // Time zone used to show dates and to decide when to send them a message.
    // Example: "Asia/Kolkata"
    private String timeZone;

    // Whether this person has to give a second factor as well as a password.
    // The factors themselves are not modelled yet. Example: false
    @NotNull
    @Builder.Default
    private Boolean twoFactorRequired = false;

    // Links to the staff identity that suspended or closed the account, and why.
    // Example: "67aa15d9dc3f7d0055555555"
    private String statusChangedByDocsId;

    // Example: 2026-08-14T09:15:00Z
    private Instant statusChangedAt;

    // Needed whenever the status becomes SUSPENDED or DISABLED.
    // Example: "Left the school at the end of the 2026-2027 session."
    private String statusReason;
}
