package com.orbitastra.backend.services.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.repositories.core.AcademicYearRepository;
import com.orbitastra.backend.repositories.core.SchoolRepository;

@ExtendWith(MockitoExtension.class)
class AcademicYearServiceTest {

    @Mock
    private AcademicYearRepository academicYearRepository;

    @Mock
    private SchoolRepository schoolRepository;

    private AcademicYearService academicYearService;
    private AcademicYear existingYear;

    @BeforeEach
    void setUp() {
        academicYearService = new AcademicYearService(academicYearRepository, schoolRepository);
        existingYear = AcademicYear.builder()
                .id("year-id")
                .schoolId("school-id")
                .name("2026-2027")
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2027, 3, 31))
                .build();
    }

    @Test
    void updateAcademicYear_preservesNameWhenUpdateOmitsIt() {
        AcademicYear details = AcademicYear.builder()
                .schoolId("school-id")
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2027, 4, 30))
                .build();

        when(academicYearRepository.findById("year-id")).thenReturn(Optional.of(existingYear));
        when(schoolRepository.existsById("school-id")).thenReturn(true);
        when(academicYearRepository.findBySchoolId("school-id")).thenReturn(List.of(existingYear));
        when(academicYearRepository.save(any(AcademicYear.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AcademicYear updated = academicYearService.updateAcademicYear("year-id", details);

        assertEquals("2026-2027", updated.getName());
        assertEquals(LocalDate.of(2026, 5, 1), updated.getStartDate());
        assertEquals(LocalDate.of(2027, 4, 30), updated.getEndDate());
    }

    @Test
    void updateAcademicYear_rejectsChangedNameForDirectServiceCallers() {
        AcademicYear details = AcademicYear.builder()
                .schoolId("school-id")
                .name("2027-2028")
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2027, 3, 31))
                .build();

        when(academicYearRepository.findById("year-id")).thenReturn(Optional.of(existingYear));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> academicYearService.updateAcademicYear("year-id", details));

        assertEquals("The name of an academic year cannot be changed once created ('2026-2027') — "
                + "student records, classes and other data reference it by name. "
                + "Create a new academic year instead.", exception.getMessage());
        verify(academicYearRepository, never()).save(any(AcademicYear.class));
    }
}
