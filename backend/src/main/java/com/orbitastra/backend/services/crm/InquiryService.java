package com.orbitastra.backend.services.crm;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.crm.Inquiry;
import com.orbitastra.backend.models.crm.enums.InquiryStatus;
import com.orbitastra.backend.models.staff.Staff;
import com.orbitastra.backend.repositories.crm.InquiryRepository;
import com.orbitastra.backend.repositories.staff.StaffRepository;

import lombok.RequiredArgsConstructor;

/**
 * Manages admission inquiries — the top of the CRM funnel. An inquiry is a lead
 * (walk-in / call / online) that a counselor works through the {@link InquiryStatus}
 * pipeline; once it reaches admission it is linked to an {@link com.orbitastra.backend.models.crm.Admission}.
 */
@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final StaffRepository staffRepository;

    public Inquiry createInquiry(Inquiry inquiry) {
        validateCounselor(inquiry.getCounselorId(), inquiry.getSchoolId());

        if (inquiry.getStatus() == null) {
            inquiry.setStatus(InquiryStatus.INQUIRY);
        }
        inquiry.setCreatedAt(LocalDateTime.now());
        inquiry.setUpdatedAt(LocalDateTime.now());
        return inquiryRepository.save(inquiry);
    }

    public Inquiry getInquiryById(String id) {
        return inquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found with id: " + id));
    }

    public List<Inquiry> getInquiriesBySchool(String schoolId) {
        return inquiryRepository.findBySchoolId(schoolId);
    }

    public List<Inquiry> getInquiriesBySchoolAndStatus(String schoolId, InquiryStatus status) {
        return inquiryRepository.findBySchoolIdAndStatus(schoolId, status);
    }

    public List<Inquiry> getInquiriesByCounselor(String counselorId) {
        return inquiryRepository.findByCounselorId(counselorId);
    }

    /** Inquiries whose next follow-up is due on or before the given date. */
    public List<Inquiry> getFollowUpsDue(String schoolId, LocalDate asOf) {
        return inquiryRepository.findBySchoolIdAndNextFollowUpLessThanEqual(schoolId, asOf);
    }

    public Inquiry updateInquiry(String id, Inquiry details) {
        Inquiry inquiry = getInquiryById(id);

        if (details.getCounselorId() != null) {
            validateCounselor(details.getCounselorId(), inquiry.getSchoolId());
            inquiry.setCounselorId(details.getCounselorId());
        }
        if (details.getGuardians() != null) inquiry.setGuardians(details.getGuardians());
        if (details.getStudentName() != null) inquiry.setStudentName(details.getStudentName());
        if (details.getSource() != null) inquiry.setSource(details.getSource());
        if (details.getStatus() != null) inquiry.setStatus(details.getStatus());
        if (details.getNextFollowUp() != null) inquiry.setNextFollowUp(details.getNextFollowUp());
        if (details.getNotes() != null) inquiry.setNotes(details.getNotes());

        inquiry.setUpdatedAt(LocalDateTime.now());
        return inquiryRepository.save(inquiry);
    }

    /**
     * Moves an inquiry forward in the pipeline, but never backwards — a linked
     * action (e.g. an admission being created) can only advance the stage.
     */
    public Inquiry advanceStatus(String id, InquiryStatus target) {
        Inquiry inquiry = getInquiryById(id);
        if (target.ordinal() > inquiry.getStatus().ordinal()) {
            inquiry.setStatus(target);
            inquiry.setUpdatedAt(LocalDateTime.now());
            return inquiryRepository.save(inquiry);
        }
        return inquiry;
    }

    public void deleteInquiry(String id) {
        Inquiry inquiry = getInquiryById(id);
        inquiryRepository.delete(inquiry);
    }

    private void validateCounselor(String counselorId, String schoolId) {
        if (counselorId == null || counselorId.isBlank()) {
            return; // counselor is optional
        }
        if (schoolId == null || schoolId.isBlank()) {
            throw new IllegalArgumentException("School ID is required for an inquiry.");
        }
        Staff counselor = staffRepository.findById(counselorId)
                .orElseThrow(() -> new ResourceNotFoundException("Counselor (staff) not found with id: " + counselorId));
        if (!counselor.getSchoolId().equals(schoolId)) {
            throw new IllegalArgumentException("Counselor does not belong to the same school as the inquiry.");
        }
    }
}
