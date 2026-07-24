package com.orbitastra.backend.services.finance;

import java.util.List;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.models.finance.WalletTransaction;
import com.orbitastra.backend.repositories.finance.WalletTransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WalletTransactionService {

    private final WalletTransactionRepository walletTransactionRepository;

    public List<WalletTransaction> getTransactionsByStudent(String studentDocsId) {
        return walletTransactionRepository.findByStudentDocsIdOrderByTransactionDateDesc(studentDocsId);
    }

    public List<WalletTransaction> getTransactionsBySchoolAndStudent(String schoolId, String studentDocsId) {
        return walletTransactionRepository.findBySchoolIdAndStudentDocsId(schoolId, studentDocsId);
    }

    public WalletTransaction saveTransaction(WalletTransaction transaction) {
        return walletTransactionRepository.save(transaction);
    }
}
