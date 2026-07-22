package com.orbitastra.backend.services.crm;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.orbitastra.backend.models.crm.Admission;
import com.orbitastra.backend.models.crm.Inquiry;
import com.orbitastra.backend.models.crm.InquiryGuardian;
import com.orbitastra.backend.models.crm.enums.AdmissionStatus;

/**
 * Centralises admission snapshot creation and request-over-inquiry merge rules.
 * Both explicit admission creation and the inquiry status workflow use this
 * factory so defaults and copied fields cannot drift between code paths.
 */
final class AdmissionFactory {

    private AdmissionFactory() {
    }

    static Admission fromInquiry(Inquiry inquiry) {
        Admission admission = Admission.builder()
                .inquiryDocsId(inquiry.getId())
                .build();
        mergeInquirySnapshot(admission, inquiry);
        applyCreationDefaults(admission);
        return admission;
    }

    /**
     * Uses the inquiry as the base snapshot and overlays fields supplied on the
     * admission. Admission-only fields (DOB, gender, documents, etc.) already live
     * on {@code admission} and are therefore preserved.
     */
    static void mergeInquirySnapshot(Admission admission, Inquiry inquiry) {
        admission.setSchoolId(inquiry.getSchoolId());
        admission.setInquiryDocsId(inquiry.getId());

        if (isBlank(admission.getStudentName())) {
            admission.setStudentName(inquiry.getStudentName());
        }
        admission.setGuardians(mergeGuardians(inquiry.getGuardians(), admission.getGuardians()));
    }

    static void applyCreationDefaults(Admission admission) {
        ensureAdmissionNo(admission);
        admission.setGuardians(copyGuardians(admission.getGuardians()));
        admission.setDocuments(admission.getDocuments() == null
                ? new ArrayList<>()
                : new ArrayList<>(admission.getDocuments()));
        if (admission.getAdmissionDate() == null) {
            admission.setAdmissionDate(LocalDate.now());
        }
        if (admission.getStatus() == null) {
            admission.setStatus(AdmissionStatus.PENDING);
        }

        LocalDateTime now = LocalDateTime.now();
        admission.setCreatedAt(now);
        admission.setUpdatedAt(now);
    }

    static void ensureAdmissionNo(Admission admission) {
        String normalized = normalizeAdmissionNo(admission.getAdmissionNo());
        admission.setAdmissionNo(normalized != null ? normalized : generateAdmissionNo());
    }

    static String normalizeAdmissionNo(String admissionNo) {
        return isBlank(admissionNo) ? null : admissionNo.trim();
    }

    private static String generateAdmissionNo() {
        String suffix = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase(Locale.ROOT);
        return "ADM-" + LocalDate.now().getYear() + "-" + suffix;
    }

    private static List<InquiryGuardian> mergeGuardians(List<InquiryGuardian> inquiryGuardians,
                                                         List<InquiryGuardian> requestGuardians) {
        List<InquiryGuardian> merged = copyGuardians(inquiryGuardians);
        if (requestGuardians == null || requestGuardians.isEmpty()) {
            return merged;
        }

        for (InquiryGuardian requested : requestGuardians) {
            if (requested == null) {
                continue;
            }
            int existingIndex = findMatchingGuardian(merged, requested);
            if (existingIndex >= 0) {
                merged.set(existingIndex, overlayGuardian(merged.get(existingIndex), requested));
            } else {
                merged.add(copyGuardian(requested));
            }
        }
        return merged;
    }

    private static int findMatchingGuardian(List<InquiryGuardian> guardians, InquiryGuardian requested) {
        for (int i = 0; i < guardians.size(); i++) {
            InquiryGuardian existing = guardians.get(i);
            if (sameNonBlank(existing.getEmail(), requested.getEmail())
                    || sameNonBlank(existing.getPhone(), requested.getPhone())
                    || sameNonBlank(existing.getName(), requested.getName())) {
                return i;
            }
        }
        return -1;
    }

    private static InquiryGuardian overlayGuardian(InquiryGuardian base, InquiryGuardian override) {
        return InquiryGuardian.builder()
                .name(preferNonBlank(override.getName(), base.getName()))
                .relation(override.getRelation() != null ? override.getRelation() : base.getRelation())
                .phone(preferNonBlank(override.getPhone(), base.getPhone()))
                .email(preferNonBlank(override.getEmail(), base.getEmail()))
                .address(preferNonBlank(override.getAddress(), base.getAddress()))
                .occupation(preferNonBlank(override.getOccupation(), base.getOccupation()))
                .build();
    }

    private static List<InquiryGuardian> copyGuardians(List<InquiryGuardian> guardians) {
        List<InquiryGuardian> copy = new ArrayList<>();
        if (guardians != null) {
            guardians.stream()
                    .filter(java.util.Objects::nonNull)
                    .map(AdmissionFactory::copyGuardian)
                    .forEach(copy::add);
        }
        return copy;
    }

    private static InquiryGuardian copyGuardian(InquiryGuardian guardian) {
        return InquiryGuardian.builder()
                .name(guardian.getName())
                .relation(guardian.getRelation())
                .phone(guardian.getPhone())
                .email(guardian.getEmail())
                .address(guardian.getAddress())
                .occupation(guardian.getOccupation())
                .build();
    }

    private static String preferNonBlank(String preferred, String fallback) {
        return isBlank(preferred) ? fallback : preferred;
    }

    private static boolean sameNonBlank(String left, String right) {
        return !isBlank(left) && !isBlank(right) && left.trim().equalsIgnoreCase(right.trim());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
