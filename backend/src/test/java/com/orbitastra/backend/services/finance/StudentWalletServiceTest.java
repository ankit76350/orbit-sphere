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
import com.orbitastra.backend.models.finance.StudentWallet;
import com.orbitastra.backend.models.finance.WalletTransaction;
import com.orbitastra.backend.models.finance.enums.TransactionType;
import com.orbitastra.backend.models.student.Student;
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
        student.setSchoolId("school-123");

        wallet = new StudentWallet();
        wallet.setId("wallet-123");
        wallet.setStudentId("student-123");
        wallet.setSchoolId("school-123");
        wallet.setBalance(new BigDecimal("100.00"));
    }

    @Test
    void getWalletByStudentId_ExistingWallet_Success() {
        when(studentWalletRepository.findByStudentId("student-123")).thenReturn(Optional.of(wallet));

        StudentWallet result = studentWalletService.getWalletByStudentId("student-123");

        assertNotNull(result);
        assertEquals("wallet-123", result.getId());
        assertEquals(new BigDecimal("100.00"), result.getBalance());
        verify(studentWalletRepository, times(1)).findByStudentId("student-123");
        verifyNoInteractions(studentRepository);
    }

    @Test
    void getWalletByStudentId_NewWallet_Success() {
        when(studentWalletRepository.findByStudentId("student-123")).thenReturn(Optional.empty());
        when(studentRepository.findById("student-123")).thenReturn(Optional.of(student));
        when(studentWalletRepository.save(any(StudentWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentWallet result = studentWalletService.getWalletByStudentId("student-123");

        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getBalance());
        assertEquals("student-123", result.getStudentId());
        assertEquals("school-123", result.getSchoolId());
        verify(studentWalletRepository, times(1)).findByStudentId("student-123");
        verify(studentRepository, times(1)).findById("student-123");
        verify(studentWalletRepository, times(1)).save(any(StudentWallet.class));
    }

    @Test
    void getWalletByStudentId_StudentNotFound_ThrowsException() {
        when(studentWalletRepository.findByStudentId("student-123")).thenReturn(Optional.empty());
        when(studentRepository.findById("student-123")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            studentWalletService.getWalletByStudentId("student-123");
        });
    }

    @Test
    void creditWallet_Success() {
        when(studentWalletRepository.findByStudentId("student-123")).thenReturn(Optional.of(wallet));
        when(studentWalletRepository.save(any(StudentWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletTransactionRepository.save(any(WalletTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentWallet result = studentWalletService.creditWallet("student-123", new BigDecimal("50.00"), "Deposit pocket money");

        assertNotNull(result);
        assertEquals(new BigDecimal("150.00"), result.getBalance());
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
    void debitWallet_Success() {
        when(studentWalletRepository.findByStudentId("student-123")).thenReturn(Optional.of(wallet));
        when(studentWalletRepository.save(any(StudentWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletTransactionRepository.save(any(WalletTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudentWallet result = studentWalletService.debitWallet("student-123", new BigDecimal("30.00"), "Buy stationary");

        assertNotNull(result);
        assertEquals(new BigDecimal("70.00"), result.getBalance());
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
        when(studentWalletRepository.findByStudentId("student-123")).thenReturn(Optional.of(wallet));

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
}
