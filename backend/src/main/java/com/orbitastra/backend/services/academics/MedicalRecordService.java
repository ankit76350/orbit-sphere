package com.orbitastra.backend.services.academics;

import java.util.List;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.academics.MedicalRecord;
import com.orbitastra.backend.repositories.academics.MedicalRecordRepository;
import com.orbitastra.backend.repositories.core.SchoolRepository;
import com.orbitastra.backend.services.utils.StudentValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final SchoolRepository schoolRepository;
    private final StudentValidator studentValidator;

    public MedicalRecord createMedicalRecord(MedicalRecord record) {
        if (record.getSchoolId() == null || !schoolRepository.existsById(record.getSchoolId())) {
            throw new ResourceNotFoundException("School not found with id: " + record.getSchoolId());
        }

        studentValidator.validateStudent(record.getStudentId(), record.getSchoolId());

        return medicalRecordRepository.save(record);
    }

    public List<MedicalRecord> getAllMedicalRecords() {
        return medicalRecordRepository.findAll();
    }

    public MedicalRecord getMedicalRecordById(String id) {
        return medicalRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medical record not found with id: " + id));
    }

    public List<MedicalRecord> getMedicalRecordsBySchool(String schoolId) {
        return medicalRecordRepository.findBySchoolId(schoolId);
    }

    public List<MedicalRecord> getMedicalRecordsByStudent(String studentId) {
        return medicalRecordRepository.findByStudentId(studentId);
    }

    public MedicalRecord updateMedicalRecord(String id, MedicalRecord recordDetails) {
        MedicalRecord record = getMedicalRecordById(id);

        if (recordDetails.getSchoolId() != null && !recordDetails.getSchoolId().equals(record.getSchoolId())) {
            if (!schoolRepository.existsById(recordDetails.getSchoolId())) {
                throw new ResourceNotFoundException("School not found with id: " + recordDetails.getSchoolId());
            }
            record.setSchoolId(recordDetails.getSchoolId());
        }

        if (recordDetails.getStudentId() != null && !recordDetails.getStudentId().equals(record.getStudentId())) {
            studentValidator.validateStudent(recordDetails.getStudentId(), record.getSchoolId());
            record.setStudentId(recordDetails.getStudentId());
        }

        if (recordDetails.getVisitDate() != null) {
            record.setVisitDate(recordDetails.getVisitDate());
        }
        if (recordDetails.getDiagnosis() != null) {
            record.setDiagnosis(recordDetails.getDiagnosis());
        }
        if (recordDetails.getMedicines() != null) {
            record.setMedicines(recordDetails.getMedicines());
        }
        if (recordDetails.getDoctorName() != null) {
            record.setDoctorName(recordDetails.getDoctorName());
        }

        return medicalRecordRepository.save(record);
    }

    public void deleteMedicalRecord(String id) {
        MedicalRecord record = getMedicalRecordById(id);
        medicalRecordRepository.delete(record);
    }
}
