package com.orbitastra.backend.controllers.finance;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.dto.finance.CreateFeeRequest;
import com.orbitastra.backend.dto.finance.UpdateFeeRequest;
import com.orbitastra.backend.models.finance.FeeInvoice;
import com.orbitastra.backend.services.finance.FeeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/fees")
@RequiredArgsConstructor
public class FeeController {

    private final FeeService feeService;

    @PostMapping
    public ResponseEntity<FeeInvoice> createFee(@Valid @RequestBody CreateFeeRequest request) {
        FeeInvoice fee = FeeInvoice.builder()
                .schoolId(request.getSchoolId())
                .academicYear(request.getAcademicYear())
                .studentDocsId(request.getStudentDocsId())
                .type(request.getType())
                .amount(request.getAmount())
                .discount(request.getDiscount())
                .dueDate(request.getDueDate())
                .build();
        FeeInvoice created = feeService.createFee(fee);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FeeInvoice> getFeeById(@PathVariable String id) {
        FeeInvoice fee = feeService.getFeeById(id);
        return ResponseEntity.ok(fee);
    }

    @GetMapping("/student/{studentDocsId}")
    public ResponseEntity<List<FeeInvoice>> getFeesByStudent(@PathVariable String studentDocsId) {
        List<FeeInvoice> fees = feeService.getFeesByStudent(studentDocsId);
        return ResponseEntity.ok(fees);
    }

    @GetMapping("/school/{schoolId}")
    public ResponseEntity<List<FeeInvoice>> getFeesBySchool(@PathVariable String schoolId) {
        List<FeeInvoice> fees = feeService.getFeesBySchool(schoolId);
        return ResponseEntity.ok(fees);
    }

    @GetMapping("/school/{schoolId}/academic-year/{academicYear}")
    public ResponseEntity<List<FeeInvoice>> getFeesBySchoolAndAcademicYear(
            @PathVariable String schoolId,
            @PathVariable String academicYear) {
        return ResponseEntity.ok(feeService.getFeesBySchoolAndAcademicYear(schoolId, academicYear));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<FeeInvoice> updateFee(@PathVariable String id, @Valid @RequestBody UpdateFeeRequest request) {
        FeeInvoice feeDetails = FeeInvoice.builder()
                .academicYear(request.getAcademicYear())
                .type(request.getType())
                .amount(request.getAmount())
                .discount(request.getDiscount())
                .dueDate(request.getDueDate())
                .build();
        FeeInvoice updated = feeService.updateFee(id, feeDetails);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFee(@PathVariable String id) {
        feeService.deleteFee(id);
        return ResponseEntity.ok(java.util.Map.of("message", "Fee deleted successfully."));
    }
}
