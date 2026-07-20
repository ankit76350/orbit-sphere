package com.orbitastra.backend.services.crm;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.crm.Admission;
import com.orbitastra.backend.models.crm.Inquiry;
import com.orbitastra.backend.models.crm.InquiryGuardian;
import com.orbitastra.backend.models.crm.enums.AdmissionStatus;
import com.orbitastra.backend.models.crm.enums.InquiryStatus;
import com.orbitastra.backend.models.student.Guardian;
import com.orbitastra.backend.models.student.GuardianLink;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.repositories.crm.AdmissionRepository;
import com.orbitastra.backend.services.student.GuardianService;
import com.orbitastra.backend.services.student.StudentService;
import com.orbitastra.backend.services.utils.AcademicYearResolver;

import lombok.RequiredArgsConstructor;

/**
 * Owns the admission stage of the CRM funnel. An admission is year-scoped
 * (via {@link AcademicYearResolver}) and optionally linked to the originating
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
    private final AcademicYearResolver academicYearResolver;

    public Admission createAdmission(Admission admission) {
        if (admission.getSchoolId() == null || admission.getSchoolId().isBlank()) {
            throw new IllegalArgumentException("School ID is required for an admission.");
        }
        admission.setAcademicYear(academicYearResolver
                .resolve(admission.getSchoolId(), admission.getAcademicYear(), admission.getAdmissionDate())
                .getName());

        // Scenario A — via inquiry: carry the applicant snapshot over (as-is unless
        // the admission already supplies its own), and advance the inquiry to ADMITTED.
        if (admission.getInquiryId() != null && !admission.getInquiryId().isBlank()) {
            if (admissionRepository.existsByInquiryId(admission.getInquiryId())) {
                throw new IllegalArgumentException("An admission already exists for inquiry ID: " + admission.getInquiryId());
            }
            Inquiry inquiry = inquiryService.getInquiryById(admission.getInquiryId());
            if (!inquiry.getSchoolId().equals(admission.getSchoolId())) {
                throw new IllegalArgumentException("Inquiry does not belong to the same school as the admission.");
            }
            if (admission.getStudentName() == null || admission.getStudentName().isBlank()) {
                admission.setStudentName(inquiry.getStudentName());
            }
            if (admission.getGuardians() == null || admission.getGuardians().isEmpty()) {
                admission.setGuardians(inquiry.getGuardians());
            }
            inquiryService.advanceStatus(inquiry.getId(), InquiryStatus.ADMITTED);
        }
        // Scenario B — direct admission: studentName/guardians/dob/gender come straight
        // from the request; nothing to copy.

        if (admission.getStatus() == null) {
            admission.setStatus(AdmissionStatus.PENDING);
        }
        admission.setCreatedAt(LocalDateTime.now());
        admission.setUpdatedAt(LocalDateTime.now());
        return admissionRepository.save(admission);
    }

    public Admission getAdmissionById(String id) {
        return admissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admission not found with id: " + id));
    }

    public List<Admission> getAdmissionsBySchool(String schoolId) {
        return admissionRepository.findBySchoolId(schoolId);
    }

    public List<Admission> getAdmissionsBySchoolAndAcademicYear(String schoolId, String academicYear) {
        return admissionRepository.findBySchoolIdAndAcademicYear(schoolId, academicYear);
    }

    public List<Admission> getAdmissionsBySchoolAndStatus(String schoolId, AdmissionStatus status) {
        return admissionRepository.findBySchoolIdAndStatus(schoolId, status);
    }

    public List<Admission> getAdmissionsByInquiry(String inquiryId) {
        return admissionRepository.findByInquiryId(inquiryId);
    }

    public Admission updateAdmission(String id, Admission details) {
        Admission admission = getAdmissionById(id);
        academicYearResolver.assertImmutable(admission.getAcademicYear(), details.getAcademicYear());

        if (details.getStatus() != null) admission.setStatus(details.getStatus());
        if (details.getDocuments() != null) admission.setDocuments(details.getDocuments());
        if (details.getAdmissionDate() != null) admission.setAdmissionDate(details.getAdmissionDate());
        if (details.getInquiryId() != null && !details.getInquiryId().isBlank()) {
            if (!details.getInquiryId().equals(admission.getInquiryId()) && admissionRepository.existsByInquiryId(details.getInquiryId())) {
                throw new IllegalArgumentException("An admission already exists for inquiry ID: " + details.getInquiryId());
            }
            admission.setInquiryId(details.getInquiryId());
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
    public Student convertToStudent(String admissionId, Student studentPayload) {
        Admission admission = getAdmissionById(admissionId);
        if (admission.getStudentId() != null && !admission.getStudentId().isBlank()) {
            throw new IllegalArgumentException(
                    "This admission has already been converted to student " + admission.getStudentId() + ".");
        }

        // Force the new student into the admission's school + academic year.
        studentPayload.setSchoolId(admission.getSchoolId());
        if (studentPayload.getAdmissionDate() == null) {
            studentPayload.setAdmissionDate(admission.getAdmissionDate());
        }
        StudentAcademicRecord record = studentPayload.getCurrentAcademicRecord();
        if (record == null) {
            record = new StudentAcademicRecord();
        }
        record.setAcademicYear(admission.getAcademicYear());
        studentPayload.setCurrentAcademicRecord(record);

        // Prefill identity from the admission's applicant snapshot when not overridden.
        if ((studentPayload.getName() == null || studentPayload.getName().isBlank())
                && admission.getStudentName() != null && !admission.getStudentName().isBlank()) {
            studentPayload.setName(admission.getStudentName());
        }
        if (studentPayload.getDob() == null) studentPayload.setDob(admission.getDob());
        if (studentPayload.getGender() == null) studentPayload.setGender(admission.getGender());

        // Materialise the admission's prospective guardians into real Guardian records,
        // then link them to the student (first one flagged primary). Any guardian links
        // already on the payload are preserved.
        List<GuardianLink> links = new ArrayList<>();
        if (studentPayload.getGuardians() != null) {
            links.addAll(studentPayload.getGuardians());
        }
        if (admission.getGuardians() != null) {
            int idx = 0;
            for (InquiryGuardian pg : admission.getGuardians()) {
                if (pg.getName() == null || pg.getName().isBlank()) continue;
                // Reuse an existing guardian if this person already exists (dedup),
                // otherwise create one — never store the same guardian twice.
                Guardian guardian = guardianService.findOrCreate(
                        admission.getSchoolId(), pg.getName(), pg.getPhone(),
                        pg.getEmail(), pg.getAddress(), pg.getOccupation());
                // Skip if this guardian is already linked (e.g. sibling already added it).
                boolean alreadyLinked = links.stream()
                        .anyMatch(l -> guardian.getId().equals(l.getGuardianId()));
                if (alreadyLinked) continue;
                links.add(GuardianLink.builder()
                        .guardianId(guardian.getId())
                        .relation(pg.getRelation())
                        .primary(idx == 0)
                        .build());
                idx++;
            }
        }
        studentPayload.setGuardians(links);

        Student saved = studentService.createStudent(studentPayload);

        admission.setStudentId(saved.getId());
        admission.setStatus(AdmissionStatus.CONFIRMED);
        admission.setUpdatedAt(LocalDateTime.now());
        admissionRepository.save(admission);

        return saved;
    }

    public void deleteAdmission(String id) {
        Admission admission = getAdmissionById(id);
        admissionRepository.delete(admission);
    }
}
