package com.orbitastra.backend.services.crm;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.exceptions.ConflictException;
import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.crm.Admission;
import com.orbitastra.backend.models.crm.Inquiry;
import com.orbitastra.backend.models.crm.InquiryFollowUp;
import com.orbitastra.backend.models.crm.enums.InquiryStatus;
import com.orbitastra.backend.models.staff.Staff;
import com.orbitastra.backend.repositories.crm.AdmissionRepository;
import com.orbitastra.backend.repositories.crm.InquiryRepository;
import com.orbitastra.backend.repositories.staff.StaffRepository;

import lombok.RequiredArgsConstructor;

/**
 * Manages admission inquiries — the top of the CRM funnel. An inquiry is a lead
 * (walk-in / call / online) that a counselor works through the {@link InquiryStatus}
 * pipeline. Progress is tracked as a follow-up timeline ({@link InquiryFollowUp}):
 * each status change appends an entry with a note and the next follow-up date,
 * and the inquiry's top-level {@code status} mirrors the latest entry.
 */
@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final StaffRepository staffRepository;
    private final AdmissionRepository admissionRepository;

    public Inquiry createInquiry(Inquiry inquiry) {
        validateCounselor(inquiry.getCounselorId(), inquiry.getSchoolId());

        LocalDateTime now = LocalDateTime.now();
        if (inquiry.getFollowUps() == null) {
            inquiry.setFollowUps(new ArrayList<>());
        }
        // Stamp any initial follow-up entries supplied on creation.
        for (InquiryFollowUp f : inquiry.getFollowUps()) {
            if (f.getStatus() == null) f.setStatus(InquiryStatus.INQUIRY);
            if (f.getCounselorId() == null) f.setCounselorId(inquiry.getCounselorId());
            validateCounselor(f.getCounselorId(), inquiry.getSchoolId());
            validateNextFollowUpDate(f.getNextFollowUp());
            if (f.getRecordedAt() == null) f.setRecordedAt(now);
        }
        // Current status/owner mirror the latest entry; else default the stage to INQUIRY.
        if (!inquiry.getFollowUps().isEmpty()) {
            InquiryFollowUp last = inquiry.getFollowUps().get(inquiry.getFollowUps().size() - 1);
            inquiry.setStatus(last.getStatus());
            if (last.getCounselorId() != null) inquiry.setCounselorId(last.getCounselorId());
        } else if (inquiry.getStatus() == null) {
            inquiry.setStatus(InquiryStatus.INQUIRY);
        }

        inquiry.setCreatedAt(now);
        inquiry.setUpdatedAt(now);
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

    /** Inquiries (still open) whose latest follow-up entry is due on or before the given date. */
    public List<Inquiry> getFollowUpsDue(String schoolId, LocalDate asOf) {
        return inquiryRepository.findBySchoolId(schoolId).stream()
                .filter(i -> i.getStatus() != InquiryStatus.LOST && i.getStatus() != InquiryStatus.ADMITTED)
                .filter(i -> {
                    LocalDate due = latestNextFollowUp(i);
                    return due != null && !due.isAfter(asOf);
                })
                .toList();
    }

    /** Updates the lead's profile fields. Status changes go through {@link #recordFollowUp}. */
    public Inquiry updateInquiry(String id, Inquiry details) {
        Inquiry inquiry = getInquiryById(id);

        if (details.getCounselorId() != null) {
            validateCounselor(details.getCounselorId(), inquiry.getSchoolId());
            inquiry.setCounselorId(details.getCounselorId());
        }
        if (details.getGuardians() != null) inquiry.setGuardians(details.getGuardians());
        if (details.getStudentName() != null) inquiry.setStudentName(details.getStudentName());
        if (details.getSource() != null) inquiry.setSource(details.getSource());

        inquiry.setUpdatedAt(LocalDateTime.now());
        return inquiryRepository.save(inquiry);
    }

    /**
     * Records a follow-up on the lead: appends {status, note, nextFollowUp} to the
     * timeline and sets the inquiry's current status to the entry's status. This is
     * the single way a status change is captured, so every move keeps its note and
     * next-follow-up context.
     */
    @Transactional
    public Inquiry recordFollowUp(String id, InquiryFollowUp entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Follow-up details are required.");
        }
        Inquiry inquiry = getInquiryById(id);
        if (inquiry.getStatus() == InquiryStatus.ADMITTED && entry.getStatus() != null && entry.getStatus() != InquiryStatus.ADMITTED) {
            throw new IllegalArgumentException("Cannot change status of an inquiry that is already ADMITTED.");
        }
        if (entry.getStatus() == null) {
            entry.setStatus(inquiry.getStatus());
        }
        if (entry.getCounselorId() == null) {
            entry.setCounselorId(inquiry.getCounselorId());
        }
        validateCounselor(entry.getCounselorId(), inquiry.getSchoolId());
        validateNextFollowUpDate(entry.getNextFollowUp());
        entry.setRecordedAt(LocalDateTime.now());
        if (inquiry.getFollowUps() == null) {
            inquiry.setFollowUps(new ArrayList<>());
        }
        inquiry.getFollowUps().add(entry);
        inquiry.setStatus(entry.getStatus());
        if (entry.getCounselorId() != null) {
            inquiry.setCounselorId(entry.getCounselorId());
        }
        handleAdmittedStatus(inquiry);
        inquiry.setUpdatedAt(LocalDateTime.now());
        return inquiryRepository.save(inquiry);
    }

    /**
     * Moves an inquiry forward in the pipeline, but never backwards — a linked
     * action (e.g. an admission being created) can only advance the stage. The
     * move is logged as an auto follow-up entry.
     */
    @Transactional
    public Inquiry advanceStatus(String id, InquiryStatus target) {
        Inquiry inquiry = getInquiryById(id);
        InquiryStatus current = inquiry.getStatus();
        if (current == InquiryStatus.ADMITTED && target != InquiryStatus.ADMITTED) {
            throw new IllegalArgumentException("Cannot change status of an inquiry that is already ADMITTED.");
        }
        if (current == null || target.ordinal() > current.ordinal()) {
            applyAutomaticStatusChange(inquiry, target);
            handleAdmittedStatus(inquiry);
            inquiry.setUpdatedAt(LocalDateTime.now());
            return inquiryRepository.save(inquiry);
        }
        return inquiry;
    }

    /**
     * Atomically associates an admission created by AdmissionService and moves the
     * inquiry to ADMITTED. This also repairs the reference when an inquiry was
     * already marked ADMITTED before its admission was linked.
     */
    @Transactional
    public Inquiry linkAdmission(String inquiryId, String admissionId) {
        if (admissionId == null || admissionId.isBlank()) {
            throw new IllegalArgumentException("Admission ID is required when linking an inquiry.");
        }

        Inquiry inquiry = getInquiryById(inquiryId);
        if (inquiry.getAdmissionDocsId() != null && !inquiry.getAdmissionDocsId().isBlank()
                && !inquiry.getAdmissionDocsId().equals(admissionId)) {
            throw new ConflictException("Inquiry " + inquiryId
                    + " is already linked to admission " + inquiry.getAdmissionDocsId() + ".");
        }

        if (inquiry.getStatus() != InquiryStatus.ADMITTED) {
            applyAutomaticStatusChange(inquiry, InquiryStatus.ADMITTED);
        }
        inquiry.setAdmissionDocsId(admissionId);
        inquiry.setUpdatedAt(LocalDateTime.now());
        return inquiryRepository.save(inquiry);
    }

    private void handleAdmittedStatus(Inquiry inquiry) {
        if (inquiry.getStatus() == InquiryStatus.ADMITTED) {
            List<Admission> existingAdmissions = admissionRepository.findByInquiryDocsId(inquiry.getId());
            if (!existingAdmissions.isEmpty()) {
                inquiry.setAdmissionDocsId(existingAdmissions.get(0).getId());
            } else {
                Admission saved;
                try {
                    saved = admissionRepository.save(AdmissionFactory.fromInquiry(inquiry));
                } catch (DuplicateKeyException ex) {
                    throw new ConflictException(
                            "An admission already exists for inquiryDocsId: " + inquiry.getId(), ex);
                }
                inquiry.setAdmissionDocsId(saved.getId());
            }
        }
    }

    private void applyAutomaticStatusChange(Inquiry inquiry, InquiryStatus target) {
        if (inquiry.getFollowUps() == null) {
            inquiry.setFollowUps(new ArrayList<>());
        }
        inquiry.getFollowUps().add(InquiryFollowUp.builder()
                .status(target)
                .note("Auto-advanced to " + target)
                .counselorId(inquiry.getCounselorId())
                .recordedAt(LocalDateTime.now())
                .build());
        inquiry.setStatus(target);
    }

    public void deleteInquiry(String id) {
        Inquiry inquiry = getInquiryById(id);
        inquiryRepository.delete(inquiry);
    }

    private LocalDate latestNextFollowUp(Inquiry inquiry) {
        if (inquiry.getFollowUps() == null || inquiry.getFollowUps().isEmpty()) {
            return null;
        }
        return inquiry.getFollowUps().get(inquiry.getFollowUps().size() - 1).getNextFollowUp();
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

    private void validateNextFollowUpDate(LocalDate nextFollowUp) {
        if (nextFollowUp != null && nextFollowUp.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Next follow-up date cannot be in the past.");
        }
    }
}
