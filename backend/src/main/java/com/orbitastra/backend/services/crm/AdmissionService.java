package com.orbitastra.backend.services.crm;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.dto.crm.ConvertAdmissionRequest;
import com.orbitastra.backend.dto.student.StudentGuardianRequest;
import com.orbitastra.backend.exceptions.ConflictException;
import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.crm.Admission;
import com.orbitastra.backend.models.crm.Inquiry;
import com.orbitastra.backend.models.crm.embedded.InquiryGuardian;
import com.orbitastra.backend.models.crm.enums.AdmissionStatus;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.dto.student.StudentResponse;
import com.orbitastra.backend.repositories.crm.AdmissionRepository;
import com.orbitastra.backend.services.student.GuardianService;
import com.orbitastra.backend.services.student.StudentService;

import lombok.RequiredArgsConstructor;

/**
 * Owns the admission stage of the CRM funnel. An admission is a pre-enrolment
 * record — it carries no academic year (the year is assigned only once the
 * applicant becomes a student) — and is optionally linked to the originating
 * {@link Inquiry}. Confirming an admission converts it into an enrolled
 * {@link Student} through {@link StudentService} — the single bridge from CRM
 * into the student module.
 */
@Service
@RequiredArgsConstructor
public class AdmissionService {

    private final AdmissionRepository admissionRepository;
    private final InquiryService inquiryService;
    private final StudentService studentService;
    private final GuardianService guardianService;

    /**
     * Creates both direct and inquiry-backed admissions. When an inquiry is used,
     * this transaction covers the admission insert and inquiry status/reference
     * update; any failure rolls both writes back.
     */
    @Transactional
    public Admission createAdmission(Admission admission) {
        if (admission == null) {
            throw new IllegalArgumentException("Admission details are required.");
        }

        String inquiryDocsId = normalizeOptionalId(admission.getInquiryDocsId());
        admission.setInquiryDocsId(inquiryDocsId);
        Inquiry sourceInquiry = null;

        if (inquiryDocsId != null) {
            // Resolve first so a stale/non-existent reference is always reported as 404.
            sourceInquiry = inquiryService.getInquiryById(inquiryDocsId);
            validateRequestedSchool(admission.getSchoolId(), sourceInquiry);
            rejectDuplicateInquiry(inquiryDocsId);
            AdmissionFactory.mergeInquirySnapshot(admission, sourceInquiry);
        } else {
            // Keep the sparse unique field absent for direct admissions.
            admission.setInquiryDocsId(null);
            validateDirectAdmission(admission);
        }

        AdmissionFactory.applyCreationDefaults(admission);
        rejectDuplicateAdmissionNo(admission.getAdmissionNo());

        Admission saved;
        try {
            saved = admissionRepository.save(admission);
        } catch (DuplicateKeyException ex) {
            // The unique MongoDB index is the final guard against concurrent requests
            // that both pass the pre-insert exists check.
            if (duplicateKeyReferences(ex, "admissionNo")) {
                throw duplicateAdmissionNoConflict(admission.getAdmissionNo(), ex);
            }
            if (inquiryDocsId != null && duplicateKeyReferences(ex, "inquiryDocsId")) {
                throw duplicateInquiryConflict(inquiryDocsId, ex);
            }
            throw new ConflictException(
                    "An admission with the same admissionNo or inquiryDocsId already exists.", ex);
        }

        if (sourceInquiry != null) {
            inquiryService.linkAdmission(sourceInquiry.getId(), saved.getId());
        }
        return saved;
    }

    public Admission getAdmissionById(String id) {
        return admissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admission not found with id: " + id));
    }

    public Admission getAdmissionByAdmissionNo(String admissionNo) {
        String normalized = AdmissionFactory.normalizeAdmissionNo(admissionNo);
        if (normalized == null) {
            throw new IllegalArgumentException("Admission number is required.");
        }
        return admissionRepository.findByAdmissionNo(normalized)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Admission not found with admission number: " + normalized));
    }

    public List<Admission> getAdmissionsBySchool(String schoolId) {
        return admissionRepository.findBySchoolId(schoolId);
    }

    public List<Admission> getAdmissionsBySchoolAndStatus(String schoolId, AdmissionStatus status) {
        return admissionRepository.findBySchoolIdAndStatus(schoolId, status);
    }

    public List<Admission> getAdmissionsByInquiry(String inquiryId) {
        return admissionRepository.findByInquiryDocsId(inquiryId);
    }

    public Admission updateAdmission(String id, Admission details) {
        Admission admission = getAdmissionById(id);

        if (details.getAdmissionNo() != null) {
            String requestedAdmissionNo = AdmissionFactory.normalizeAdmissionNo(details.getAdmissionNo());
            if (requestedAdmissionNo == null) {
                throw new IllegalArgumentException("Admission number cannot be blank.");
            }
            if (!requestedAdmissionNo.equals(admission.getAdmissionNo())) {
                rejectDuplicateAdmissionNo(requestedAdmissionNo);
                admission.setAdmissionNo(requestedAdmissionNo);
            }
        }
        if (details.getStatus() != null) admission.setStatus(details.getStatus());
        if (details.getDocuments() != null) admission.setDocuments(details.getDocuments());
        if (details.getAdmissionDate() != null) admission.setAdmissionDate(details.getAdmissionDate());
        if (details.getStudentName() != null) admission.setStudentName(details.getStudentName());
        if (details.getDob() != null) admission.setDob(details.getDob());
        if (details.getGender() != null) admission.setGender(details.getGender());
        if (details.getGuardians() != null) admission.setGuardians(details.getGuardians());
        // studentDocsId is set only by convertToStudent — never edited directly here.

        admission.setUpdatedAt(LocalDateTime.now());
        try {
            return admissionRepository.save(admission);
        } catch (DuplicateKeyException ex) {
            if (duplicateKeyReferences(ex, "admissionNo")) {
                throw duplicateAdmissionNoConflict(admission.getAdmissionNo(), ex);
            }
            throw ex;
        }
    }

    /**
     * Confirms an admission by creating the enrolled {@link Student}. The student
     * is created under the admission's school and academic year; the resulting
     * student id is stamped back on the admission and its status set to CONFIRMED.
     * An admission can only be converted once.
     */
    @Transactional
    public StudentResponse convertToStudent(String admissionDocsId, ConvertAdmissionRequest request) {
        ConvertAdmissionRequest effectiveRequest = request == null
                ? new ConvertAdmissionRequest()
                : request;
        return convertToStudent(
                admissionDocsId,
                effectiveRequest.toStudent(),
                effectiveRequest.toAcademicRecord(),
                effectiveRequest.getGuardians());
    }

    private StudentResponse convertToStudent(String admissionDocsId, Student studentPayload,
                                             StudentAcademicRecord initialRecord,
                                             List<StudentGuardianRequest> guardianRequests) {
        Admission admission = getAdmissionById(admissionDocsId);
        if (admission.getStudentDocsId() != null && !admission.getStudentDocsId().isBlank()) {
            throw new ConflictException(
                    "This admission has already been converted to student " + admission.getStudentDocsId() + ".");
        }
        if (admission.getStatus() == AdmissionStatus.REJECTED) {
            throw new IllegalArgumentException("A rejected admission cannot be converted to a student.");
        }
        if (studentPayload == null) {
            studentPayload = Student.builder().build();
        }

        // Force the new student into the admission's school. The admission itself
        // carries no academic year, but an academic record explicitly supplied on
        // this conversion request is created by StudentService.
        studentPayload.setSchoolId(admission.getSchoolId());
        // Server-owned back-reference: callers cannot point a student at an arbitrary admission.
        studentPayload.setAdmissionDocsId(admission.getId());
        if (studentPayload.getAdmissionDate() == null) {
            studentPayload.setAdmissionDate(admission.getAdmissionDate());
        }

        String requestedAdmissionNo = AdmissionFactory.normalizeAdmissionNo(studentPayload.getAdmissionNo());
        boolean admissionNoWasMissing = AdmissionFactory.normalizeAdmissionNo(admission.getAdmissionNo()) == null;
        if (admissionNoWasMissing && requestedAdmissionNo != null) {
            admission.setAdmissionNo(requestedAdmissionNo);
        }
        AdmissionFactory.ensureAdmissionNo(admission);
        if (admissionNoWasMissing) {
            rejectDuplicateAdmissionNo(admission.getAdmissionNo());
        }
        if (requestedAdmissionNo != null && !requestedAdmissionNo.equals(admission.getAdmissionNo())) {
            throw new IllegalArgumentException(
                    "Student admissionNo must match the admission's admissionNo: " + admission.getAdmissionNo());
        }
        studentPayload.setAdmissionNo(admission.getAdmissionNo());

        // Prefill identity from the admission's applicant snapshot when not overridden.
        if ((studentPayload.getName() == null || studentPayload.getName().isBlank())
                && admission.getStudentName() != null && !admission.getStudentName().isBlank()) {
            studentPayload.setName(admission.getStudentName());
        }
        if (studentPayload.getDob() == null) studentPayload.setDob(admission.getDob());
        if (studentPayload.getGender() == null) studentPayload.setGender(admission.getGender());
        if (studentPayload.getDocuments() == null
                && admission.getDocuments() != null && !admission.getDocuments().isEmpty()) {
            studentPayload.setDocuments(new ArrayList<>(admission.getDocuments()));
        }

        // Materialise the admission's prospective guardians into real (de-duplicated)
        // Guardian records and link them, preserving any links already on the payload.
        // Dedup + link-building is shared with direct student creation (GuardianService).
        List<GuardianService.GuardianDraft> drafts =
                buildConversionGuardianDrafts(admission.getGuardians(), guardianRequests);
        studentPayload.setGuardians(
                guardianService.buildDedupedLinks(admission.getSchoolId(),
                        studentPayload.getGuardians(), drafts));

        Student saved = studentService.persistStudent(studentPayload, initialRecord);

        admission.setStudentDocsId(saved.getId());
        admission.setStatus(AdmissionStatus.CONFIRMED);
        admission.setUpdatedAt(LocalDateTime.now());
        admissionRepository.save(admission);
        if (admission.getInquiryDocsId() != null && !admission.getInquiryDocsId().isBlank()) {
            inquiryService.confirmEnrollment(admission.getInquiryDocsId(), admission.getId());
        }

        return studentService.buildResponse(saved);
    }

    private List<GuardianService.GuardianDraft> buildConversionGuardianDrafts(
            List<InquiryGuardian> admissionGuardians,
            List<StudentGuardianRequest> guardianRequests) {
        List<StudentGuardianRequest> requests = guardianRequests == null
                ? List.of()
                : guardianRequests.stream().filter(java.util.Objects::nonNull).toList();
        List<GuardianService.GuardianDraft> drafts = new ArrayList<>();

        // Existing Guardian document references go first so their relationship
        // flags win if the admission snapshot describes the same person.
        for (StudentGuardianRequest request : requests) {
            String guardianDocsId = normalizeOptionalId(request.getGuardianDocsId());
            if (guardianDocsId != null) {
                drafts.add(new GuardianService.GuardianDraft(
                        guardianDocsId,
                        request.getName(), request.getPhone(), request.getEmail(),
                        request.getAddress(), request.getOccupation(), request.getRelation(),
                        request.getPrimary(), request.getEmergencyContact(),
                        request.getPickupApproved(), request.getPortalAccess()));
            }
        }

        List<StudentGuardianRequest> detailRequests = requests.stream()
                .filter(request -> normalizeOptionalId(request.getGuardianDocsId()) == null)
                .toList();
        boolean[] matchedRequests = new boolean[detailRequests.size()];

        if (admissionGuardians != null) {
            for (InquiryGuardian admissionGuardian : admissionGuardians) {
                if (admissionGuardian == null) continue;
                int requestIndex = findMatchingGuardianRequest(
                        admissionGuardian, detailRequests, matchedRequests);
                StudentGuardianRequest override = requestIndex >= 0
                        ? detailRequests.get(requestIndex)
                        : null;
                if (requestIndex >= 0) matchedRequests[requestIndex] = true;
                drafts.add(toGuardianDraft(admissionGuardian, override));
            }
        }

        for (int i = 0; i < detailRequests.size(); i++) {
            if (!matchedRequests[i]) {
                drafts.add(toGuardianDraft(null, detailRequests.get(i)));
            }
        }
        return drafts;
    }

    private int findMatchingGuardianRequest(InquiryGuardian admissionGuardian,
                                            List<StudentGuardianRequest> requests,
                                            boolean[] matchedRequests) {
        for (int i = 0; i < requests.size(); i++) {
            if (matchedRequests[i]) continue;
            StudentGuardianRequest request = requests.get(i);
            if (sameNonBlank(admissionGuardian.getEmail(), request.getEmail())
                    || sameNonBlank(admissionGuardian.getPhone(), request.getPhone())
                    || sameNonBlank(admissionGuardian.getName(), request.getName())) {
                return i;
            }
        }
        return -1;
    }

    private GuardianService.GuardianDraft toGuardianDraft(
            InquiryGuardian base, StudentGuardianRequest override) {
        return new GuardianService.GuardianDraft(
                null,
                preferNonBlank(override == null ? null : override.getName(),
                        base == null ? null : base.getName()),
                preferNonBlank(override == null ? null : override.getPhone(),
                        base == null ? null : base.getPhone()),
                preferNonBlank(override == null ? null : override.getEmail(),
                        base == null ? null : base.getEmail()),
                preferNonBlank(override == null ? null : override.getAddress(),
                        base == null ? null : base.getAddress()),
                preferNonBlank(override == null ? null : override.getOccupation(),
                        base == null ? null : base.getOccupation()),
                override != null && override.getRelation() != null
                        ? override.getRelation()
                        : base == null ? null : base.getRelation(),
                override == null ? null : override.getPrimary(),
                override == null ? null : override.getEmergencyContact(),
                override == null ? null : override.getPickupApproved(),
                override == null ? null : override.getPortalAccess());
    }

    public void deleteAdmission(String id) {
        Admission admission = getAdmissionById(id);
        admissionRepository.delete(admission);
    }

    private void validateRequestedSchool(String requestedSchoolId, Inquiry inquiry) {
        if (inquiry.getSchoolId() == null || inquiry.getSchoolId().isBlank()) {
            throw new IllegalArgumentException("The inquiry is not associated with a school.");
        }
        if (requestedSchoolId != null && !requestedSchoolId.isBlank()
                && !inquiry.getSchoolId().equals(requestedSchoolId.trim())) {
            throw new IllegalArgumentException("Inquiry does not belong to the requested school.");
        }
    }

    private void validateDirectAdmission(Admission admission) {
        if (admission.getSchoolId() == null || admission.getSchoolId().isBlank()) {
            throw new IllegalArgumentException("schoolId is required when inquiryDocsId is not provided.");
        }
        admission.setSchoolId(admission.getSchoolId().trim());
        if (admission.getStudentName() == null || admission.getStudentName().isBlank()) {
            throw new IllegalArgumentException("studentName is required when inquiryDocsId is not provided.");
        }
    }

    private void rejectDuplicateInquiry(String inquiryDocsId) {
        if (admissionRepository.existsByInquiryDocsId(inquiryDocsId)) {
            throw duplicateInquiryConflict(inquiryDocsId, null);
        }
    }

    private void rejectDuplicateAdmissionNo(String admissionNo) {
        if (admissionRepository.existsByAdmissionNo(admissionNo)) {
            throw duplicateAdmissionNoConflict(admissionNo, null);
        }
    }

    private ConflictException duplicateAdmissionNoConflict(String admissionNo, Throwable cause) {
        String message = "An admission already exists with admissionNo: " + admissionNo;
        return cause == null ? new ConflictException(message) : new ConflictException(message, cause);
    }

    private ConflictException duplicateInquiryConflict(String inquiryDocsId, Throwable cause) {
        String message = "An admission already exists for inquiryDocsId: " + inquiryDocsId;
        return cause == null ? new ConflictException(message) : new ConflictException(message, cause);
    }

    private String normalizeOptionalId(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String preferNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }

    private boolean sameNonBlank(String left, String right) {
        return left != null && !left.isBlank()
                && right != null && !right.isBlank()
                && left.trim().equalsIgnoreCase(right.trim());
    }

    private boolean duplicateKeyReferences(DuplicateKeyException ex, String fieldName) {
        return ex.getMessage() != null && ex.getMessage().contains(fieldName);
    }
}
