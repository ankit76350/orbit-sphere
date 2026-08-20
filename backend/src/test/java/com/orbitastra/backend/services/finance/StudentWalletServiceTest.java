package com.orbitastra.backend.services.finance;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.old.finance.StudentWallet;
import com.orbitastra.backend.models.old.finance.WalletTransaction;
import com.orbitastra.backend.models.old.finance.enums.TransactionType;
import com.orbitastra.backend.models.old.student.Student;
import com.orbitastra.backend.repositories.finance.StudentWalletRepository;
import com.orbitastra.backend.repositories.finance.WalletTransactionRepository;
import com.orbitastra.backend.repositories.student.StudentRepository;

@ExtendWith(MockitoExtension.class)
public class StudentWalletServiceTest {

    @Mock
    private StudentWalletRepository studentWalletRepository;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentWalletService studentWalletService;

    private Student student;
    private StudentWallet wallet;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId("student-123");
        student.setName("John Doe");
        student.setSchoolId("school-123");

        wallet = new StudentWallet();
        wallet.setId("wallet-123");
        wallet.setStudentDocsId("student-123");
        wallet.setSchoolId("school-123");
        wallet.setBalance(new BigDecimal("100.00"));
    }

    @Test
    void getWalletByStudentDocsId_ExistingWallet_Success() {
        when(studentWalletRepository.findByStudentDocsId("student-123")).thenReturn(Optional.of(wallet));

        StudentWallet result = studentWalletService.getWalletByStudentDocsId("student-123");

        assertNotNull(result);
        assertEquals("wallet-123", result.getId());
        assertEquals(new BigDecimal("100.00"), result.getBalance());
        verify(studentWalletRepository, times(1)).findByStudentDocsId("student-123");
        verifyNoInteractions(studentRepository);
    }

    @Test
    void getWalletByStudentDocsId_WalletNotFound_ThrowsException() {
        when(studentWalletRepository.findByStudentDocsId("student-123")).thenReturn(Optional.empty());
        when(studentRepository.findById("student-123")).thenReturn(Optional.of(student));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> {
            studentWalletService.getWalletByStudentDocsId("student-123");
        });
        assertEquals("Wallet not found for student John Doe, want to open a wallet account?", ex.getMessage());
    }

    @Test
    void getWalletByStudentDocsId_StudentNotFound_ThrowsException() {
        when(studentWalletRepository.findByStudentDocsId("student-123")).thenReturn(Optional.empty());
        when(studentRepository.findById("student-123")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> {
            studentWalletService.getWalletByStudentDocsId("student-123");
        });
        assertEquals("Student not found with id: student-123", ex.getMessage());
    }

    @Test
    void creditWallet_Success() {
        when(studentWalletRepository.findByStudentDocsId("student-123")).thenReturn(Optional.of(wallet));
        when(studentWalletRepository.save(any(StudentWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletTransactionRepository.existsByReferenceNo(anyString())).thenReturn(false);
        when(walletTransactionRepository.save(any(WalletTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletTransaction result = studentWalletService.creditWallet("student-123", new BigDecimal("50.00"), "Deposit pocket money");

        assertNotNull(result);
        assertEquals(new BigDecimal("150.00"), result.getBalanceAfter());
        verify(studentWalletRepository, times(1)).save(any(StudentWallet.class));
        verify(walletTransactionRepository, times(1)).save(argThat(tx -> 
            tx.getType() == TransactionType.CREDIT && 
            tx.getAmount().equals(new BigDecimal("50.00")) &&
            tx.getBalanceAfter().equals(new BigDecimal("150.00")) &&
            tx.getRemarks().equals("Deposit pocket money")
        ));
    }

    @Test
    void creditWallet_InvalidAmount_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            studentWalletService.creditWallet("student-123", BigDecimal.ZERO, "Invalid credit");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            studentWalletService.creditWallet("student-123", new BigDecimal("-10.00"), "Invalid credit");
        });
    }

    @Test
    void creditWallet_WalletNotFound_ThrowsException() {
        when(studentWalletRepository.findByStudentDocsId("student-123")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            studentWalletService.creditWallet("student-123", new BigDecimal("50.00"), "Credit test");
        });
    }

    @Test
    void debitWallet_Success() {
        when(studentWalletRepository.findByStudentDocsId("student-123")).thenReturn(Optional.of(wallet));
        when(studentWalletRepository.save(any(StudentWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletTransactionRepository.existsByReferenceNo(anyString())).thenReturn(false);
        when(walletTransactionRepository.save(any(WalletTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WalletTransaction result = studentWalletService.debitWallet("student-123", new BigDecimal("30.00"), "Buy stationary");

        assertNotNull(result);
        assertEquals(new BigDecimal("70.00"), result.getBalanceAfter());
        verify(studentWalletRepository, times(1)).save(any(StudentWallet.class));
        verify(walletTransactionRepository, times(1)).save(argThat(tx -> 
            tx.getType() == TransactionType.DEBIT && 
            tx.getAmount().equals(new BigDecimal("30.00")) &&
            tx.getBalanceAfter().equals(new BigDecimal("70.00")) &&
            tx.getRemarks().equals("Buy stationary")
        ));
    }

    @Test
    void debitWallet_InsufficientBalance_ThrowsException() {
        when(studentWalletRepository.findByStudentDocsId("student-123")).thenReturn(Optional.of(wallet));

        assertThrows(IllegalArgumentException.class, () -> {
            studentWalletService.debitWallet("student-123", new BigDecimal("200.00"), "Overdraft");
        });
    }

    @Test
    void debitWallet_InvalidAmount_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            studentWalletService.debitWallet("student-123", BigDecimal.ZERO, "Invalid debit");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            studentWalletService.debitWallet("student-123", new BigDecimal("-5.00"), "Invalid debit");
        });
    }

    @Test
    void debitWallet_WalletNotFound_ThrowsException() {
        when(studentWalletRepository.findByStudentDocsId("student-123")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            studentWalletService.debitWallet("student-123", new BigDecimal("30.00"), "Debit test");
        });
    }

    @Test
    void getWalletByWalletNo_ExistingWallet_Success() {
        wallet.setWalletNo("WLT/2026/07/2501");
        when(studentWalletRepository.findByWalletNo("WLT/2026/07/2501")).thenReturn(Optional.of(wallet));

        StudentWallet result = studentWalletService.getWalletByWalletNo("WLT/2026/07/2501");

        assertNotNull(result);
        assertEquals("wallet-123", result.getId());
        assertEquals("WLT/2026/07/2501", result.getWalletNo());
        verify(studentWalletRepository, times(1)).findByWalletNo("WLT/2026/07/2501");
    }

    @Test
    void getWalletByWalletNo_WithLeadingSlash_Success() {
        wallet.setWalletNo("WLT/2026/07/2501");
        when(studentWalletRepository.findByWalletNo("WLT/2026/07/2501")).thenReturn(Optional.of(wallet));

        StudentWallet result = studentWalletService.getWalletByWalletNo("/WLT/2026/07/2501");

        assertNotNull(result);
        assertEquals("wallet-123", result.getId());
        assertEquals("WLT/2026/07/2501", result.getWalletNo());
        verify(studentWalletRepository, times(1)).findByWalletNo("WLT/2026/07/2501");
    }

    @Test
    void getWalletByWalletNo_WalletNotFound_ThrowsException() {
        when(studentWalletRepository.findByWalletNo("WLT/2026/07/2501")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            studentWalletService.getWalletByWalletNo("WLT/2026/07/2501");
        });
    }

    @Test
    void getWalletTransactionByReferenceNo_ExistingTransaction_Success() {
        WalletTransaction transaction = WalletTransaction.builder()
                .id("txn-123")
                .referenceNo("WTR/2026/07/2501")
                .build();
        when(walletTransactionRepository.findByReferenceNo("WTR/2026/07/2501")).thenReturn(Optional.of(transaction));

        WalletTransaction result = studentWalletService.getWalletTransactionByReferenceNo("WTR/2026/07/2501");

        assertNotNull(result);
        assertEquals("txn-123", result.getId());
        assertEquals("WTR/2026/07/2501", result.getReferenceNo());
        verify(walletTransactionRepository, times(1)).findByReferenceNo("WTR/2026/07/2501");
    }

    @Test
    void getWalletTransactionByReferenceNo_WithLeadingSlash_Success() {
        WalletTransaction transaction = WalletTransaction.builder()
                .id("txn-123")
                .referenceNo("WTR/2026/07/2501")
                .build();
        when(walletTransactionRepository.findByReferenceNo("WTR/2026/07/2501")).thenReturn(Optional.of(transaction));

        WalletTransaction result = studentWalletService.getWalletTransactionByReferenceNo("/WTR/2026/07/2501");

        assertNotNull(result);
        assertEquals("txn-123", result.getId());
        assertEquals("WTR/2026/07/2501", result.getReferenceNo());
        verify(walletTransactionRepository, times(1)).findByReferenceNo("WTR/2026/07/2501");
    }

    @Test
    void getWalletTransactionByReferenceNo_TransactionNotFound_ThrowsException() {
        when(walletTransactionRepository.findByReferenceNo("WTR/2026/07/2501")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            studentWalletService.getWalletTransactionByReferenceNo("WTR/2026/07/2501");
        });
    }

    @Test
    void openWallet_Success() {
        when(studentRepository.findById("student-123")).thenReturn(Optional.of(student));
        when(studentWalletRepository.findByStudentDocsId("student-123")).thenReturn(Optional.empty());
        when(studentWalletRepository.existsByWalletNo(anyString())).thenReturn(false);
        when(studentWalletRepository.save(any(StudentWallet.class))).thenAnswer(invocation -> {
            StudentWallet w = invocation.getArgument(0);
            w.setId("wallet-new-123");
            return w;
        });
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentWallet result = studentWalletService.openWallet("student-123");

        assertNotNull(result);
        assertEquals(new BigDecimal("0.0"), result.getBalance());
        assertEquals("student-123", result.getStudentDocsId());
        assertEquals("school-123", result.getSchoolId());
        assertNotNull(result.getWalletNo());
        assertTrue(result.getWalletNo().startsWith("WLT/"));
        verify(studentRepository, times(1)).findById("student-123");
        verify(studentWalletRepository, times(1)).findByStudentDocsId("student-123");
        verify(studentWalletRepository, times(1)).save(any(StudentWallet.class));
        verify(studentRepository, times(1)).save(argThat(s -> "wallet-new-123".equals(s.getWalletDocsId())));
    }

    @Test
    void openWallet_StudentNotFound_ThrowsException() {
        when(studentRepository.findById("student-123")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            studentWalletService.openWallet("student-123");
        });
        verifyNoInteractions(studentWalletRepository);
    }

    @Test
    void openWallet_WalletAlreadyExists_ThrowsException() {
        when(studentRepository.findById("student-123")).thenReturn(Optional.of(student));
        when(studentWalletRepository.findByStudentDocsId("student-123")).thenReturn(Optional.of(wallet));

        assertThrows(IllegalArgumentException.class, () -> {
            studentWalletService.openWallet("student-123");
        });
        verify(studentRepository, times(1)).findById("student-123");
        verify(studentWalletRepository, times(1)).findByStudentDocsId("student-123");
        verify(studentWalletRepository, never()).save(any(StudentWallet.class));
    }
}
