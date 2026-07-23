package com.orbitastra.backend.services.academics;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.orbitastra.backend.models.academics.SchoolClass;
import com.orbitastra.backend.models.staff.Staff;
import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.repositories.academics.SchoolClassRepository;
import com.orbitastra.backend.repositories.core.SchoolRepository;
import com.orbitastra.backend.repositories.staff.StaffRepository;

@ExtendWith(MockitoExtension.class)
public class SchoolClassServiceTest {

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private SchoolClassService schoolClassService;

    private SchoolClass schoolClass;

    @BeforeEach
    void setUp() {
        schoolClass = new SchoolClass();
        schoolClass.setId("class-id");
        schoolClass.setSchoolId("school-id");
        schoolClass.setName("Class 10");
        schoolClass.setSections(new ArrayList<>());
        lenient().when(schoolRepository.existsById("school-id")).thenReturn(true);
    }

    @Test
    void addSections_Success() {
        when(schoolClassRepository.findById("class-id")).thenReturn(Optional.of(schoolClass));
        when(schoolClassRepository.save(any(SchoolClass.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<String> newSections = List.of("Section A", "Section B");
        SchoolClass updated = schoolClassService.addSections("class-id", newSections);

        assertNotNull(updated);
        assertTrue(updated.getSections().contains("Section A"));
        assertTrue(updated.getSections().contains("Section B"));
        assertEquals(2, updated.getSections().size());
        verify(schoolClassRepository, times(1)).save(schoolClass);
    }

    @Test
    void addSections_Duplicate_ThrowsException() {
        schoolClass.getSections().add("Section A");
        when(schoolClassRepository.findById("class-id")).thenReturn(Optional.of(schoolClass));

        List<String> newSections = List.of("Section B", "Section A");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            schoolClassService.addSections("class-id", newSections);
        });

        assertEquals("Section 'Section A' already exists in this class.", exception.getMessage());
        verify(schoolClassRepository, never()).save(any());
    }

    @Test
    void addSubject_Success_TrimsNameAndInitialisesSubjects() {
        schoolClass.setSubjects(null);
        when(schoolClassRepository.findById("class-id")).thenReturn(Optional.of(schoolClass));
        when(schoolClassRepository.save(any(SchoolClass.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SchoolClass updated = schoolClassService.addSubject("class-id",
                SchoolClass.ClassSubject.builder().name("  Mathematics  ").build());

        assertEquals(1, updated.getSubjects().size());
        assertEquals("Mathematics", updated.getSubjects().get(0).getName());
        assertNull(updated.getSubjects().get(0).getTeacherDocsId());
        verify(schoolClassRepository).save(schoolClass);
        verifyNoInteractions(staffRepository);
    }

    @Test
    void addSubject_WithTeacher_RequiresSameSchoolAndNormalisesReference() {
        Staff teacher = new Staff();
        teacher.setId("teacher-id");
        teacher.setSchoolId("school-id");
        when(schoolClassRepository.findById("class-id")).thenReturn(Optional.of(schoolClass));
        when(staffRepository.findById("teacher-id")).thenReturn(Optional.of(teacher));
        when(schoolClassRepository.save(any(SchoolClass.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SchoolClass updated = schoolClassService.addSubject("class-id",
                SchoolClass.ClassSubject.builder().name("Mathematics").teacherDocsId(" teacher-id ").build());

        assertEquals("teacher-id", updated.getSubjects().get(0).getTeacherDocsId());
        verify(staffRepository).findById("teacher-id");
        verify(schoolClassRepository).save(schoolClass);
    }

    @Test
    void addSubject_DuplicateName_IgnoresCaseAndWhitespace() {
        schoolClass.setSubjects(new ArrayList<>(List.of(
                SchoolClass.ClassSubject.builder().name("Mathematics").build())));
        when(schoolClassRepository.findById("class-id")).thenReturn(Optional.of(schoolClass));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                schoolClassService.addSubject("class-id",
                        SchoolClass.ClassSubject.builder().name(" mathematics ").build()));

        assertEquals("Subject 'mathematics' already exists in this class.", exception.getMessage());
        verify(schoolClassRepository, never()).save(any());
    }

    @Test
    void addSubject_RejectsBlankName() {
        when(schoolClassRepository.findById("class-id")).thenReturn(Optional.of(schoolClass));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                schoolClassService.addSubject("class-id",
                        SchoolClass.ClassSubject.builder().name("  ").build()));

        assertEquals("Subject name is required.", exception.getMessage());
        verify(schoolClassRepository, never()).save(any());
    }

    @Test
    void addSubject_UnknownTeacher_ThrowsNotFound() {
        when(schoolClassRepository.findById("class-id")).thenReturn(Optional.of(schoolClass));
        when(staffRepository.findById("missing-teacher")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                schoolClassService.addSubject("class-id",
                        SchoolClass.ClassSubject.builder().name("Mathematics")
                                .teacherDocsId("missing-teacher").build()));

        verify(schoolClassRepository, never()).save(any());
    }

    @Test
    void addSubject_CrossSchoolTeacher_IsRejected() {
        Staff teacher = new Staff();
        teacher.setId("teacher-id");
        teacher.setSchoolId("different-school");
        when(schoolClassRepository.findById("class-id")).thenReturn(Optional.of(schoolClass));
        when(staffRepository.findById("teacher-id")).thenReturn(Optional.of(teacher));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                schoolClassService.addSubject("class-id",
                        SchoolClass.ClassSubject.builder().name("Mathematics")
                                .teacherDocsId("teacher-id").build()));

        assertTrue(exception.getMessage().contains("does not belong to this school"));
        verify(schoolClassRepository, never()).save(any());
    }

    @Test
    void addSubject_BlankTeacherReference_IsRejected() {
        when(schoolClassRepository.findById("class-id")).thenReturn(Optional.of(schoolClass));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                schoolClassService.addSubject("class-id",
                        SchoolClass.ClassSubject.builder().name("Mathematics").teacherDocsId("  ").build()));

        assertEquals("teacherDocsId cannot be blank when provided.", exception.getMessage());
        verifyNoInteractions(staffRepository);
        verify(schoolClassRepository, never()).save(any());
    }

    @Test
    void addSubject_MissingClass_ThrowsNotFound() {
        when(schoolClassRepository.findById("missing-class")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                schoolClassService.addSubject("missing-class",
                        SchoolClass.ClassSubject.builder().name("Mathematics").build()));

        verify(schoolClassRepository, never()).save(any());
    }

    @Test
    void addSubject_ClassSchoolMissing_ThrowsNotFound() {
        when(schoolClassRepository.findById("class-id")).thenReturn(Optional.of(schoolClass));
        when(schoolRepository.existsById("school-id")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () ->
                schoolClassService.addSubject("class-id",
                        SchoolClass.ClassSubject.builder().name("Mathematics").build()));

        verify(schoolClassRepository, never()).save(any());
        verifyNoInteractions(staffRepository);
    }

    @Test
    void addSubject_BlankClassId_IsRejected() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                schoolClassService.addSubject("  ",
                        SchoolClass.ClassSubject.builder().name("Mathematics").build()));

        assertEquals("classId is required.", exception.getMessage());
        verifyNoInteractions(schoolClassRepository, schoolRepository, staffRepository);
    }

    @Test
    void addSubject_NullSubject_IsRejected() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                schoolClassService.addSubject("class-id", null));

        assertEquals("Subject request is required.", exception.getMessage());
        verifyNoInteractions(schoolClassRepository, schoolRepository, staffRepository);
    }
}
