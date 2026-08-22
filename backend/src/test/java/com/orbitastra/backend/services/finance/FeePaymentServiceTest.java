package com.orbitastra.backend.services.finance;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.orbitastra.backend.models.old.finance.FeeInvoice;
import com.orbitastra.backend.models.old.finance.FeePayment;
import com.orbitastra.backend.models.old.finance.enums.FeeStatus;
import com.orbitastra.backend.models.old.finance.enums.FeeType;
import com.orbitastra.backend.models.old.finance.enums.PaymentMode;
import com.orbitastra.backend.old.repositories.finance.FeePaymentRepository;
import com.orbitastra.backend.old.services.finance.FeePaymentService;
import com.orbitastra.backend.old.services.finance.FeeService;
import com.orbitastra.backend.old.services.finance.StudentWalletService;

@ExtendWith(MockitoExtension.class)
public class FeePaymentServiceTest {

    @Mock
    private FeePaymentRepository feePaymentRepository;

    @Mock
    private FeeService feeService;

    @Mock
    private StudentWalletService studentWalletService;

    @InjectMocks
    private FeePaymentService feePaymentService;

    private FeeInvoice fee;

    @BeforeEach
    void setUp() {
        fee = new FeeInvoice();
        fee.setId("fee-123");
        fee.setStudentDocsId("student-123");
        fee.setSchoolId("school-123");
        fee.setAcademicYear("2026-2027");
        fee.setType(FeeType.TUITION);
        fee.setAmount(new BigDecimal("500.00"));
        fee.setPaidAmount(BigDecimal.ZERO);
        fee.setStatus(FeeStatus.UNPAID);
        fee.setDueDate(LocalDate.now().plusMonths(1));

        // Mock remainingBalance call to return remaining balance dynamically
        lenient().when(feeService.remainingBalance(any(FeeInvoice.class)))
                .thenAnswer(invocation -> {
                    FeeInvoice invoice = invocation.getArgument(0);
                    BigDecimal amount = invoice.getAmount() != null ? invoice.getAmount() : BigDecimal.ZERO;
                    BigDecimal discount = invoice.getDiscount() != null ? invoice.getDiscount() : BigDecimal.ZERO;
                    BigDecimal net = amount.subtract(discount);
                    BigDecimal netPayable = net.compareTo(BigDecimal.ZERO) > 0 ? net : BigDecimal.ZERO;
                    BigDecimal paid = invoice.getPaidAmount() != null ? invoice.getPaidAmount() : BigDecimal.ZERO;
                    BigDecimal remaining = netPayable.subtract(paid);
                    return remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO;
                });
    }

    // Mimics the real FeeService.applyPaidAmount so result assertions stay meaningful.
    private void stubApplyPaidAmount() {
        when(feeService.applyPaidAmount(any(FeeInvoice.class), any(BigDecimal.class)))
                .thenAnswer(invocation -> {
                    FeeInvoice invoice = invocation.getArgument(0);
                    BigDecimal total = invocation.getArgument(1);
                    invoice.setPaidAmount(total);
                    if (total.compareTo(BigDecimal.ZERO) == 0) {
                        invoice.setStatus(FeeStatus.UNPAID);
                    } else if (total.compareTo(invoice.getAmount()) >= 0) {
                        invoice.setStatus(FeeStatus.PAID);
                    } else {
                        invoice.setStatus(FeeStatus.PARTIALLY_PAID);
                    }
                    return invoice;
                });
    }

    @Test
    void recordPayment_PartialPayment_Success() {
        when(feeService.getFeeById("fee-123")).thenReturn(fee);
        when(feePaymentRepository.existsByReceiptNo(anyString())).thenReturn(false);
        when(feePaymentRepository.findByFeeDocsId("fee-123"))
                .thenReturn(List.of(FeePayment.builder().amount(new BigDecimal("200.00")).build()));
        stubApplyPaidAmount();

        FeeInvoice result = feePaymentService.recordPayment(
                "fee-123", new BigDecimal("200.00"), PaymentMode.CASH, "partial", "admin");

        assertNotNull(result);
        assertEquals(new BigDecimal("200.00"), result.getPaidAmount());
        assertEquals(FeeStatus.PARTIALLY_PAID, result.getStatus());
        verify(feePaymentRepository, times(1)).save(any(FeePayment.class));
        verify(feeService, times(1)).applyPaidAmount(fee, new BigDecimal("200.00"));
    }

    @Test
    void recordPayment_FullPayment_Success() {
        when(feeService.getFeeById("fee-123")).thenReturn(fee);
        when(feePaymentRepository.existsByReceiptNo(anyString())).thenReturn(false);
        when(feePaymentRepository.findByFeeDocsId("fee-123"))
                .thenReturn(List.of(FeePayment.builder().amount(new BigDecimal("500.00")).build()));
        stubApplyPaidAmount();

        FeeInvoice result = feePaymentService.recordPayment(
                "fee-123", new BigDecimal("500.00"), PaymentMode.CASH, "full", "admin");

        assertNotNull(result);
        assertEquals(new BigDecimal("500.00"), result.getPaidAmount());
        assertEquals(FeeStatus.PAID, result.getStatus());
        verify(feePaymentRepository, times(1)).save(any(FeePayment.class));
        verify(feeService, times(1)).applyPaidAmount(fee, new BigDecimal("500.00"));
    }

    @Test
    void recordPayment_ExceedsBalance_ThrowsException() {
        when(feeService.getFeeById("fee-123")).thenReturn(fee);

        assertThrows(IllegalArgumentException.class, () -> {
            feePaymentService.recordPayment(
                    "fee-123", new BigDecimal("600.00"), PaymentMode.CASH, null, "admin");
        });

        verify(feePaymentRepository, never()).save(any());
        verify(feeService, never()).applyPaidAmount(any(), any());
    }

    @Test
    void recordPayment_NonPositiveAmount_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            feePaymentService.recordPayment(
                    "fee-123", BigDecimal.ZERO, PaymentMode.CASH, null, "admin");
        });

        verifyNoInteractions(feeService);
        verifyNoInteractions(feePaymentRepository);
        verifyNoInteractions(studentWalletService);
    }

    @Test
    void recordPayment_NullMode_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            feePaymentService.recordPayment(
                    "fee-123", new BigDecimal("100.00"), null, null, "admin");
        });

        verifyNoInteractions(feeService);
        verifyNoInteractions(feePaymentRepository);
    }

    @Test
    void recordPayment_AlreadyPaid_ThrowsException() {
        fee.setStatus(FeeStatus.PAID);
        when(feeService.getFeeById("fee-123")).thenReturn(fee);

        assertThrows(IllegalArgumentException.class, () -> {
            feePaymentService.recordPayment(
                    "fee-123", new BigDecimal("100.00"), PaymentMode.CASH, null, "admin");
        });

        verify(feePaymentRepository, never()).save(any());
        verify(feeService, never()).applyPaidAmount(any(), any());
    }

    @Test
    void recordPayment_ViaWallet_Success() {
        when(feeService.getFeeById("fee-123")).thenReturn(fee);
        when(feePaymentRepository.existsByReceiptNo(anyString())).thenReturn(false);
        when(feePaymentRepository.findByFeeDocsId("fee-123"))
                .thenReturn(List.of(FeePayment.builder().amount(new BigDecimal("300.00")).build()));
        
        com.orbitastra.backend.models.old.finance.WalletTransaction txn = 
                com.orbitastra.backend.models.old.finance.WalletTransaction.builder()
                        .referenceNo("WTR/2026/07/2501")
                        .build();
        when(studentWalletService.debitWallet(anyString(), any(BigDecimal.class), anyString())).thenReturn(txn);
        
        stubApplyPaidAmount();

        FeeInvoice result = feePaymentService.recordPayment(
                "fee-123", new BigDecimal("300.00"), PaymentMode.WALLET, "wallet pay", "admin");

        assertNotNull(result);
        assertEquals(new BigDecimal("300.00"), result.getPaidAmount());
        assertEquals(FeeStatus.PARTIALLY_PAID, result.getStatus());
        verify(studentWalletService, times(1)).debitWallet(
                eq("student-123"),
                eq(new BigDecimal("300.00")),
                contains("invoice fee-123"));
        verify(feePaymentRepository, times(1)).save(argThat(p -> 
            p.isCollectedFromWallet() && 
            "WTR/2026/07/2501".equals(p.getWalletNo())
        ));
        verify(feeService, times(1)).applyPaidAmount(fee, new BigDecimal("300.00"));
    }

    @Test
    void getPaymentsByFee_ReturnsPayments() {
        List<FeePayment> payments = List.of(FeePayment.builder().feeDocsId("fee-123").build());
        when(feePaymentRepository.findByFeeDocsId("fee-123")).thenReturn(payments);

        assertEquals(payments, feePaymentService.getPaymentsByFee("fee-123"));
    }

    @Test
    void getPaymentsByStudent_ReturnsPayments() {
        List<FeePayment> payments = List.of(FeePayment.builder().studentDocsId("student-123").build());
        when(feePaymentRepository.findByStudentDocsIdOrderByPaidOnDesc("student-123")).thenReturn(payments);

        assertEquals(payments, feePaymentService.getPaymentsByStudent("student-123"));
    }

    @Test
    void getPaymentByReceiptNo_ExistingPayment_Success() {
        FeePayment payment = FeePayment.builder()
                .id("pay-123")
                .receiptNo("RPN/2026/07/2501")
                .build();
        when(feePaymentRepository.findByReceiptNo("RPN/2026/07/2501")).thenReturn(java.util.Optional.of(payment));

        FeePayment result = feePaymentService.getPaymentByReceiptNo("RPN/2026/07/2501");

        assertNotNull(result);
        assertEquals("pay-123", result.getId());
        assertEquals("RPN/2026/07/2501", result.getReceiptNo());
        verify(feePaymentRepository, times(1)).findByReceiptNo("RPN/2026/07/2501");
    }

    @Test
    void getPaymentByReceiptNo_WithLeadingSlash_Success() {
        FeePayment payment = FeePayment.builder()
                .id("pay-123")
                .receiptNo("RPN/2026/07/2501")
                .build();
        when(feePaymentRepository.findByReceiptNo("RPN/2026/07/2501")).thenReturn(java.util.Optional.of(payment));

        FeePayment result = feePaymentService.getPaymentByReceiptNo("/RPN/2026/07/2501");

        assertNotNull(result);
        assertEquals("pay-123", result.getId());
        assertEquals("RPN/2026/07/2501", result.getReceiptNo());
        verify(feePaymentRepository, times(1)).findByReceiptNo("RPN/2026/07/2501");
    }

    @Test
    void getPaymentByReceiptNo_PaymentNotFound_ThrowsException() {
        when(feePaymentRepository.findByReceiptNo("RPN/2026/07/2501")).thenReturn(java.util.Optional.empty());

        assertThrows(com.orbitastra.backend.old.exceptions.ResourceNotFoundException.class, () -> {
            feePaymentService.getPaymentByReceiptNo("RPN/2026/07/2501");
        });
    }
}
