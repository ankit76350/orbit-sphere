package com.orbitastra.backend.services.student;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.Parent;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.repositories.student.StudentRepository;
import com.orbitastra.backend.repositories.student.ParentRepository;
import com.orbitastra.backend.repositories.core.SchoolRepository;
import com.orbitastra.backend.repositories.student.StudentAcademicRecordRepository;

@ExtendWith(MockitoExtension.class)
public class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ParentRepository parentRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private StudentAcademicRecordRepository studentAcademicRecordRepository;

    @InjectMocks
    private StudentService studentService;

    private Student student;
    private Parent parent;
    private School school;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId("student-id-123");
        student.setSchoolId("school-id-123");
        student.setParentId("parent-id-123");
        student.setAdmissionNo("ADM-001");
        student.setFirstName("John");
        student.setLastName("Doe");
        student.setDob(LocalDate.of(2012, 5, 10));

        parent = new Parent();
        parent.setId("parent-id-123");
        parent.setSchoolId("school-id-123");
        parent.setStudentIds(new ArrayList<>());

        school = new School();
        school.setId("school-id-123");
        school.setMaxStudents(100);
    }

    @Test
    void createStudent_Success() {
        when(schoolRepository.findById("school-id-123")).thenReturn(Optional.of(school));
        when(studentRepository.countBySchoolId("school-id-123")).thenReturn(10L);
        when(parentRepository.existsById("parent-id-123")).thenReturn(true);
        when(studentRepository.findByAdmissionNo("ADM-001")).thenReturn(Optional.empty());
        when(studentRepository.save(student)).thenReturn(student);
        when(parentRepository.findById("parent-id-123")).thenReturn(Optional.of(parent));
        when(studentAcademicRecordRepository.save(any(StudentAcademicRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Student created = studentService.createStudent(student);

        assertNotNull(created);
        assertEquals("ADM-001", created.getAdmissionNo());
        verify(schoolRepository, times(1)).findById("school-id-123");
        verify(parentRepository, times(1)).existsById("parent-id-123");
        verify(studentRepository, times(1)).findByAdmissionNo("ADM-001");
        verify(studentRepository, times(1)).save(student);
        verify(parentRepository, times(1)).findById("parent-id-123");
        verify(parentRepository, times(1)).save(parent);
        verify(studentAcademicRecordRepository, times(1)).save(any(StudentAcademicRecord.class));
        assertTrue(parent.getStudentIds().contains("student-id-123"));
    }

    @Test
    void createStudent_SchoolNotFound_ThrowsException() {
        when(schoolRepository.findById("school-id-123")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            studentService.createStudent(student);
        });

        verify(schoolRepository, times(1)).findById("school-id-123");
        verify(studentRepository, never()).save(any());
    }

    @Test
    void createStudent_ParentNotFound_ThrowsException() {
        when(schoolRepository.findById("school-id-123")).thenReturn(Optional.of(school));
        when(studentRepository.countBySchoolId("school-id-123")).thenReturn(10L);
        when(parentRepository.existsById("parent-id-123")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> {
            studentService.createStudent(student);
        });

        verify(schoolRepository, times(1)).findById("school-id-123");
        verify(parentRepository, times(1)).existsById("parent-id-123");
        verify(studentRepository, never()).save(any());
    }

    @Test
    void createStudent_AdmissionNoDuplicate_ThrowsException() {
        when(schoolRepository.findById("school-id-123")).thenReturn(Optional.of(school));
        when(studentRepository.countBySchoolId("school-id-123")).thenReturn(10L);
        when(parentRepository.existsById("parent-id-123")).thenReturn(true);
        when(studentRepository.findByAdmissionNo("ADM-001")).thenReturn(Optional.of(new Student()));

        assertThrows(IllegalArgumentException.class, () -> {
            studentService.createStudent(student);
        });

        verify(studentRepository, times(1)).findByAdmissionNo("ADM-001");
        verify(studentRepository, never()).save(any());
    }

    @Test
    void createStudent_LimitExceeded_ThrowsException() {
        school.setMaxStudents(5);
        when(schoolRepository.findById("school-id-123")).thenReturn(Optional.of(school));
        when(studentRepository.countBySchoolId("school-id-123")).thenReturn(5L);

        assertThrows(IllegalArgumentException.class, () -> {
            studentService.createStudent(student);
        });

        verify(studentRepository, never()).save(any());
    }

    @Test
    void getStudentById_Success() {
        StudentAcademicRecord record = StudentAcademicRecord.builder()
                .studentDocId("student-id-123")
                .academicYearId("2026-2027")
                .studentId("STU-001")
                .rollNo("12")
                .build();

        when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
        when(studentAcademicRecordRepository.findByStudentDocId("student-id-123")).thenReturn(List.of(record));

        Student found = studentService.getStudentById("student-id-123");

        assertNotNull(found);
        assertEquals("student-id-123", found.getId());
        assertEquals("STU-001", found.getStudentId());
        assertEquals("12", found.getRollNo());
        assertEquals("2026-2027", found.getAcademicYearId());
    }

    @Test
    void getStudentById_NotFound_ThrowsException() {
        when(studentRepository.findById("student-id-123")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            studentService.getStudentById("student-id-123");
        });
    }

    @Test
    void updateStudent_Success() {
        Student details = new Student();
        details.setFirstName("Jane");
        details.setLastName("Doe");

        when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
        when(studentAcademicRecordRepository.findByStudentDocId("student-id-123")).thenReturn(new ArrayList<>());
        when(studentAcademicRecordRepository.findByStudentDocIdAndAcademicYearId(anyString(), anyString())).thenReturn(Optional.empty());
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Student updated = studentService.updateStudent("student-id-123", details);

        assertNotNull(updated);
        assertEquals("Jane", updated.getFirstName());
        assertEquals("Doe", updated.getLastName());
        assertEquals("ADM-001", updated.getAdmissionNo()); // unchanged
    }

    @Test
    void updateStudent_ParentChanged_Success() {
        Student details = new Student();
        details.setParentId("parent-new-id");

        Parent oldParent = new Parent();
        oldParent.setId("parent-id-123");
        oldParent.setStudentIds(new ArrayList<>(List.of("student-id-123")));

        Parent newParent = new Parent();
        newParent.setId("parent-new-id");
        newParent.setStudentIds(new ArrayList<>());

        when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
        when(studentAcademicRecordRepository.findByStudentDocId("student-id-123")).thenReturn(new ArrayList<>());
        when(studentAcademicRecordRepository.findByStudentDocIdAndAcademicYearId(anyString(), anyString())).thenReturn(Optional.empty());
        when(parentRepository.existsById("parent-new-id")).thenReturn(true);
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(parentRepository.findById("parent-id-123")).thenReturn(Optional.of(oldParent));
        when(parentRepository.findById("parent-new-id")).thenReturn(Optional.of(newParent));

        Student updated = studentService.updateStudent("student-id-123", details);

        assertNotNull(updated);
        assertEquals("parent-new-id", updated.getParentId());
        assertFalse(oldParent.getStudentIds().contains("student-id-123"));
        assertTrue(newParent.getStudentIds().contains("student-id-123"));
        verify(parentRepository, times(1)).save(oldParent);
        verify(parentRepository, times(1)).save(newParent);
    }

    @Test
    void deleteStudent_Success() {
        Parent linkedParent = new Parent();
        linkedParent.setId("parent-id-123");
        linkedParent.setStudentIds(new ArrayList<>(List.of("student-id-123")));

        when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
        when(studentAcademicRecordRepository.findByStudentDocId("student-id-123")).thenReturn(new ArrayList<>());
        when(parentRepository.findById("parent-id-123")).thenReturn(Optional.of(linkedParent));

        studentService.deleteStudent("student-id-123");

        verify(studentRepository, times(1)).delete(student);
        verify(parentRepository, times(1)).save(linkedParent);
        verify(studentAcademicRecordRepository, times(1)).deleteAll(anyList());
        assertFalse(linkedParent.getStudentIds().contains("student-id-123"));
    }

    @Test
    void createOrUpdateAcademicRecord_Success() {
        StudentAcademicRecord input = StudentAcademicRecord.builder()
                .academicYearId("2026-2027")
                .classId("class-new")
                .build();

        when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
        when(studentAcademicRecordRepository.findByStudentDocId("student-id-123")).thenReturn(new ArrayList<>());
        when(studentAcademicRecordRepository.findByStudentDocIdAndAcademicYearId("student-id-123", "2026-2027"))
                .thenReturn(Optional.empty());
        when(studentAcademicRecordRepository.save(any(StudentAcademicRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StudentAcademicRecord record = studentService.createOrUpdateAcademicRecord("student-id-123", input);

        assertNotNull(record);
        assertEquals("class-new", record.getClassId());
        assertEquals("2026-2027", record.getAcademicYearId());
        verify(studentAcademicRecordRepository, times(1)).save(any(StudentAcademicRecord.class));
    }

    @Test
    void getAcademicHistory_Success() {
        StudentAcademicRecord r1 = StudentAcademicRecord.builder().academicYearId("2025-2026").build();
        StudentAcademicRecord r2 = StudentAcademicRecord.builder().academicYearId("2026-2027").build();

        when(studentRepository.existsById("student-id-123")).thenReturn(true);
        when(studentAcademicRecordRepository.findByStudentDocId("student-id-123")).thenReturn(List.of(r1, r2));

        List<StudentAcademicRecord> history = studentService.getAcademicHistory("student-id-123");

        assertEquals(2, history.size());
        verify(studentAcademicRecordRepository, times(1)).findByStudentDocId("student-id-123");
    }

    @Test
    void promoteStudent_Success() {
        StudentAcademicRecord input = StudentAcademicRecord.builder()
                .academicYearId("2027-2028")
                .classId("class-new")
                .build();

        when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
        when(studentAcademicRecordRepository.findByStudentDocId("student-id-123")).thenReturn(new ArrayList<>());
        when(studentAcademicRecordRepository.findByStudentDocIdAndAcademicYearId("student-id-123", "2027-2028"))
                .thenReturn(Optional.empty());
        when(studentAcademicRecordRepository.save(any(StudentAcademicRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StudentAcademicRecord record = studentService.promoteStudent("student-id-123", input);

        assertNotNull(record);
        assertEquals("class-new", record.getClassId());
        assertEquals("2027-2028", record.getAcademicYearId());
        verify(studentAcademicRecordRepository, times(1)).save(any(StudentAcademicRecord.class));
    }

    @Test
    void getSiblings_Success() {
        Student sibling = new Student();
        sibling.setId("sibling-id-999");
        sibling.setSchoolId("school-id-123");
        sibling.setParentId("parent-id-123");
        sibling.setAdmissionNo("ADM-999");

        when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
        when(studentAcademicRecordRepository.findByStudentDocId("student-id-123")).thenReturn(new ArrayList<>());
        when(studentRepository.findByParentId("parent-id-123")).thenReturn(List.of(student, sibling));

        List<Student> siblings = studentService.getSiblings("student-id-123");

        assertEquals(1, siblings.size());
        assertEquals("sibling-id-999", siblings.get(0).getId());
        verify(studentRepository, times(1)).findByParentId("parent-id-123");
    }
}
