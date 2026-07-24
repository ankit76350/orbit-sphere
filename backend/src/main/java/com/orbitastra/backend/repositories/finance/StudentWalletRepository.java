package com.orbitastra.backend.repositories.finance;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.finance.StudentWallet;

@Repository
public interface StudentWalletRepository extends MongoRepository<StudentWallet, String> {
    Optional<StudentWallet> findByStudentDocsId(String studentDocsId);
    Optional<StudentWallet> findBySchoolIdAndStudentDocsId(String schoolId, String studentDocsId);
}
