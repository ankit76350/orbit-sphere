package com.orbitastra.backend.controllers.finance;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.models.finance.StudentWallet;
import com.orbitastra.backend.models.finance.WalletTransaction;
import com.orbitastra.backend.services.finance.StudentWalletService;
import com.orbitastra.backend.services.finance.WalletTransactionService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class StudentWalletController {

    private final StudentWalletService studentWalletService;
    private final WalletTransactionService walletTransactionService;

    @GetMapping("/student/{studentId}")
    public ResponseEntity<StudentWallet> getWalletByStudentId(@PathVariable String studentId) {
        StudentWallet wallet = studentWalletService.getWalletByStudentId(studentId);
        return ResponseEntity.ok(wallet);
    }

    @PostMapping("/student/{studentId}/credit")
    public ResponseEntity<StudentWallet> creditWallet(@PathVariable String studentId, @Valid @RequestBody WalletOperationRequest request) {
        StudentWallet wallet = studentWalletService.creditWallet(studentId, request.getAmount(), request.getRemarks());
        return ResponseEntity.ok(wallet);
    }

    @PostMapping("/student/{studentId}/debit")
    public ResponseEntity<StudentWallet> debitWallet(@PathVariable String studentId, @Valid @RequestBody WalletOperationRequest request) {
        StudentWallet wallet = studentWalletService.debitWallet(studentId, request.getAmount(), request.getRemarks());
        return ResponseEntity.ok(wallet);
    }

    @GetMapping("/student/{studentId}/transactions")
    public ResponseEntity<List<WalletTransaction>> getTransactionsByStudent(@PathVariable String studentId) {
        List<WalletTransaction> transactions = walletTransactionService.getTransactionsByStudent(studentId);
        return ResponseEntity.ok(transactions);
    }

    @Data
    public static class WalletOperationRequest {
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "amount must be greater than zero")
        private BigDecimal amount;

        private String remarks;
    }
}
