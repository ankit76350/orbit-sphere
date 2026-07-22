package com.orbitastra.backend.services.crm;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.crm.Admission;
import com.orbitastra.backend.models.crm.Inquiry;
import com.orbitastra.backend.models.crm.enums.AdmissionStatus;
import com.orbitastra.backend.models.crm.enums.InquiryStatus;
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

    public Admission createAdmission(Admission admission) {
        if (admission.getSchoolId() == null || admission.getSchoolId().isBlank()) {
            throw new IllegalArgumentException("School ID is required for an admission.");
        }

        // Scenario A — via inquiry: carry the applicant snapshot over (as-is unless
        // the admission already supplies its own), and advance the inquiry to ADMITTED.
        Inquiry inquiryToAdvance = null;
        if (admission.getInquiryDocsId() != null && !admission.getInquiryDocsId().isBlank()) {
            if (admissionRepository.existsByInquiryDocsId(admission.getInquiryDocsId())) {
                throw new IllegalArgumentException("An admission already exists for inquiry ID: " + admission.getInquiryDocsId());
            }
            Inquiry inquiry = inquiryService.getInquiryById(admission.getInquiryDocsId());
            if (!inquiry.getSchoolId().equals(admission.getSchoolId())) {
                throw new IllegalArgumentException("Inquiry does not belong to the same school as the admission.");
            }
            if (admission.getStudentName() == null || admission.getStudentName().isBlank()) {
                admission.setStudentName(inquiry.getStudentName());
            }
            if (admission.getGuardians() == null || admission.getGuardians().isEmpty()) {
                admission.setGuardians(inquiry.getGuardians());
            }
            inquiryToAdvance = inquiry;
        }

        // Validate required fields for direct and inquiry admissions
        if (admission.getStudentName() == null || admission.getStudentName().isBlank()) {
            throw new IllegalArgumentException("Student name is required for an admission.");
        }
        if (admission.getGender() == null) {
            admission.setGender(com.orbitastra.backend.models.student.enums.Gender.MALE);
        }
        if (admission.getGuardians() == null) {
            admission.setGuardians(java.util.Collections.emptyList());
        }
        if (admission.getAdmissionDate() == null) {
            admission.setAdmissionDate(java.time.LocalDate.now());
        }
        if (admission.getDocuments() == null) {
            admission.setDocuments(java.util.Collections.emptyList());
        }
        if (admission.getStatus() == null) {
            admission.setStatus(AdmissionStatus.PENDING);
        }

        admission.setCreatedAt(LocalDateTime.now());
        admission.setUpdatedAt(LocalDateTime.now());
        Admission saved = admissionRepository.save(admission);

        if (inquiryToAdvance != null) {
            inquiryService.advanceStatus(inquiryToAdvance.getId(), InquiryStatus.ADMITTED);
        }

        return saved;
    }

    public Admission getAdmissionById(String id) {
        return admissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admission not found with id: " + id));
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

        if (details.getStatus() != null) admission.setStatus(details.getStatus());
        if (details.getDocuments() != null) admission.setDocuments(details.getDocuments());
        if (details.getAdmissionDate() != null) admission.setAdmissionDate(details.getAdmissionDate());
        if (details.getInquiryDocsId() != null && !details.getInquiryDocsId().isBlank()) {
            if (!details.getInquiryDocsId().equals(admission.getInquiryDocsId()) && admissionRepository.existsByInquiryDocsId(details.getInquiryDocsId())) {
                throw new IllegalArgumentException("An admission already exists for inquiry ID: " + details.getInquiryDocsId());
            }
            admission.setInquiryDocsId(details.getInquiryDocsId());
        }
        if (details.getStudentName() != null) admission.setStudentName(details.getStudentName());
        if (details.getDob() != null) admission.setDob(details.getDob());
        if (details.getGender() != null) admission.setGender(details.getGender());
        if (details.getGuardians() != null) admission.setGuardians(details.getGuardians());
        // studentId is set only by convertToStudent — never edited directly here.

        admission.setUpdatedAt(LocalDateTime.now());
        return admissionRepository.save(admission);
    }

    /**
     * Confirms an admission by creating the enrolled {@link Student}. The student
     * is created under the admission's school and academic year; the resulting
     * student id is stamped back on the admission and its status set to CONFIRMED.
     * An admission can only be converted once.
     */
    @Transactional
    public StudentResponse convertToStudent(String admissionId, Student studentPayload,
                                            StudentAcademicRecord initialRecord) {
        Admission admission = getAdmissionById(admissionId);
        if (admission.getStudentId() != null && !admission.getStudentId().isBlank()) {
            throw new IllegalArgumentException(
                    "This admission has already been converted to student " + admission.getStudentId() + ".");
        }

        // Force the new student into the admission's school. No academic year is set here —
        // it is assigned separately after enrolment via POST /api/students/{id}/academic-records
        // (an admission carries no year). Any academic record explicitly supplied on the convert
        // request (initialRecord) is passed through and honoured by StudentService.
        studentPayload.setSchoolId(admission.getSchoolId());
        if (studentPayload.getAdmissionDate() == null) {
            studentPayload.setAdmissionDate(admission.getAdmissionDate());
        }

        // Prefill identity from the admission's applicant snapshot when not overridden.
        if ((studentPayload.getName() == null || studentPayload.getName().isBlank())
                && admission.getStudentName() != null && !admission.getStudentName().isBlank()) {
            studentPayload.setName(admission.getStudentName());
        }
        if (studentPayload.getDob() == null) studentPayload.setDob(admission.getDob());
        if (studentPayload.getGender() == null) studentPayload.setGender(admission.getGender());

        // Materialise the admission's prospective guardians into real (de-duplicated)
        // Guardian records and link them, preserving any links already on the payload.
        // Dedup + link-building is shared with direct student creation (GuardianService).
        List<GuardianService.GuardianDraft> drafts = admission.getGuardians() == null
                ? null
                : admission.getGuardians().stream()
                        .map(pg -> GuardianService.GuardianDraft.ofPerson(
                                pg.getName(), pg.getPhone(), pg.getEmail(),
                                pg.getAddress(), pg.getOccupation(), pg.getRelation()))
                        .toList();
        studentPayload.setGuardians(
                guardianService.buildDedupedLinks(admission.getSchoolId(),
                        studentPayload.getGuardians(), drafts));

        Student saved = studentService.persistStudent(studentPayload, initialRecord);

        admission.setStudentId(saved.getId());
        admission.setStatus(AdmissionStatus.CONFIRMED);
        admission.setUpdatedAt(LocalDateTime.now());
        admissionRepository.save(admission);

        return studentService.buildResponse(saved);
    }

    public void deleteAdmission(String id) {
        Admission admission = getAdmissionById(id);
        admissionRepository.delete(admission);
    }
}
