package com.orbitastra.backend.services.finance;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.finance.StudentWallet;
import com.orbitastra.backend.models.finance.WalletTransaction;
import com.orbitastra.backend.models.finance.enums.TransactionType;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.repositories.finance.StudentWalletRepository;
import com.orbitastra.backend.repositories.finance.WalletTransactionRepository;
import com.orbitastra.backend.repositories.student.StudentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentWalletService {

    private final StudentWalletRepository studentWalletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final StudentRepository studentRepository;

    public StudentWallet getWalletByStudentId(String studentId) {
        return studentWalletRepository.findByStudentId(studentId)
                .orElseGet(() -> {
                    Student student = studentRepository.findById(studentId)
                            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
                    StudentWallet wallet = StudentWallet.builder()
                            .studentId(studentId)
                            .schoolId(student.getSchoolId())
                            .balance(BigDecimal.ZERO)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return studentWalletRepository.save(wallet);
                });
    }

    @Transactional
    public StudentWallet creditWallet(String studentId, BigDecimal amount, String remarks) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be greater than zero.");
        }
        StudentWallet wallet = getWalletByStudentId(studentId);
        BigDecimal oldBalance = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
        BigDecimal newBalance = oldBalance.add(amount);
        wallet.setBalance(newBalance);
        wallet.setUpdatedAt(LocalDateTime.now());
        StudentWallet savedWallet = studentWalletRepository.save(wallet);

        WalletTransaction transaction = WalletTransaction.builder()
                .schoolId(wallet.getSchoolId())
                .studentId(studentId)
                .type(TransactionType.CREDIT)
                .amount(amount)
                .balanceAfter(newBalance)
                .referenceNo("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .remarks(remarks)
                .transactionDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        walletTransactionRepository.save(transaction);

        return savedWallet;
    }

    @Transactional
    public StudentWallet debitWallet(String studentId, BigDecimal amount, String remarks) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be greater than zero.");
        }
        StudentWallet wallet = getWalletByStudentId(studentId);
        BigDecimal oldBalance = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
        if (oldBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient wallet balance.");
        }
        BigDecimal newBalance = oldBalance.subtract(amount);
        wallet.setBalance(newBalance);
        wallet.setUpdatedAt(LocalDateTime.now());
        StudentWallet savedWallet = studentWalletRepository.save(wallet);

        WalletTransaction transaction = WalletTransaction.builder()
                .schoolId(wallet.getSchoolId())
                .studentId(studentId)
                .type(TransactionType.DEBIT)
                .amount(amount)
                .balanceAfter(newBalance)
                .referenceNo("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .remarks(remarks)
                .transactionDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        walletTransactionRepository.save(transaction);

        return savedWallet;
    }
}
