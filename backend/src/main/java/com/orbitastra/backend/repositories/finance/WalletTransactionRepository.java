package com.orbitastra.backend.repositories.finance;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.finance.WalletTransaction;

@Repository
public interface WalletTransactionRepository extends MongoRepository<WalletTransaction, String> {
    List<WalletTransaction> findByStudentId(String studentId);
    List<WalletTransaction> findBySchoolIdAndStudentId(String schoolId, String studentId);
    List<WalletTransaction> findByStudentIdOrderByTransactionDateDesc(String studentId);
}
