package com.orbitastra.backend.models.plans.enums;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Every capability a plan can grant.
 *
 * <p><b>A fixed list, not free text, and that is the point.</b> A feature code is not data
 * somebody invents — it points at behaviour in this codebase, and a plan cannot grant a
 * capability the software does not have. So the set is closed by definition.
 *
 * <p>It used to be a {@code String}. That accepted {@code STUDNET_MANAGEMENT} with a {@code 200},
 * the plan looked perfect on every screen, and the entitlement service — asking for
 * {@code STUDENT_MANAGEMENT} — never found it. The school was locked out of something they had
 * paid for, by one transposed letter, and nobody would find out until they rang up. As an enum
 * that is a {@code 400} listing the accepted values, at the moment the plan is written.
 *
 * <h2>The rule for changing this list</h2>
 *
 * <p><b>Add constants. Never rename or remove one.</b> The name is what is stored in every
 * existing {@code PlanDefinition}, and plan versions are immutable once published — a renamed
 * constant would orphan the feature on every plan already sold, silently, with the rows still
 * looking valid.
 *
 * <p>Adding one needs a deploy, which costs nothing: a new feature always needed a deploy,
 * because the software has to actually do the new thing before a plan can sell it.
 *
 * <h2>What is not here</h2>
 *
 * <p>The things every plan includes and nobody is charged separately for — the tenant itself,
 * accounts and sign-in, the audit trail, the plan and billing machinery. A feature nobody can be
 * sold is not a feature, and listing it would invite somebody to switch it off.
 */
public enum FeatureCode {

    //! teaching and learning ------------------------------------------------------------

    STUDENT_MANAGEMENT("Student management",
            "Student records, guardians, enrolment and transfers.", UsageMetric.ACTIVE_STUDENTS),

    ACADEMICS("Academic structure",
            "Classes, sections, subjects and the curriculum.", null),

    ATTENDANCE("Attendance",
            "Daily and period attendance, with registers and shortfall reports.", null),

    TIMETABLE("Timetable",
            "Period scheduling, teacher allocation and substitutions.", null),

    EXAMINATIONS("Examinations",
            "Exam schedules, marks entry, grading and report cards.", null),

    HOMEWORK("Homework",
            "Assignments set to a class, with submissions and marking.", null),

    //! money ----------------------------------------------------------------------------

    FEE_MANAGEMENT("Fee management",
            "Student fee structures, invoices, receipts and dues.", null),

    PAYROLL("Payroll",
            "Salary structures, payslips and statutory deductions.", UsageMetric.ACTIVE_STAFF),

    //! people ---------------------------------------------------------------------------

    STAFF_MANAGEMENT("Staff management",
            "Staff records, departments, leave and performance reviews.",
            UsageMetric.ACTIVE_STAFF),

    ADMISSIONS_CRM("Admissions",
            "Enquiries, follow-ups and the admission pipeline.", null),

    //! daily operations -----------------------------------------------------------------

    TRANSPORT("Transport",
            "Routes, stops, vehicles, trips and travel attendance.", UsageMetric.VEHICLES),

    LIBRARY("Library",
            "Catalogue, issues, returns and fines.", UsageMetric.LIBRARY_TITLES),

    HOSTEL("Hostel",
            "Blocks, rooms, bed allocation and hostel attendance.", UsageMetric.HOSTEL_BEDS),

    MESS("Mess",
            "Menus, meal plans and mess attendance.", null),

    HEALTH("Health",
            "Health profiles, clinic visits, medication and alerts.", null),

    FRONT_OFFICE("Front office",
            "Visitors, gate passes, enquiries and the call log.", null),

    //! stores and premises --------------------------------------------------------------

    INVENTORY("Inventory",
            "Stock items, issues, returns and stock levels.", null),

    PROCUREMENT("Procurement",
            "Vendors, purchase requests, orders and goods received.", null),

    FACILITIES("Facilities",
            "Rooms and resources, bookings, inspections and maintenance.", null),

    //! communication and records --------------------------------------------------------

    NOTIFICATIONS("Notifications",
            "Announcements and alerts to staff, students and guardians.",
            UsageMetric.SMS_MESSAGES),

    DOCUMENTS("Documents",
            "Document storage, templates and issued certificates.",
            UsageMetric.STORAGE_MEGABYTES),

    GALLERY("Gallery",
            "Photo albums and media shared with guardians.", UsageMetric.STORAGE_MEGABYTES),

    FEEDBACK("Feedback",
            "Feedback campaigns, and the reporting channel to the principal.", null),

    STUDENT_LIFE("Student life",
            "Houses, clubs, activities, achievements and discipline.", null);

    private final String label;
    private final String description;
    private final UsageMetric usageMetric;

    FeatureCode(String label, String description, UsageMetric usageMetric) {
        this.label = label;
        this.description = description;
        this.usageMetric = usageMetric;
    }

    /** What a person calls it, on a pricing page or a plan comparison. Example: "Transport" */
    public String getLabel() {
        return label;
    }

    /** One sentence a school would understand, for the same screens. */
    public String getDescription() {
        return description;
    }

    /**
     * The counter a limit on this feature is measured in, or null when the feature has no
     * sensible number.
     *
     * <p>Null is a real answer, not a gap. "Attendance" is either included or it is not — there
     * is nothing to count — so a {@code usageLimit} on it is refused rather than stored as a
     * number nothing reads.
     */
    public UsageMetric getUsageMetric() {
        return usageMetric;
    }

    /** True when a limit means anything for this feature. */
    public boolean isMeasurable() {
        return usageMetric != null;
    }

    /** Every value, for an error message that tells the caller what would have worked. */
    public static String allNames() {
        return Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));
    }
}
