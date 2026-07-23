package com.orbitastra.backend.controllers.academics;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.orbitastra.backend.models.academics.SchoolClass;
import com.orbitastra.backend.dto.academics.AddClassSubjectRequest;
import com.orbitastra.backend.services.academics.SchoolClassService;

@ExtendWith(MockitoExtension.class)
public class SchoolClassControllerTest {

    @Mock
    private SchoolClassService schoolClassService;

    @InjectMocks
    private SchoolClassController schoolClassController;

    private SchoolClass schoolClass;

    @BeforeEach
    void setUp() {
        schoolClass = new SchoolClass();
        schoolClass.setId("class-id");
        schoolClass.setSchoolId("school-id");
        schoolClass.setName("Class 10");
        schoolClass.setSections(List.of("Section A", "Section B"));
    }

    @Test
    void addSection_List_Success() {
        when(schoolClassService.addSections("class-id", List.of("Section A", "Section B"))).thenReturn(schoolClass);

        ResponseEntity<SchoolClass> response = schoolClassController.addSection(
                "class-id", Map.of("section", List.of("Section A", "Section B")));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(schoolClass, response.getBody());
        verify(schoolClassService, times(1)).addSections("class-id", List.of("Section A", "Section B"));
    }

    @Test
    void addSection_EmptyBody_ThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            schoolClassController.addSection("class-id", Map.of());
        });

        assertEquals("Section list cannot be null or empty.", exception.getMessage());
    }

    @Test
    void addSection_EmptyList_ThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            schoolClassController.addSection("class-id", Map.of("section", List.of()));
        });

        assertEquals("Section list cannot be null or empty.", exception.getMessage());
    }

    @Test
    void addSubject_MapsRequestToCurrentReferenceFields() {
        when(schoolClassService.addSubject(eq("class-id"), any(SchoolClass.ClassSubject.class)))
                .thenReturn(schoolClass);
        AddClassSubjectRequest request = new AddClassSubjectRequest();
        request.setName(" Mathematics ");
        request.setTeacherDocsId("teacher-id");

        ResponseEntity<SchoolClass> response = schoolClassController.addSubject("class-id", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(schoolClass, response.getBody());
        verify(schoolClassService).addSubject(eq("class-id"), argThat(subject ->
                " Mathematics ".equals(subject.getName()) && "teacher-id".equals(subject.getTeacherDocsId())));
    }

    @Test
    void addSubject_NullRequest_IsRejected() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                schoolClassController.addSubject("class-id", null));

        assertEquals("Subject request is required.", exception.getMessage());
        verifyNoInteractions(schoolClassService);
    }
}
