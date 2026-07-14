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

import com.orbitastra.backend.models.finance.FeeInvoice;
import com.orbitastra.backend.models.finance.FeePayment;
import com.orbitastra.backend.models.finance.enums.PaymentMode;
import com.orbitastra.backend.services.finance.FeeService;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/fees")
@RequiredArgsConstructor
public class FeeController {

    private final FeeService feeService;

    @PostMapping
    public ResponseEntity<FeeInvoice> createFee(@RequestBody FeeInvoice fee) {
        FeeInvoice created = feeService.createFee(fee);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FeeInvoice> getFeeById(@PathVariable String id) {
        FeeInvoice fee = feeService.getFeeById(id);
        return ResponseEntity.ok(fee);
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<FeeInvoice>> getFeesByStudent(@PathVariable String studentId) {
        List<FeeInvoice> fees = feeService.getFeesByStudent(studentId);
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
    public ResponseEntity<FeeInvoice> updateFee(@PathVariable String id, @RequestBody FeeInvoice feeDetails) {
        FeeInvoice updated = feeService.updateFee(id, feeDetails);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFee(@PathVariable String id) {
        feeService.deleteFee(id);
        return ResponseEntity.ok(java.util.Map.of("message", "Fee deleted successfully."));
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<FeeInvoice> recordPayment(@PathVariable String id, @RequestBody PaymentRequest request) {
        FeeInvoice updated = feeService.recordPayment(
                id, request.getAmount(), request.getPaymentMode(), request.getRemarks(), request.getCollectedBy());
        return new ResponseEntity<>(updated, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/payments")
    public ResponseEntity<List<FeePayment>> getPaymentsByFee(@PathVariable String id) {
        return ResponseEntity.ok(feeService.getPaymentsByFee(id));
    }

    @GetMapping("/payments/student/{studentId}")
    public ResponseEntity<List<FeePayment>> getPaymentsByStudent(@PathVariable String studentId) {
        return ResponseEntity.ok(feeService.getPaymentsByStudent(studentId));
    }

    @Data
    public static class PaymentRequest {
        private BigDecimal amount;
        private PaymentMode paymentMode;
        private String remarks;
        private String collectedBy;
    }
}
