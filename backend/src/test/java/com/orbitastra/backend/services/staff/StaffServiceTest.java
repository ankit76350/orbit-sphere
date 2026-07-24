package com.orbitastra.backend.services.staff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.orbitastra.backend.models.staff.Staff;
import com.orbitastra.backend.repositories.core.SchoolRepository;
import com.orbitastra.backend.repositories.staff.StaffRepository;

@ExtendWith(MockitoExtension.class)
class StaffServiceTest {

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private SchoolRepository schoolRepository;

    private StaffService staffService;

    @BeforeEach
    void setUp() {
        staffService = new StaffService(staffRepository, schoolRepository);
    }

    @Test
    void createStaff_checksAndPersistsEmployeeNumber() {
        Staff staff = Staff.builder()
                .schoolId("school-id")
                .employeeNo("EMP-001")
                .name("Asha Rao")
                .build();

        when(schoolRepository.existsById("school-id")).thenReturn(true);
        when(staffRepository.findByEmployeeNo("EMP-001")).thenReturn(Optional.empty());
        when(staffRepository.save(staff)).thenReturn(staff);

        Staff created = staffService.createStaff(staff);

        assertEquals("EMP-001", created.getEmployeeNo());
        verify(staffRepository).findByEmployeeNo("EMP-001");
        verify(staffRepository).save(staff);
    }

    @Test
    void createStaff_rejectsDuplicateEmployeeNumber() {
        Staff staff = Staff.builder()
                .schoolId("school-id")
                .employeeNo("EMP-001")
                .build();

        when(schoolRepository.existsById("school-id")).thenReturn(true);
        when(staffRepository.findByEmployeeNo("EMP-001"))
                .thenReturn(Optional.of(Staff.builder().employeeNo("EMP-001").build()));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> staffService.createStaff(staff));

        assertEquals("Employee number 'EMP-001' is already taken.", exception.getMessage());
        verify(staffRepository, never()).save(any(Staff.class));
    }

    @Test
    void getStaffByEmployeeNo_usesRenamedRepositoryQuery() {
        Staff staff = Staff.builder().employeeNo("EMP-001").build();
        when(staffRepository.findByEmployeeNo("EMP-001")).thenReturn(Optional.of(staff));

        Staff found = staffService.getStaffByEmployeeNo("EMP-001");

        assertEquals("EMP-001", found.getEmployeeNo());
    }
}
