package com.orbitastra.backend.services.finance;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.orbitastra.backend.models.old.core.AcademicYear;
import com.orbitastra.backend.models.old.finance.FeeInvoice;
import com.orbitastra.backend.models.old.finance.enums.FeeStatus;
import com.orbitastra.backend.models.old.finance.enums.FeeType;
import com.orbitastra.backend.models.old.student.Student;
import com.orbitastra.backend.repositories.finance.FeePaymentRepository;
import com.orbitastra.backend.repositories.finance.FeeRepository;
import com.orbitastra.backend.services.utils.AcademicYearResolver;
import com.orbitastra.backend.services.utils.StudentValidator;

@ExtendWith(MockitoExtension.class)
public class FeeServiceTest {

    @Mock private FeeRepository feeRepository;
    @Mock private FeePaymentRepository feePaymentRepository;
    @Mock private StudentValidator studentValidator;
    @Mock private AcademicYearResolver academicYearResolver;

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
        fee.setInvoiceNo("INV/2026/07/2501");
        fee.setStudentDocsId("student-123");
        fee.setSchoolId("school-123");
        fee.setType(FeeType.TUITION);
        fee.setAmount(new BigDecimal("500.00"));
        fee.setPaidAmount(BigDecimal.ZERO);
        fee.setStatus(FeeStatus.UNPAID);
        fee.setDueDate(LocalDate.now().plusMonths(1));
    }

    // ─── CREATE ────────────────────────────────────────────────────────────────

    @Test
    void createFee_WithPresetInvoiceNo_Success() {
        // invoiceNo is already set → generation loop is skipped entirely
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
        assertEquals("INV/2026/07/2501", created.getInvoiceNo()); // unchanged
        verify(studentValidator, times(1)).validateStudent("student-123", "school-123");
        verify(feeRepository, times(1)).save(fee);
    }

    @Test
    void createFee_WithBlankInvoiceNo_AutoGenerates() {
        // No invoiceNo set → service must auto-generate one
        fee.setInvoiceNo(null);
        AcademicYear year = AcademicYear.builder().name("2026-2027").build();
        when(studentValidator.validateStudent("student-123", "school-123")).thenReturn(student);
        when(academicYearResolver.resolve(any(), any(), any())).thenReturn(year);
        when(feeRepository.existsByInvoiceNo(anyString())).thenReturn(false);
        when(feeRepository.save(fee)).thenReturn(fee);

        FeeInvoice created = feeService.createFee(fee);

        assertNotNull(created.getInvoiceNo());
        assertTrue(created.getInvoiceNo().startsWith("INV/"));
        verify(feeRepository, times(1)).save(fee);
    }

    @Test
    void createFee_StudentNotFound_ThrowsException() {
        when(studentValidator.validateStudent("student-123", "school-123"))
                .thenThrow(new ResourceNotFoundException("Student not found with id: student-123"));

        assertThrows(ResourceNotFoundException.class, () -> feeService.createFee(fee));

        verifyNoInteractions(feeRepository);
    }

    // ─── GET ───────────────────────────────────────────────────────────────────

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

        assertThrows(ResourceNotFoundException.class, () -> feeService.getFeeById("fee-123"));
    }

    @Test
    void getFeeByInvoiceNo_Success() {
        when(feeRepository.findByInvoiceNo("INV/2026/07/2501")).thenReturn(Optional.of(fee));

        FeeInvoice result = feeService.getFeeByInvoiceNo("INV/2026/07/2501");

        assertNotNull(result);
        assertEquals("INV/2026/07/2501", result.getInvoiceNo());
    }

    @Test
    void getFeeByInvoiceNo_NotFound_ThrowsException() {
        when(feeRepository.findByInvoiceNo("INV/2026/07/2501")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> feeService.getFeeByInvoiceNo("INV/2026/07/2501"));
    }

    // ─── UPDATE — happy path ────────────────────────────────────────────────

    @Test
    void updateFee_NoPriorPayments_Success() {
        // Invoice has no payments → edit is allowed
        when(feeRepository.findById("fee-123")).thenReturn(Optional.of(fee));
        when(feePaymentRepository.existsByFeeDocsId("fee-123")).thenReturn(false);
        when(feeRepository.save(any(FeeInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

        FeeInvoice patch = new FeeInvoice();
        patch.setAmount(new BigDecimal("600.00"));

        FeeInvoice result = feeService.updateFee("fee-123", patch);

        assertEquals(new BigDecimal("600.00"), result.getAmount());
        verify(feeRepository, times(1)).save(any(FeeInvoice.class));
    }

    @Test
    void updateFee_OnlyUpdateType_NoPriorPayments_Success() {
        when(feeRepository.findById("fee-123")).thenReturn(Optional.of(fee));
        when(feePaymentRepository.existsByFeeDocsId("fee-123")).thenReturn(false);
        when(feeRepository.save(any(FeeInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

        FeeInvoice patch = new FeeInvoice();
        patch.setType(FeeType.LIBRARY);

        FeeInvoice result = feeService.updateFee("fee-123", patch);

        assertEquals(FeeType.LIBRARY, result.getType());
    }

    @Test
    void updateFee_OnlyUpdateDiscount_NoPriorPayments_Success() {
        when(feeRepository.findById("fee-123")).thenReturn(Optional.of(fee));
        when(feePaymentRepository.existsByFeeDocsId("fee-123")).thenReturn(false);
        when(feeRepository.save(any(FeeInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

        FeeInvoice patch = new FeeInvoice();
        patch.setDiscount(new BigDecimal("50.00"));

        FeeInvoice result = feeService.updateFee("fee-123", patch);

        assertEquals(new BigDecimal("50.00"), result.getDiscount());
    }

    @Test
    void updateFee_OnlyUpdateDueDate_NoPriorPayments_Success() {
        LocalDate newDate = LocalDate.now().plusMonths(3);
        when(feeRepository.findById("fee-123")).thenReturn(Optional.of(fee));
        when(feePaymentRepository.existsByFeeDocsId("fee-123")).thenReturn(false);
        when(feeRepository.save(any(FeeInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

        FeeInvoice patch = new FeeInvoice();
        patch.setDueDate(newDate);

        FeeInvoice result = feeService.updateFee("fee-123", patch);

        assertEquals(newDate, result.getDueDate());
    }

    @Test
    void updateFee_EmptyPatch_NoPriorPayments_Success_NoFieldChanges() {
        // An empty patch should not blow up — fields stay the same, status is recalculated
        when(feeRepository.findById("fee-123")).thenReturn(Optional.of(fee));
        when(feePaymentRepository.existsByFeeDocsId("fee-123")).thenReturn(false);
        when(feeRepository.save(any(FeeInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

        FeeInvoice patch = new FeeInvoice(); // no fields set

        FeeInvoice result = feeService.updateFee("fee-123", patch);

        assertEquals(new BigDecimal("500.00"), result.getAmount()); // unchanged
    }

    // ─── UPDATE — LOCKED (payment already exists) ──────────────────────────

    @Test
    void updateFee_HasPayment_ThrowsIllegalStateException() {
        when(feeRepository.findById("fee-123")).thenReturn(Optional.of(fee));
        when(feePaymentRepository.existsByFeeDocsId("fee-123")).thenReturn(true); // payment exists

        FeeInvoice patch = new FeeInvoice();
        patch.setAmount(new BigDecimal("999.00"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> feeService.updateFee("fee-123", patch));

        assertTrue(ex.getMessage().contains("INV/2026/07/2501"));
        assertTrue(ex.getMessage().contains("cannot be modified"));
        verify(feeRepository, never()).save(any()); // must NOT be saved
    }

    @Test
    void updateFee_HasPayment_ChangeType_ThrowsIllegalStateException() {
        when(feeRepository.findById("fee-123")).thenReturn(Optional.of(fee));
        when(feePaymentRepository.existsByFeeDocsId("fee-123")).thenReturn(true);

        FeeInvoice patch = new FeeInvoice();
        patch.setType(FeeType.LIBRARY);

        assertThrows(IllegalStateException.class, () -> feeService.updateFee("fee-123", patch));
        verify(feeRepository, never()).save(any());
    }

    @Test
    void updateFee_HasPayment_ChangeDiscount_ThrowsIllegalStateException() {
        when(feeRepository.findById("fee-123")).thenReturn(Optional.of(fee));
        when(feePaymentRepository.existsByFeeDocsId("fee-123")).thenReturn(true);

        FeeInvoice patch = new FeeInvoice();
        patch.setDiscount(new BigDecimal("100.00"));

        assertThrows(IllegalStateException.class, () -> feeService.updateFee("fee-123", patch));
        verify(feeRepository, never()).save(any());
    }

    @Test
    void updateFee_HasPayment_ChangeDueDate_ThrowsIllegalStateException() {
        when(feeRepository.findById("fee-123")).thenReturn(Optional.of(fee));
        when(feePaymentRepository.existsByFeeDocsId("fee-123")).thenReturn(true);

        FeeInvoice patch = new FeeInvoice();
        patch.setDueDate(LocalDate.now().plusYears(1));

        assertThrows(IllegalStateException.class, () -> feeService.updateFee("fee-123", patch));
        verify(feeRepository, never()).save(any());
    }

    @Test
    void updateFee_InvoiceNotFound_ThrowsResourceNotFoundException() {
        when(feeRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> feeService.updateFee("bad-id", new FeeInvoice()));

        verify(feePaymentRepository, never()).existsByFeeDocsId(any()); // never reached
    }

    // ─── DELETE ────────────────────────────────────────────────────────────────

    @Test
    void deleteFee_NoPriorPayments_Success() {
        when(feeRepository.findById("fee-123")).thenReturn(Optional.of(fee));
        when(feePaymentRepository.existsByFeeDocsId("fee-123")).thenReturn(false);

        assertDoesNotThrow(() -> feeService.deleteFee("fee-123"));

        verify(feeRepository, times(1)).delete(fee);
    }

    @Test
    void deleteFee_HasPayment_ThrowsIllegalStateException() {
        when(feeRepository.findById("fee-123")).thenReturn(Optional.of(fee));
        when(feePaymentRepository.existsByFeeDocsId("fee-123")).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> feeService.deleteFee("fee-123"));

        assertTrue(ex.getMessage().contains("cannot be modified"));
        verify(feeRepository, never()).delete(any()); // must NOT be deleted
    }

    @Test
    void deleteFee_InvoiceNotFound_ThrowsResourceNotFoundException() {
        when(feeRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> feeService.deleteFee("bad-id"));

        verify(feePaymentRepository, never()).existsByFeeDocsId(any());
        verify(feeRepository, never()).delete(any());
    }

    // ─── PAID AMOUNT / STATUS ────────────────────────────────────────────────

    @Test
    void applyPaidAmount_PartialPayment_SetsPartiallyPaid() {
        when(feeRepository.save(any(FeeInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

        FeeInvoice result = feeService.applyPaidAmount(fee, new BigDecimal("200.00"));

        assertEquals(new BigDecimal("200.00"), result.getPaidAmount());
        assertEquals(FeeStatus.PARTIALLY_PAID, result.getStatus());
        verify(feeRepository, times(1)).save(fee);
    }

    @Test
    void applyPaidAmount_FullPayment_SetsPaid() {
        when(feeRepository.save(any(FeeInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

        FeeInvoice result = feeService.applyPaidAmount(fee, new BigDecimal("500.00"));

        assertEquals(new BigDecimal("500.00"), result.getPaidAmount());
        assertEquals(FeeStatus.PAID, result.getStatus());
        verify(feeRepository, times(1)).save(fee);
    }

    @Test
    void applyPaidAmount_ZeroPaid_SetsUnpaid() {
        when(feeRepository.save(any(FeeInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

        FeeInvoice result = feeService.applyPaidAmount(fee, BigDecimal.ZERO);

        assertEquals(BigDecimal.ZERO, result.getPaidAmount());
        assertEquals(FeeStatus.UNPAID, result.getStatus());
    }

    @Test
    void applyPaidAmount_NullTreatedAsZero_SetsUnpaid() {
        when(feeRepository.save(any(FeeInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

        FeeInvoice result = feeService.applyPaidAmount(fee, null);

        assertEquals(BigDecimal.ZERO, result.getPaidAmount());
        assertEquals(FeeStatus.UNPAID, result.getStatus());
    }

    @Test
    void applyPaidAmount_FullyWaived_ZeroNetPayable_SetsPaid() {
        // discount == amount → net payable = 0 → any paid >= 0 should be PAID
        fee.setAmount(new BigDecimal("500.00"));
        fee.setDiscount(new BigDecimal("500.00"));
        when(feeRepository.save(any(FeeInvoice.class))).thenAnswer(inv -> inv.getArgument(0));

        FeeInvoice result = feeService.applyPaidAmount(fee, BigDecimal.ZERO);

        assertEquals(FeeStatus.PAID, result.getStatus()); // 0 paid >= 0 net → PAID
    }
}
