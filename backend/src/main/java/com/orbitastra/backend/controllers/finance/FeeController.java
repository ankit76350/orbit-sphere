package com.orbitastra.backend.controllers.finance;

import java.math.BigDecimal;
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

import com.orbitastra.backend.models.finance.Fee;
import com.orbitastra.backend.services.finance.FeeService;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/fees")
@RequiredArgsConstructor
public class FeeController {

    private final FeeService feeService;

    @PostMapping
    public ResponseEntity<Fee> createFee(@RequestBody Fee fee) {
        Fee created = feeService.createFee(fee);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Fee> getFeeById(@PathVariable String id) {
        Fee fee = feeService.getFeeById(id);
        return ResponseEntity.ok(fee);
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Fee>> getFeesByStudent(@PathVariable String studentId) {
        List<Fee> fees = feeService.getFeesByStudent(studentId);
        return ResponseEntity.ok(fees);
    }

    @GetMapping("/school/{schoolId}")
    public ResponseEntity<List<Fee>> getFeesBySchool(@PathVariable String schoolId) {
        List<Fee> fees = feeService.getFeesBySchool(schoolId);
        return ResponseEntity.ok(fees);
    }

    @GetMapping("/school/{schoolId}/academic-year/{academicYear}")
    public ResponseEntity<List<Fee>> getFeesBySchoolAndAcademicYear(
            @PathVariable String schoolId,
            @PathVariable String academicYear) {
        return ResponseEntity.ok(feeService.getFeesBySchoolAndAcademicYear(schoolId, academicYear));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Fee> updateFee(@PathVariable String id, @RequestBody Fee feeDetails) {
        Fee updated = feeService.updateFee(id, feeDetails);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<Fee> payFee(@PathVariable String id, @RequestBody PaymentRequest request) {
        Fee paid = feeService.payFee(id, request.getAmount());
        return ResponseEntity.ok(paid);
    }

    @PostMapping("/{id}/pay-wallet")
    public ResponseEntity<Fee> payFeeViaWallet(@PathVariable String id, @RequestBody PaymentRequest request) {
        Fee paid = feeService.payFeeViaWallet(id, request.getAmount());
        return ResponseEntity.ok(paid);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFee(@PathVariable String id) {
        feeService.deleteFee(id);
        return ResponseEntity.ok(java.util.Map.of("message", "Fee deleted successfully."));
    }

    @Data
    public static class PaymentRequest {
        private BigDecimal amount;
    }
}
