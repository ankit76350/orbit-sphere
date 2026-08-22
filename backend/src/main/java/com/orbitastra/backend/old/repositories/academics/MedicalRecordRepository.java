package com.orbitastra.backend.old.repositories.academics;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.old.academics.MedicalRecord;

@Repository
public interface MedicalRecordRepository extends MongoRepository<MedicalRecord, String> {
    List<MedicalRecord> findBySchoolId(String schoolId);
    List<MedicalRecord> findByStudentDocsId(String studentDocsId);
}
