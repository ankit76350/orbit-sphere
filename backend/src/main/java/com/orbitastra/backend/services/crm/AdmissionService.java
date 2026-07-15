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
import com.orbitastra.backend.repositories.crm.AdmissionRepository;
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
    private final AcademicYearResolver academicYearResolver;

    public Admission createAdmission(Admission admission) {
        if (admission.getSchoolId() == null || admission.getSchoolId().isBlank()) {
            throw new IllegalArgumentException("School ID is required for an admission.");
        }
        admission.setAcademicYear(academicYearResolver
                .resolve(admission.getSchoolId(), admission.getAcademicYear(), admission.getAdmissionDate())
                .getName());

        // Link to the originating inquiry and advance its stage to ADMISSION.
        if (admission.getInquiryId() != null && !admission.getInquiryId().isBlank()) {
            Inquiry inquiry = inquiryService.getInquiryById(admission.getInquiryId());
            if (!inquiry.getSchoolId().equals(admission.getSchoolId())) {
                throw new IllegalArgumentException("Inquiry does not belong to the same school as the admission.");
            }
            inquiryService.advanceStatus(inquiry.getId(), InquiryStatus.ADMISSION);
        }

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
        if (details.getInquiryId() != null) admission.setInquiryId(details.getInquiryId());
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
