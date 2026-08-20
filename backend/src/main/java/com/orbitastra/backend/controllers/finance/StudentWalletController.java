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

import com.orbitastra.backend.models.old.finance.StudentWallet;
import com.orbitastra.backend.models.old.finance.WalletTransaction;
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

    @GetMapping("/student/{studentDocsId}")
    public ResponseEntity<StudentWallet> getWalletByStudentDocsId(@PathVariable String studentDocsId) {
        StudentWallet wallet = studentWalletService.getWalletByStudentDocsId(studentDocsId);
        return ResponseEntity.ok(wallet);
    }

    @PostMapping("/student/{studentDocsId}")
    public ResponseEntity<StudentWallet> openWallet(@PathVariable String studentDocsId) {
        StudentWallet wallet = studentWalletService.openWallet(studentDocsId);
        return ResponseEntity.ok(wallet);
    }

    @GetMapping("/no/{*walletNo}")
    public ResponseEntity<StudentWallet> getWalletByWalletNo(@PathVariable String walletNo) {
        StudentWallet wallet = studentWalletService.getWalletByWalletNo(walletNo);
        return ResponseEntity.ok(wallet);
    }

    @PostMapping("/student/{studentDocsId}/credit")
    public ResponseEntity<StudentWallet> creditWallet(@PathVariable String studentDocsId, @Valid @RequestBody WalletOperationRequest request) {
        studentWalletService.creditWallet(studentDocsId, request.getAmount(), request.getRemarks());
        StudentWallet wallet = studentWalletService.getWalletByStudentDocsId(studentDocsId);
        return ResponseEntity.ok(wallet);
    }

    @PostMapping("/student/{studentDocsId}/debit")
    public ResponseEntity<StudentWallet> debitWallet(@PathVariable String studentDocsId, @Valid @RequestBody WalletOperationRequest request) {
        studentWalletService.debitWallet(studentDocsId, request.getAmount(), request.getRemarks());
        StudentWallet wallet = studentWalletService.getWalletByStudentDocsId(studentDocsId);
        return ResponseEntity.ok(wallet);
    }

    @GetMapping("/student/{studentDocsId}/transactions")
    public ResponseEntity<List<WalletTransaction>> getTransactionsByStudent(@PathVariable String studentDocsId) {
        List<WalletTransaction> transactions = walletTransactionService.getTransactionsByStudent(studentDocsId);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/transactions/no/{*referenceNo}")
    public ResponseEntity<WalletTransaction> getTransactionByReferenceNo(@PathVariable String referenceNo) {
        WalletTransaction transaction = studentWalletService.getWalletTransactionByReferenceNo(referenceNo);
        return ResponseEntity.ok(transaction);
    }

    @Data
    public static class WalletOperationRequest {
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "amount must be greater than zero")
        private BigDecimal amount;

        private String remarks;
    }
}
