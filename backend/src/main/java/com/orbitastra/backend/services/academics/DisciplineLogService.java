package com.orbitastra.backend.services.academics;

import java.util.List;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.academics.DisciplineLog;
import com.orbitastra.backend.repositories.academics.DisciplineLogRepository;
import com.orbitastra.backend.repositories.core.SchoolRepository;
import com.orbitastra.backend.services.utils.AcademicYearResolver;
import com.orbitastra.backend.services.utils.StudentValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DisciplineLogService {

    private final DisciplineLogRepository disciplineLogRepository;
    private final SchoolRepository schoolRepository;
    private final StudentValidator studentValidator;
    private final AcademicYearResolver academicYearResolver;

    public DisciplineLog createDisciplineLog(DisciplineLog log) {
        if (log.getSchoolId() == null || !schoolRepository.existsById(log.getSchoolId())) {
            throw new ResourceNotFoundException("School not found with id: " + log.getSchoolId());
        }

        studentValidator.validateStudent(log.getStudentId(), log.getSchoolId());

        java.time.LocalDate incidentDay = log.getIncidentDate() != null
                ? log.getIncidentDate().toLocalDate()
                : null;
        log.setAcademicYear(academicYearResolver
                .resolve(log.getSchoolId(), log.getAcademicYear(), incidentDay)
                .getName());

        return disciplineLogRepository.save(log);
    }

    public List<DisciplineLog> getAllDisciplineLogs() {
        return disciplineLogRepository.findAll();
    }

    public DisciplineLog getDisciplineLogById(String id) {
        return disciplineLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discipline log not found with id: " + id));
    }

    public List<DisciplineLog> getDisciplineLogsBySchool(String schoolId) {
        return disciplineLogRepository.findBySchoolId(schoolId);
    }

    public List<DisciplineLog> getDisciplineLogsBySchoolAndAcademicYear(String schoolId, String academicYear) {
        return disciplineLogRepository.findBySchoolIdAndAcademicYear(schoolId, academicYear);
    }

    public List<DisciplineLog> getDisciplineLogsByStudent(String studentId) {
        return disciplineLogRepository.findByStudentId(studentId);
    }

    public DisciplineLog updateDisciplineLog(String id, DisciplineLog logDetails) {
        DisciplineLog log = getDisciplineLogById(id);
        academicYearResolver.assertImmutable(log.getAcademicYear(), logDetails.getAcademicYear());

        if (logDetails.getSchoolId() != null && !logDetails.getSchoolId().equals(log.getSchoolId())) {
            if (!schoolRepository.existsById(logDetails.getSchoolId())) {
                throw new ResourceNotFoundException("School not found with id: " + logDetails.getSchoolId());
            }
            log.setSchoolId(logDetails.getSchoolId());
        }

        if (logDetails.getStudentId() != null && !logDetails.getStudentId().equals(log.getStudentId())) {
            studentValidator.validateStudent(logDetails.getStudentId(), log.getSchoolId());
            log.setStudentId(logDetails.getStudentId());
        }

        if (logDetails.getViolation() != null) {
            log.setViolation(logDetails.getViolation());
        }
        if (logDetails.getFineAmount() != null) {
            log.setFineAmount(logDetails.getFineAmount());
        }
        if (logDetails.getActionTaken() != null) {
            log.setActionTaken(logDetails.getActionTaken());
        }
        if (logDetails.getIncidentDate() != null) {
            log.setIncidentDate(logDetails.getIncidentDate());
        }

        return disciplineLogRepository.save(log);
    }

    public void deleteDisciplineLog(String id) {
        DisciplineLog log = getDisciplineLogById(id);
        disciplineLogRepository.delete(log);
    }
}
