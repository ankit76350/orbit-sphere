package com.orbitastra.backend.controllers.crm;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.dto.crm.CreateInquiryRequest;
import com.orbitastra.backend.dto.crm.FollowUpRequest;
import com.orbitastra.backend.dto.crm.InquiryGuardianRequest;
import com.orbitastra.backend.dto.crm.UpdateInquiryRequest;
import com.orbitastra.backend.models.crm.Inquiry;
import com.orbitastra.backend.models.crm.InquiryFollowUp;
import com.orbitastra.backend.models.crm.enums.InquiryStatus;
import com.orbitastra.backend.services.crm.InquiryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @PostMapping
    public ResponseEntity<Inquiry> createInquiry(@Valid @RequestBody CreateInquiryRequest request) {
        Inquiry inquiry = Inquiry.builder()
                .schoolId(request.getSchoolId())
                .studentName(request.getStudentName())
                .guardians(InquiryGuardianRequest.toModels(request.getGuardians()))
                .source(request.getSource())
                .counselorId(request.getCounselorId())
                .status(request.getStatus())
                .followUps(request.getFollowUps() == null ? null
                        : request.getFollowUps().stream().map(FollowUpRequest::toModel)
                                .collect(java.util.stream.Collectors.toList()))
                .build();
        return new ResponseEntity<>(inquiryService.createInquiry(inquiry), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Inquiry> getInquiryById(@PathVariable String id) {
        return ResponseEntity.ok(inquiryService.getInquiryById(id));
    }

    @GetMapping("/school/{schoolId}")
    public ResponseEntity<List<Inquiry>> getInquiriesBySchool(@PathVariable String schoolId) {
        return ResponseEntity.ok(inquiryService.getInquiriesBySchool(schoolId));
    }

    @GetMapping("/school/{schoolId}/status/{status}")
    public ResponseEntity<List<Inquiry>> getInquiriesBySchoolAndStatus(
            @PathVariable String schoolId,
            @PathVariable InquiryStatus status) {
        return ResponseEntity.ok(inquiryService.getInquiriesBySchoolAndStatus(schoolId, status));
    }

    @GetMapping("/counselor/{counselorId}")
    public ResponseEntity<List<Inquiry>> getInquiriesByCounselor(@PathVariable String counselorId) {
        return ResponseEntity.ok(inquiryService.getInquiriesByCounselor(counselorId));
    }

    @GetMapping("/school/{schoolId}/follow-ups")
    public ResponseEntity<List<Inquiry>> getFollowUpsDue(
            @PathVariable String schoolId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ResponseEntity.ok(inquiryService.getFollowUpsDue(schoolId, asOf != null ? asOf : LocalDate.now()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Inquiry> updateInquiry(@PathVariable String id, @Valid @RequestBody UpdateInquiryRequest request) {
        Inquiry details = Inquiry.builder()
                .counselorId(request.getCounselorId())
                .guardians(InquiryGuardianRequest.toModels(request.getGuardians()))
                .studentName(request.getStudentName())
                .source(request.getSource())
                .build();
        return ResponseEntity.ok(inquiryService.updateInquiry(id, details));
    }

    /** Records a follow-up / status change: {status, note, nextFollowUp}. */
    @PostMapping("/{id}/follow-ups")
    public ResponseEntity<Inquiry> recordFollowUp(@PathVariable String id, @Valid @RequestBody FollowUpRequest request) {
        InquiryFollowUp entry = request.toModel();
        return ResponseEntity.ok(inquiryService.recordFollowUp(id, entry));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteInquiry(@PathVariable String id) {
        inquiryService.deleteInquiry(id);
        return ResponseEntity.ok(Map.of("message", "Inquiry deleted successfully."));
    }
}
