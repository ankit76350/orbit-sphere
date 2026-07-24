package com.orbitastra.backend.services.academics;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.academics.SchoolClass;
import com.orbitastra.backend.repositories.academics.SchoolClassRepository;
import com.orbitastra.backend.repositories.core.SchoolRepository;
import com.orbitastra.backend.repositories.staff.StaffRepository;
import com.orbitastra.backend.services.utils.AcademicYearResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SchoolClassService {

    private final SchoolClassRepository schoolClassRepository;
    private final SchoolRepository schoolRepository;
    private final StaffRepository staffRepository;
    private final AcademicYearResolver academicYearResolver;

    private String normalizeOptionalReference(String value, String fieldName) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank when provided.");
        }
        return normalized;
    }

    private String validateTeacher(String teacherDocsId, String schoolId) {
        String normalizedTeacherDocsId = normalizeOptionalReference(teacherDocsId, "teacherDocsId");
        if (normalizedTeacherDocsId == null) {
            return null;
        }
        if (schoolId == null || schoolId.trim().isEmpty()) {
            throw new IllegalArgumentException("Class schoolId is required before assigning a subject teacher.");
        }

        com.orbitastra.backend.models.staff.Staff teacher = staffRepository.findById(normalizedTeacherDocsId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + normalizedTeacherDocsId));
        if (!Objects.equals(teacher.getSchoolId(), schoolId)) {
            throw new IllegalArgumentException("Teacher with ID " + normalizedTeacherDocsId
                    + " does not belong to this school.");
        }
        return normalizedTeacherDocsId;
    }

    private String normalizeSubjectName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Subject name is required.");
        }
        return name.trim();
    }

    private SchoolClass.ClassSubject validateSubject(SchoolClass.ClassSubject subject, String schoolId) {
        if (subject == null) {
            throw new IllegalArgumentException("Subject cannot be null.");
        }
        subject.setName(normalizeSubjectName(subject.getName()));
        subject.setTeacherDocsId(validateTeacher(subject.getTeacherDocsId(), schoolId));
        return subject;
    }

    private void validateNoDuplicateSubjects(List<SchoolClass.ClassSubject> subjects) {
        if (subjects == null) {
            return;
        }
        List<String> names = new ArrayList<>();
        for (SchoolClass.ClassSubject subject : subjects) {
            String normalizedName = normalizeSubjectName(subject == null ? null : subject.getName());
            if (names.stream().anyMatch(existing -> existing.equalsIgnoreCase(normalizedName))) {
                throw new IllegalArgumentException("Subject '" + normalizedName
                        + "' appears more than once in this class.");
            }
            names.add(normalizedName);
        }
    }

    public SchoolClass createClass(SchoolClass schoolClass) {
        if (schoolClass.getSchoolId() == null || !schoolRepository.existsById(schoolClass.getSchoolId())) {
            throw new ResourceNotFoundException("School not found with id: " + schoolClass.getSchoolId());
        }

        schoolClass.setClassTeacherDocsId(
                validateTeacher(schoolClass.getClassTeacherDocsId(), schoolClass.getSchoolId()));

        if (schoolClass.getSubjects() != null) {
            validateNoDuplicateSubjects(schoolClass.getSubjects());
            for (SchoolClass.ClassSubject sub : schoolClass.getSubjects()) {
                validateSubject(sub, schoolClass.getSchoolId());
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

        if (classDetails.getClassTeacherDocsId() != null) {
            schoolClass.setClassTeacherDocsId(
                    validateTeacher(classDetails.getClassTeacherDocsId(), schoolClass.getSchoolId()));
        }

        if (classDetails.getSubjects() != null) {
            validateNoDuplicateSubjects(classDetails.getSubjects());
            for (SchoolClass.ClassSubject sub : classDetails.getSubjects()) {
                validateSubject(sub, schoolClass.getSchoolId());
            }
            schoolClass.setSubjects(new ArrayList<>(classDetails.getSubjects()));
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

    public SchoolClass addSubject(String classDocsId, SchoolClass.ClassSubject subject) {
        if (classDocsId == null || classDocsId.trim().isEmpty()) {
            throw new IllegalArgumentException("classDocsId is required.");
        }
        if (subject == null) {
            throw new IllegalArgumentException("Subject request is required.");
        }

        String normalizedClassDocsId = classDocsId.trim();
        SchoolClass schoolClass = getClassById(normalizedClassDocsId);
        if (schoolClass.getSchoolId() == null || schoolClass.getSchoolId().trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot add a subject to a class without a schoolId.");
        }
        if (!schoolRepository.existsById(schoolClass.getSchoolId())) {
            throw new ResourceNotFoundException("School not found with id: " + schoolClass.getSchoolId());
        }

        subject.setName(normalizeSubjectName(subject.getName()));
        List<SchoolClass.ClassSubject> subjects = schoolClass.getSubjects() == null
                ? new ArrayList<>()
                : new ArrayList<>(schoolClass.getSubjects());

        boolean exists = subjects.stream().filter(Objects::nonNull)
                .anyMatch(s -> s.getName() != null
                        && s.getName().trim().equalsIgnoreCase(subject.getName()));
        if (exists) {
            throw new IllegalArgumentException("Subject '" + subject.getName() + "' already exists in this class.");
        }

        subject.setTeacherDocsId(validateTeacher(subject.getTeacherDocsId(), schoolClass.getSchoolId()));
        subjects.add(subject);
        schoolClass.setSubjects(subjects);
        return schoolClassRepository.save(schoolClass);
    }

    public SchoolClass addSections(String classDocsId, List<String> sections) {
        SchoolClass schoolClass = getClassById(classDocsId);
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

    public SchoolClass removeSection(String classDocsId, String section) {
        SchoolClass schoolClass = getClassById(classDocsId);
        if (schoolClass.getSections() != null) {
            schoolClass.getSections().remove(section);
        }
        return schoolClassRepository.save(schoolClass);
    }

    public SchoolClass removeSubject(String classDocsId, String subjectName) {
        SchoolClass schoolClass = getClassById(classDocsId);
        if (schoolClass.getSubjects() != null) {
            schoolClass.getSubjects().removeIf(s -> s.getName() != null && s.getName().equalsIgnoreCase(subjectName));
        }
        return schoolClassRepository.save(schoolClass);
    }
}
