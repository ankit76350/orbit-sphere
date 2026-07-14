package com.orbitastra.backend.services.finance;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
import com.orbitastra.backend.models.finance.FeePayment;
import com.orbitastra.backend.models.finance.enums.FeeStatus;
import com.orbitastra.backend.models.finance.enums.FeeType;
import com.orbitastra.backend.models.finance.enums.PaymentMode;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.repositories.finance.FeePaymentRepository;
import com.orbitastra.backend.repositories.finance.FeeRepository;
import com.orbitastra.backend.repositories.student.StudentRepository;
import com.orbitastra.backend.services.core.AcademicYearResolver;

@ExtendWith(MockitoExtension.class)
public class FeeServiceTest {

    @Mock
    private FeeRepository feeRepository;

    @Mock
    private FeePaymentRepository feePaymentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentWalletService studentWalletService;

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
        AcademicYear year = AcademicYear.builder().name("2026-2027").build();
        when(studentRepository.findById("student-123")).thenReturn(Optional.of(student));
        when(academicYearResolver.resolve(any(), any(), any())).thenReturn(year);
        when(feeRepository.save(fee)).thenReturn(fee);

        FeeInvoice created = feeService.createFee(fee);

        assertNotNull(created);
        assertEquals("school-123", created.getSchoolId());
        assertEquals("2026-2027", created.getAcademicYear());
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
    void recordPayment_PartialPayment_Success() {
        when(feeRepository.findById("fee-123")).thenReturn(Optional.of(fee));
        when(feePaymentRepository.findByFeeId("fee-123"))
                .thenReturn(List.of(FeePayment.builder().amount(new BigDecimal("200.00")).build()));
        when(feeRepository.save(any(FeeInvoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FeeInvoice result = feeService.recordPayment(
                "fee-123", new BigDecimal("200.00"), PaymentMode.CASH, "partial", "admin");

        assertNotNull(result);
        assertEquals(new BigDecimal("200.00"), result.getPaidAmount());
        assertEquals(FeeStatus.PARTIALLY_PAID, result.getStatus());
        verify(feePaymentRepository, times(1)).save(any(FeePayment.class));
        verify(feeRepository, times(1)).save(fee);
    }

    @Test
    void recordPayment_FullPayment_Success() {
        when(feeRepository.findById("fee-123")).thenReturn(Optional.of(fee));
        when(feePaymentRepository.findByFeeId("fee-123"))
                .thenReturn(List.of(FeePayment.builder().amount(new BigDecimal("500.00")).build()));
        when(feeRepository.save(any(FeeInvoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FeeInvoice result = feeService.recordPayment(
                "fee-123", new BigDecimal("500.00"), PaymentMode.CASH, "full", "admin");

        assertNotNull(result);
        assertEquals(new BigDecimal("500.00"), result.getPaidAmount());
        assertEquals(FeeStatus.PAID, result.getStatus());
        verify(feePaymentRepository, times(1)).save(any(FeePayment.class));
        verify(feeRepository, times(1)).save(fee);
    }

    @Test
    void recordPayment_ExceedsBalance_ThrowsException() {
        when(feeRepository.findById("fee-123")).thenReturn(Optional.of(fee));

        assertThrows(IllegalArgumentException.class, () -> {
            feeService.recordPayment(
                    "fee-123", new BigDecimal("600.00"), PaymentMode.CASH, null, "admin");
        });

        verify(feePaymentRepository, never()).save(any());
        verify(feeRepository, never()).save(any());
    }

    @Test
    void recordPayment_NonPositiveAmount_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            feeService.recordPayment(
                    "fee-123", BigDecimal.ZERO, PaymentMode.CASH, null, "admin");
        });

        verifyNoInteractions(feeRepository);
        verifyNoInteractions(feePaymentRepository);
    }

    @Test
    void recordPayment_AlreadyPaid_ThrowsException() {
        fee.setStatus(FeeStatus.PAID);
        when(feeRepository.findById("fee-123")).thenReturn(Optional.of(fee));

        assertThrows(IllegalArgumentException.class, () -> {
            feeService.recordPayment(
                    "fee-123", new BigDecimal("100.00"), PaymentMode.CASH, null, "admin");
        });

        verify(feePaymentRepository, never()).save(any());
        verify(feeRepository, never()).save(any());
    }

    @Test
    void recordPayment_ViaWallet_Success() {
        when(feeRepository.findById("fee-123")).thenReturn(Optional.of(fee));
        when(feePaymentRepository.findByFeeId("fee-123"))
                .thenReturn(List.of(FeePayment.builder().amount(new BigDecimal("300.00")).build()));
        when(feeRepository.save(any(FeeInvoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FeeInvoice result = feeService.recordPayment(
                "fee-123", new BigDecimal("300.00"), PaymentMode.WALLET, "wallet pay", "admin");

        assertNotNull(result);
        assertEquals(new BigDecimal("300.00"), result.getPaidAmount());
        assertEquals(FeeStatus.PARTIALLY_PAID, result.getStatus());
        verify(studentWalletService, times(1)).debitWallet(
                eq("student-123"),
                eq(new BigDecimal("300.00")),
                contains("invoice fee-123"));
        verify(feePaymentRepository, times(1)).save(any(FeePayment.class));
        verify(feeRepository, times(1)).save(fee);
    }
}
