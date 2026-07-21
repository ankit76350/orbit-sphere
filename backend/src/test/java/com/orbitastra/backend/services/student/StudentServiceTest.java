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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.models.student.GuardianLink;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.models.student.StudentAcademicRecord;
import com.orbitastra.backend.repositories.student.GuardianRepository;
import com.orbitastra.backend.repositories.student.StudentRepository;
import com.orbitastra.backend.repositories.core.SchoolRepository;
import com.orbitastra.backend.repositories.student.StudentAcademicRecordRepository;
import com.orbitastra.backend.models.academics.SchoolClass;
import com.orbitastra.backend.repositories.academics.SchoolClassRepository;
import com.orbitastra.backend.dto.student.CreateStudentRequest;
import com.orbitastra.backend.dto.student.StudentGuardianRequest;
import com.orbitastra.backend.services.utils.AcademicYearResolver;
import com.orbitastra.backend.models.student.enums.GuardianRelation;

@ExtendWith(MockitoExtension.class)
public class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private StudentAcademicRecordRepository studentAcademicRecordRepository;

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private AcademicYearResolver academicYearResolver;

    @Mock
    private GuardianRepository guardianRepository;

    @Mock
    private GuardianService guardianService;

    @InjectMocks
    private StudentService studentService;

    private Student student;
    private School school;
    private AcademicYear academicYear;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId("student-id-123");
        student.setSchoolId("school-id-123");
        student.setAdmissionNo("ADM-001");
        student.setName("John Doe");
        student.setDob(LocalDate.of(2012, 5, 10));

        school = new School();
        school.setId("school-id-123");
        school.setMaxStudents(100);

        academicYear = new AcademicYear();
        academicYear.setName("2026-2027");
    }

    @Test
    void createStudent_Success() {
        student.setCurrentAcademicRecord(StudentAcademicRecord.builder().build());
        when(schoolRepository.findById("school-id-123")).thenReturn(Optional.of(school));
        when(studentRepository.countBySchoolId("school-id-123")).thenReturn(10L);
        when(studentRepository.findByAdmissionNo("ADM-001")).thenReturn(Optional.empty());
        when(studentRepository.save(student)).thenReturn(student);
        when(academicYearResolver.resolve(anyString(), any(), any())).thenReturn(academicYear);
        when(studentAcademicRecordRepository.save(any(StudentAcademicRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Student created = studentService.createStudent(student);

        assertNotNull(created);
        assertEquals("ADM-001", created.getAdmissionNo());
        verify(schoolRepository, times(1)).findById("school-id-123");
        verify(studentRepository, times(1)).findByAdmissionNo("ADM-001");
        verify(studentRepository, times(1)).save(student);
        verify(studentAcademicRecordRepository, times(1)).save(any(StudentAcademicRecord.class));
    }

    @Test
    void createStudent_withRequest_delegatesGuardianDedupToGuardianService() {
        CreateStudentRequest req = new CreateStudentRequest();
        req.setSchoolId("school-id-123");
        req.setName("Lucas Johnson");
        req.setAdmissionNo("ADM-2026-0003");
        req.setAcademicYear("2026-2027");

        StudentGuardianRequest gReq = StudentGuardianRequest.builder()
                .name("Priya Sharma")
                .relation(GuardianRelation.MOTHER)
                .phone("+61-400-555-666")
                .email("priya@example.com")
                .address("9 Oak Ave")
                .occupation("Teacher")
                .build();
        req.setGuardians(List.of(gReq, gReq)); // duplicate in request list — dedup is GuardianService's job

        // GuardianService owns the dedup + link building (covered by GuardianServiceTest);
        // here we only assert StudentService forwards the drafts and uses the result.
        GuardianLink link = GuardianLink.builder().guardianId("guardian-priya").primary(true).build();
        when(guardianService.buildDedupedLinks(eq("school-id-123"), isNull(), anyList()))
                .thenReturn(new ArrayList<>(List.of(link)));

        when(schoolRepository.findById("school-id-123")).thenReturn(Optional.of(school));
        when(studentRepository.countBySchoolId("school-id-123")).thenReturn(0L);
        when(studentRepository.findByAdmissionNo("ADM-2026-0003")).thenReturn(Optional.empty());
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));
        when(academicYearResolver.resolve(anyString(), any(), any())).thenReturn(academicYear);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GuardianService.GuardianDraft>> draftsCaptor = ArgumentCaptor.forClass(List.class);

        Student created = studentService.createStudent(req);

        assertNotNull(created);
        assertEquals("Lucas Johnson", created.getName());
        assertEquals(1, created.getGuardians().size());
        assertEquals("guardian-priya", created.getGuardians().get(0).getGuardianId());
        // Both request entries are forwarded as drafts; GuardianService collapses them.
        verify(guardianService).buildDedupedLinks(eq("school-id-123"), isNull(), draftsCaptor.capture());
        assertEquals(2, draftsCaptor.getValue().size());
        assertEquals("Priya Sharma", draftsCaptor.getValue().get(0).name());
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
    void createStudent_AdmissionNoDuplicate_ThrowsException() {
        when(schoolRepository.findById("school-id-123")).thenReturn(Optional.of(school));
        when(studentRepository.countBySchoolId("school-id-123")).thenReturn(10L);
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
                .academicYear("2026-2027")
                .studentId("STU-001")
                .rollNo("12")
                .build();

        when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
        when(studentAcademicRecordRepository.findByStudentDocId("student-id-123")).thenReturn(List.of(record));

        Student found = studentService.getStudentById("student-id-123");

        assertNotNull(found);
        assertEquals("student-id-123", found.getId());
        assertNotNull(found.getCurrentAcademicRecord());
        assertEquals("STU-001", found.getCurrentAcademicRecord().getStudentId());
        assertEquals("12", found.getCurrentAcademicRecord().getRollNo());
        assertEquals("2026-2027", found.getCurrentAcademicRecord().getAcademicYear());
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
        details.setName("Jane Doe");

        when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
        when(studentAcademicRecordRepository.findByStudentDocId("student-id-123")).thenReturn(new ArrayList<>());
        when(studentAcademicRecordRepository.findByStudentDocIdAndAcademicYear(anyString(), anyString())).thenReturn(Optional.empty());
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(academicYearResolver.resolve(anyString(), any(), any())).thenReturn(academicYear);

        Student updated = studentService.updateStudent("student-id-123", details);

        assertNotNull(updated);
        assertEquals("Jane Doe", updated.getName());
        assertEquals("ADM-001", updated.getAdmissionNo()); // unchanged
    }

    @Test
    void deleteStudent_Success() {
        when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
        when(studentAcademicRecordRepository.findByStudentDocId("student-id-123")).thenReturn(new ArrayList<>());

        studentService.deleteStudent("student-id-123");

        verify(studentRepository, times(1)).delete(student);
        verify(studentAcademicRecordRepository, times(1)).deleteAll(anyList());
    }

    @Test
    void createOrUpdateAcademicRecord_Success() {
        StudentAcademicRecord input = StudentAcademicRecord.builder()
                .academicYear("2026-2027")
                .classDocId("class-new")
                .build();

        when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
        when(schoolClassRepository.findById("class-new")).thenReturn(Optional.of(
                SchoolClass.builder().id("class-new").schoolId("school-id-123").build()
        ));
        when(studentAcademicRecordRepository.findByStudentDocId("student-id-123")).thenReturn(new ArrayList<>());
        when(studentAcademicRecordRepository.findByStudentDocIdAndAcademicYear("student-id-123", "2026-2027"))
                .thenReturn(Optional.empty());
        when(studentAcademicRecordRepository.save(any(StudentAcademicRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StudentAcademicRecord record = studentService.createOrUpdateAcademicRecord("student-id-123", input);

        assertNotNull(record);
        assertEquals("class-new", record.getClassDocId());
        assertEquals("2026-2027", record.getAcademicYear());
        verify(studentAcademicRecordRepository, times(1)).save(any(StudentAcademicRecord.class));
    }

    @Test
    void getAcademicHistory_Success() {
        StudentAcademicRecord r1 = StudentAcademicRecord.builder().academicYear("2025-2026").build();
        StudentAcademicRecord r2 = StudentAcademicRecord.builder().academicYear("2026-2027").build();

        when(studentRepository.existsById("student-id-123")).thenReturn(true);
        when(studentAcademicRecordRepository.findByStudentDocId("student-id-123")).thenReturn(List.of(r1, r2));

        List<StudentAcademicRecord> history = studentService.getAcademicHistory("student-id-123");

        assertEquals(2, history.size());
        verify(studentAcademicRecordRepository, times(1)).findByStudentDocId("student-id-123");
    }

    @Test
    void promoteStudent_Success() {
        StudentAcademicRecord input = StudentAcademicRecord.builder()
                .academicYear("2027-2028")
                .classDocId("class-new")
                .build();

        when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
        when(schoolClassRepository.findById("class-new")).thenReturn(Optional.of(
                SchoolClass.builder().id("class-new").schoolId("school-id-123").build()
        ));
        when(studentAcademicRecordRepository.findByStudentDocId("student-id-123")).thenReturn(new ArrayList<>());
        when(studentAcademicRecordRepository.findByStudentDocIdAndAcademicYear("student-id-123", "2027-2028"))
                .thenReturn(Optional.empty());
        when(studentAcademicRecordRepository.save(any(StudentAcademicRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StudentAcademicRecord record = studentService.promoteStudent("student-id-123", input);

        assertNotNull(record);
        assertEquals("class-new", record.getClassDocId());
        assertEquals("2027-2028", record.getAcademicYear());
        verify(studentAcademicRecordRepository, times(1)).save(any(StudentAcademicRecord.class));
    }

    @Test
    void getSiblings_Success() {
        // Siblings now share a guardian, not a parent.
        student.setGuardians(new ArrayList<>(List.of(
                GuardianLink.builder().guardianId("guardian-1").build())));

        Student sibling = new Student();
        sibling.setId("sibling-id-999");
        sibling.setSchoolId("school-id-123");
        sibling.setAdmissionNo("ADM-999");

        when(studentRepository.findById("student-id-123")).thenReturn(Optional.of(student));
        when(studentAcademicRecordRepository.findByStudentDocId("student-id-123")).thenReturn(new ArrayList<>());
        when(studentRepository.findByGuardiansGuardianId("guardian-1")).thenReturn(List.of(student, sibling));
        when(studentAcademicRecordRepository.findByStudentDocIdIn(anyList())).thenReturn(new ArrayList<>());

        List<Student> siblings = studentService.getSiblings("student-id-123");

        assertEquals(1, siblings.size());
        assertEquals("sibling-id-999", siblings.get(0).getId());
        verify(studentRepository, times(1)).findByGuardiansGuardianId("guardian-1");
    }
}
