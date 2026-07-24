package com.orbitastra.backend.services.finance;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.core.AcademicYear;
import com.orbitastra.backend.models.finance.FeeInvoice;
import com.orbitastra.backend.models.finance.enums.FeeStatus;
import com.orbitastra.backend.models.finance.enums.FeeType;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.repositories.finance.FeeRepository;
import com.orbitastra.backend.services.utils.AcademicYearResolver;
import com.orbitastra.backend.services.utils.StudentValidator;

@ExtendWith(MockitoExtension.class)
public class FeeServiceTest {

    @Mock
    private FeeRepository feeRepository;

    @Mock
    private StudentValidator studentValidator;

    @Mock
    private AcademicYearResolver academicYearResolver;

    @InjectMocks
    private FeeService feeService;

    private Student student;
    private FeeInvoice fee;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId("student-123");
        student.setSchoolId("school-123");

        fee = new FeeInvoice();
        fee.setId("fee-123");
        fee.setStudentDocsId("student-123");
        fee.setSchoolId("school-123");
        fee.setType(FeeType.TUITION);
        fee.setAmount(new BigDecimal("500.00"));
        fee.setPaidAmount(BigDecimal.ZERO);
        fee.setStatus(FeeStatus.UNPAID);
        fee.setDueDate(LocalDate.now().plusMonths(1));
    }

    @Test
    void createFee_Success() {
        AcademicYear year = AcademicYear.builder().name("2026-2027").build();
        when(studentValidator.validateStudent("student-123", "school-123")).thenReturn(student);
        when(academicYearResolver.resolve(any(), any(), any())).thenReturn(year);
        when(feeRepository.save(fee)).thenReturn(fee);

        FeeInvoice created = feeService.createFee(fee);

        assertNotNull(created);
        assertEquals("school-123", created.getSchoolId());
        assertEquals("2026-2027", created.getAcademicYear());
        assertEquals(FeeStatus.UNPAID, created.getStatus());
        assertEquals(BigDecimal.ZERO, created.getPaidAmount());
        verify(studentValidator, times(1)).validateStudent("student-123", "school-123");
        verify(feeRepository, times(1)).save(fee);
    }

    @Test
    void createFee_StudentNotFound_ThrowsException() {
        when(studentValidator.validateStudent("student-123", "school-123"))
                .thenThrow(new ResourceNotFoundException("Student not found with id: student-123"));

        assertThrows(ResourceNotFoundException.class, () -> {
            feeService.createFee(fee);
        });

        verifyNoInteractions(feeRepository);
    }

    @Test
    void getFeeById_Success() {
        when(feeRepository.findById("fee-123")).thenReturn(Optional.of(fee));

        FeeInvoice result = feeService.getFeeById("fee-123");

        assertNotNull(result);
        assertEquals("fee-123", result.getId());
        verify(feeRepository, times(1)).findById("fee-123");
    }

    @Test
    void getFeeById_NotFound_ThrowsException() {
        when(feeRepository.findById("fee-123")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            feeService.getFeeById("fee-123");
        });
    }

    @Test
    void applyPaidAmount_PartialPayment_SetsPartiallyPaid() {
        when(feeRepository.save(any(FeeInvoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FeeInvoice result = feeService.applyPaidAmount(fee, new BigDecimal("200.00"));

        assertEquals(new BigDecimal("200.00"), result.getPaidAmount());
        assertEquals(FeeStatus.PARTIALLY_PAID, result.getStatus());
        verify(feeRepository, times(1)).save(fee);
    }

    @Test
    void applyPaidAmount_FullPayment_SetsPaid() {
        when(feeRepository.save(any(FeeInvoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FeeInvoice result = feeService.applyPaidAmount(fee, new BigDecimal("500.00"));

        assertEquals(new BigDecimal("500.00"), result.getPaidAmount());
        assertEquals(FeeStatus.PAID, result.getStatus());
        verify(feeRepository, times(1)).save(fee);
    }
}
