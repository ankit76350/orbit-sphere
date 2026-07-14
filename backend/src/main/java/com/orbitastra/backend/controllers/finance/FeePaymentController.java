package com.orbitastra.backend.controllers.finance;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.dto.finance.PaymentRequest;
import com.orbitastra.backend.models.finance.FeeInvoice;
import com.orbitastra.backend.models.finance.FeePayment;
import com.orbitastra.backend.services.finance.FeeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/fees")
@RequiredArgsConstructor
public class FeePaymentController {

    private final FeeService feeService;

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
}
