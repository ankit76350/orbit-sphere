package com.orbitastra.backend.services.academics;

import java.util.List;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.academics.SchoolClass;
import com.orbitastra.backend.repositories.academics.SchoolClassRepository;
import com.orbitastra.backend.repositories.core.SchoolRepository;
import com.orbitastra.backend.repositories.staff.StaffRepository;
import com.orbitastra.backend.services.core.AcademicYearResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SchoolClassService {

    private final SchoolClassRepository schoolClassRepository;
    private final SchoolRepository schoolRepository;
    private final StaffRepository staffRepository;
    private final AcademicYearResolver academicYearResolver;

    private void validateTeacher(String teacherId, String schoolId) {
        if (teacherId == null || teacherId.isEmpty()) {
            return;
        }
        com.orbitastra.backend.models.staff.Staff teacher = staffRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + teacherId));
        if (!teacher.getSchoolId().equals(schoolId)) {
            throw new IllegalArgumentException("Teacher with ID " + teacherId + " does not belong to this school.");
        }
    }

    public SchoolClass createClass(SchoolClass schoolClass) {
        if (schoolClass.getSchoolId() == null || !schoolRepository.existsById(schoolClass.getSchoolId())) {
            throw new ResourceNotFoundException("School not found with id: " + schoolClass.getSchoolId());
        }

        validateTeacher(schoolClass.getClassTeacher(), schoolClass.getSchoolId());

        if (schoolClass.getSubjects() != null) {
            for (SchoolClass.ClassSubject sub : schoolClass.getSubjects()) {
                validateTeacher(sub.getTeacher(), schoolClass.getSchoolId());
            }
        }

        // A class belongs to one academic year of the school (SaaS: school -> year -> class).
        schoolClass.setAcademicYear(academicYearResolver
                .resolve(schoolClass.getSchoolId(), schoolClass.getAcademicYear(), null)
                .getName());

        // Class name is unique per (school, academic year) — the same "Class 5"
        // may exist in different years.
        if (schoolClass.getName() != null && schoolClassRepository
                .findByNameAndSchoolIdAndAcademicYear(schoolClass.getName(), schoolClass.getSchoolId(),
                        schoolClass.getAcademicYear())
                .isPresent()) {
            throw new IllegalArgumentException("Class '" + schoolClass.getName()
                    + "' already exists in this school for academic year " + schoolClass.getAcademicYear() + ".");
        }

        return schoolClassRepository.save(schoolClass);
    }

    public List<SchoolClass> getAllClasses() {
        return schoolClassRepository.findAll();
    }

    public SchoolClass getClassById(String id) {
        return schoolClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + id));
    }

    public List<SchoolClass> getClassesBySchool(String schoolId) {
        return schoolClassRepository.findBySchoolId(schoolId);
    }

    public List<SchoolClass> getClassesBySchoolAndAcademicYear(String schoolId, String academicYear) {
        return schoolClassRepository.findBySchoolIdAndAcademicYear(schoolId, academicYear);
    }

    public SchoolClass updateClass(String id, SchoolClass classDetails) {
        SchoolClass schoolClass = getClassById(id);
        // A class cannot be moved between academic years — timetables, homework
        // and student records reference it within its year.
        academicYearResolver.assertImmutable(schoolClass.getAcademicYear(), classDetails.getAcademicYear());

        if (classDetails.getSchoolId() != null && !classDetails.getSchoolId().equals(schoolClass.getSchoolId())) {
            if (!schoolRepository.existsById(classDetails.getSchoolId())) {
                throw new ResourceNotFoundException("School not found with id: " + classDetails.getSchoolId());
            }
            schoolClass.setSchoolId(classDetails.getSchoolId());
        }

        if (classDetails.getName() != null && !classDetails.getName().equals(schoolClass.getName())) {
            if (schoolClassRepository.findByNameAndSchoolIdAndAcademicYear(
                    classDetails.getName(), schoolClass.getSchoolId(), schoolClass.getAcademicYear()).isPresent()) {
                throw new IllegalArgumentException("Class '" + classDetails.getName()
                        + "' already exists in this school for academic year " + schoolClass.getAcademicYear() + ".");
            }
            schoolClass.setName(classDetails.getName());
        }

        if (classDetails.getClassTeacher() != null) {
            validateTeacher(classDetails.getClassTeacher(), schoolClass.getSchoolId());
            schoolClass.setClassTeacher(classDetails.getClassTeacher());
        }

        if (classDetails.getSubjects() != null) {
            for (SchoolClass.ClassSubject sub : classDetails.getSubjects()) {
                validateTeacher(sub.getTeacher(), schoolClass.getSchoolId());
            }
            schoolClass.setSubjects(classDetails.getSubjects());
        }

        if (classDetails.getSections() != null) {
            schoolClass.setSections(classDetails.getSections());
        }

        return schoolClassRepository.save(schoolClass);
    }

    public void deleteClass(String id) {
        SchoolClass schoolClass = getClassById(id);
        schoolClassRepository.delete(schoolClass);
    }

    public SchoolClass addSubject(String classId, SchoolClass.ClassSubject subject) {
        SchoolClass schoolClass = getClassById(classId);

        validateTeacher(subject.getTeacher(), schoolClass.getSchoolId());

        if (schoolClass.getSubjects() == null) {
            schoolClass.setSubjects(new java.util.ArrayList<>());
        }

        boolean exists = schoolClass.getSubjects().stream()
                .anyMatch(s -> s.getName() != null && s.getName().equalsIgnoreCase(subject.getName()));
        if (exists) {
            throw new IllegalArgumentException("Subject '" + subject.getName() + "' already exists in this class.");
        }

        schoolClass.getSubjects().add(subject);
        return schoolClassRepository.save(schoolClass);
    }

    public SchoolClass addSections(String classId, List<String> sections) {
        SchoolClass schoolClass = getClassById(classId);
        if (schoolClass.getSections() == null) {
            schoolClass.setSections(new java.util.ArrayList<>());
        }
        for (String section : sections) {
            if (section == null || section.trim().isEmpty()) {
                continue;
            }
            if (schoolClass.getSections().contains(section)) {
                throw new IllegalArgumentException("Section '" + section + "' already exists in this class.");
            }
            schoolClass.getSections().add(section);
        }
        return schoolClassRepository.save(schoolClass);
    }

    public SchoolClass removeSection(String classId, String section) {
        SchoolClass schoolClass = getClassById(classId);
        if (schoolClass.getSections() != null) {
            schoolClass.getSections().remove(section);
        }
        return schoolClassRepository.save(schoolClass);
    }

    public SchoolClass removeSubject(String classId, String subjectName) {
        SchoolClass schoolClass = getClassById(classId);
        if (schoolClass.getSubjects() != null) {
            schoolClass.getSubjects().removeIf(s -> s.getName() != null && s.getName().equalsIgnoreCase(subjectName));
        }
        return schoolClassRepository.save(schoolClass);
    }
}
