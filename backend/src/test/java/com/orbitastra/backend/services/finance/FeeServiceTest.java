package com.orbitastra.backend.services.finance;

import static org.junit.jupiter.api.Assertions.*;
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
import com.orbitastra.backend.models.finance.Fee;
import com.orbitastra.backend.models.finance.enums.FeeStatus;
import com.orbitastra.backend.models.finance.enums.FeeType;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.repositories.finance.FeeRepository;
import com.orbitastra.backend.repositories.student.StudentRepository;

@ExtendWith(MockitoExtension.class)
public class FeeServiceTest {

    @Mock
    private FeeRepository feeRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentWalletService studentWalletService;

    @InjectMocks
    private FeeService feeService;

    private Student student;
    private Fee fee;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId("student-123");
        student.setSchoolId("school-123");

        fee = new Fee();
        fee.setId("fee-123");
        fee.setStudentId("student-123");
        fee.setSchoolId("school-123");
        fee.setType(FeeType.TUITION);
        fee.setAmount(new BigDecimal("500.00"));
        fee.setPaidAmount(BigDecimal.ZERO);
        fee.setStatus(FeeStatus.UNPAID);
        fee.setDueDate(LocalDate.now().plusMonths(1));
    }

    @Test
    void createFee_Success() {
        when(studentRepository.findById("student-123")).thenReturn(Optional.of(student));
        when(feeRepository.save(fee)).thenReturn(fee);

        Fee created = feeService.createFee(fee);

        assertNotNull(created);
        assertEquals("school-123", created.getSchoolId());
        assertEquals(FeeStatus.UNPAID, created.getStatus());
        assertEquals(BigDecimal.ZERO, created.getPaidAmount());
        verify(studentRepository, times(1)).findById("student-123");
        verify(feeRepository, times(1)).save(fee);
    }

    @Test
    void createFee_StudentNotFound_ThrowsException() {
        when(studentRepository.findById("student-123")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            feeService.createFee(fee);
        });

        verifyNoInteractions(feeRepository);
    }

    @Test
    void getFeeById_Success() {
        when(feeRepository.findById("fee-123")).thenReturn(Optional.of(fee));

        Fee result = feeService.getFeeById("fee-123");

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
    void payFee_PartialPayment_Success() {
        when(feeRepository.findById("fee-123")).thenReturn(Optional.of(fee));
        when(feeRepository.save(any(Fee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Fee result = feeService.payFee("fee-123", new BigDecimal("200.00"));

        assertNotNull(result);
        assertEquals(new BigDecimal("200.00"), result.getPaidAmount());
        assertEquals(FeeStatus.PARTIALLY_PAID, result.getStatus());
        verify(feeRepository, times(1)).save(fee);
    }

    @Test
    void payFee_FullPayment_Success() {
        when(feeRepository.findById("fee-123")).thenReturn(Optional.of(fee));
        when(feeRepository.save(any(Fee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Fee result = feeService.payFee("fee-123", new BigDecimal("500.00"));

        assertNotNull(result);
        assertEquals(new BigDecimal("500.00"), result.getPaidAmount());
        assertEquals(FeeStatus.PAID, result.getStatus());
        verify(feeRepository, times(1)).save(fee);
    }

    @Test
    void payFee_ExceedsBalance_ThrowsException() {
        when(feeRepository.findById("fee-123")).thenReturn(Optional.of(fee));

        assertThrows(IllegalArgumentException.class, () -> {
            feeService.payFee("fee-123", new BigDecimal("600.00"));
        });

        verify(feeRepository, never()).save(any());
    }

    @Test
    void payFeeViaWallet_Success() {
        when(feeRepository.findById("fee-123")).thenReturn(Optional.of(fee));
        when(feeRepository.save(any(Fee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Fee result = feeService.payFeeViaWallet("fee-123", new BigDecimal("300.00"));

        assertNotNull(result);
        assertEquals(new BigDecimal("300.00"), result.getPaidAmount());
        assertEquals(FeeStatus.PARTIALLY_PAID, result.getStatus());
        verify(studentWalletService, times(1)).debitWallet(
                eq("student-123"), 
                eq(new BigDecimal("300.00")), 
                contains("Payment for Fee ID: fee-123")
        );
        verify(feeRepository, times(1)).save(fee);
    }
}
