package com.orbitastra.backend.models.core;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.AuditedDocument;
import com.orbitastra.backend.models.core.enums.SchoolStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Root tenant document for one school using the SaaS platform.
 *
 * <p>{@code School.id} is copied into {@code SchoolBase.schoolId} on every
 * school-owned document. School does not extend SchoolBase because the tenant
 * root cannot contain a schoolId that points to itself.
 *
 * <p>{@code subdomain} is the globally unique normalized hostname label used to
 * resolve a request to this school. {@code encryptionKeyReference} stores only
 * a KMS/key-vault reference; cryptographic key material must never be stored in
 * this document.
 *
 * <p>Subscription and plan information is deliberately not embedded here.
 * Current subscription data is queried from SchoolSubscription using this
 * School id.
 */
@Document(collection = "schools")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class School extends AuditedDocument {

    // Example: "Orbit Astra International School"
    @NotBlank
    private String schoolName;

    // Example: "Rohan Shinde"
    @NotBlank
    private String accountHolderName;

    // Globally unique normalized tenant label. Example: "orbit-astra-school"
    @Indexed(unique = true)
    @NotBlank
    private String subdomain;

    // Every subdomain this school has previously answered to, kept so nobody else can claim one.
    //
    // A released label is not free to take. Links, bookmarks and saved logins keep pointing at
    // it, so if another school claimed it, the first school's users would land on a stranger's
    // login page and type their password into it. Holding the old label here makes that
    // impossible without any scheduled cleanup to forget.
    //
    // A school may reclaim its own old label — it is the only party that was ever behind it.
    // Added 2026-08-31 with the subdomain-change endpoint (#10).
    // Example: ["st-marys", "stmarys-pune"]
    @Indexed
    @Builder.Default
    private List<String> previousSubdomains = new ArrayList<>();

    // Public logo/CDN URL. Example: "https://cdn.example.com/schools/orbit/logo.png"
    private String logoUrl;

    // Primary contact number, stored normalized by the service. Example: "+919876543210"
    private String phoneNumber;

    // Primary contact email, stored lowercase by the service. Example: "admin@orbit-school.edu"
    private String emailAddress;

    // KMS or key-vault key identifier, never the key itself. Example: "kms://school/67aa15d9"
    private String encryptionKeyReference;

    // IETF language tag used by default. Example: "en-IN"
    @NotBlank
    private String defaultLocale;

    // IANA time-zone id used for school-local operations. Example: "Asia/Kolkata"
    @NotBlank
    private String defaultTimeZone;

    // Example: "12, MG Road"
    private String addressLine;

    // Example: "Pune"
    private String city;

    // Example: "Maharashtra"
    private String stateOrProvince;

    // Stored as text to support leading zeros and international formats. Example: "411001"
    private String postalCode;

    // ISO 3166-1 alpha-2 country code. Example: "IN"
    @NotBlank
    private String countryCode;

    // Operational tenant lifecycle. Example: SchoolStatus.ACTIVE
    @NotNull
    @Builder.Default
    private SchoolStatus status = SchoolStatus.PROVISIONING;

    // First successful platform activation time. Example: 2026-07-30T08:30:00Z
    private Instant activatedAt;

    // Most recent suspension time. Example: 2026-08-15T11:00:00Z
    private Instant suspendedAt;

    // Why the school is in its current status. Required when suspending, because a tenant
    // nobody may use with no reason written down gets switched back on by the next person
    // who is asked about it.
    //
    // Kept after reactivation rather than cleared, so "this school was suspended in August
    // for non-payment" survives being brought back. Added on 2026-08-27 with the suspend
    // endpoint; sixteen other models already carried a statusReason and School was the one
    // that did not. Example: "Non-payment. Third invoice unpaid past 60 days."
    private String statusReason;
}
