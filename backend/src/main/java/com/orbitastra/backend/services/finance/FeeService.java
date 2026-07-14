package com.orbitastra.backend.services.finance;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.finance.FeeInvoice;
import com.orbitastra.backend.models.finance.FeePayment;
import com.orbitastra.backend.models.finance.enums.FeeStatus;
import com.orbitastra.backend.models.finance.enums.PaymentMode;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.repositories.finance.FeePaymentRepository;
import com.orbitastra.backend.repositories.finance.FeeRepository;
import com.orbitastra.backend.repositories.student.StudentRepository;
import com.orbitastra.backend.services.core.AcademicYearResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeeService {

    private final FeeRepository feeRepository;
    private final FeePaymentRepository feePaymentRepository;
    private final StudentRepository studentRepository;
    private final StudentWalletService studentWalletService;
    private final AcademicYearResolver academicYearResolver;

    public FeeInvoice createFee(FeeInvoice fee) {
        Student student = studentRepository.findById(fee.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + fee.getStudentId()));

        fee.setSchoolId(student.getSchoolId());
        fee.setAcademicYear(academicYearResolver
                .resolve(fee.getSchoolId(), fee.getAcademicYear(), fee.getDueDate())
                .getName());
        if (fee.getPaidAmount() == null) {
            fee.setPaidAmount(BigDecimal.ZERO);
        }
        if (fee.getStatus() == null) {
            fee.setStatus(FeeStatus.UNPAID);
        }
        fee.setCreatedAt(LocalDateTime.now());
        fee.setUpdatedAt(LocalDateTime.now());

        return feeRepository.save(fee);
    }

    public FeeInvoice getFeeById(String id) {
        return feeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fee not found with id: " + id));
    }

    public List<FeeInvoice> getFeesByStudent(String studentId) {
        return feeRepository.findByStudentId(studentId);
    }

    public List<FeeInvoice> getFeesBySchool(String schoolId) {
        return feeRepository.findBySchoolId(schoolId);
    }

    public List<FeeInvoice> getFeesBySchoolAndAcademicYear(String schoolId, String academicYear) {
        return feeRepository.findBySchoolIdAndAcademicYear(schoolId, academicYear);
    }

    public FeeInvoice updateFee(String id, FeeInvoice feeDetails) {
        FeeInvoice fee = getFeeById(id);
        academicYearResolver.assertImmutable(fee.getAcademicYear(), feeDetails.getAcademicYear());

        if (feeDetails.getType() != null) {
            fee.setType(feeDetails.getType());
        }
        if (feeDetails.getAmount() != null) {
            fee.setAmount(feeDetails.getAmount());
        }
        if (feeDetails.getDueDate() != null) {
            fee.setDueDate(feeDetails.getDueDate());
        }

        // paidAmount & status are derived from FeePayment records — never set directly.
        // Recalculate the cached status in case the invoice amount changed.
        recalculateStatus(fee);

        fee.setUpdatedAt(LocalDateTime.now());
        return feeRepository.save(fee);
    }

    /**
     * Collect a payment against an invoice. This is the single entry point for all
     * payment modes (cash, wallet, online, cheque). Every call produces a FeePayment
     * (receipt) as the audit trail; the invoice's paidAmount/status are then recomputed
     * from the sum of all its payments — the payments are the source of truth.
     */
    @Transactional
    public FeeInvoice recordPayment(String feeId, BigDecimal amount, PaymentMode mode,
                                    String remarks, String collectedBy) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }
        if (mode == null) {
            throw new IllegalArgumentException("Payment mode is required.");
        }
        FeeInvoice fee = getFeeById(feeId);
        if (fee.getStatus() == FeeStatus.PAID) {
            throw new IllegalArgumentException("Invoice is already fully paid.");
        }

        BigDecimal alreadyPaid = fee.getPaidAmount() != null ? fee.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal newPaidAmount = alreadyPaid.add(amount);
        if (newPaidAmount.compareTo(fee.getAmount()) > 0) {
            throw new IllegalArgumentException("Payment amount exceeds remaining invoice balance.");
        }

        // Wallet mode debits the student's wallet, which records its own WalletTransaction.
        if (mode == PaymentMode.WALLET) {
            studentWalletService.debitWallet(
                    fee.getStudentId(),
                    amount,
                    "Fee payment for invoice " + fee.getId() + " (" + fee.getType() + ")");
        }

        // Persist the collection record (receipt) — the audit trail for every mode.
        FeePayment payment = FeePayment.builder()
                .schoolId(fee.getSchoolId())
                .academicYear(fee.getAcademicYear())
                .studentId(fee.getStudentId())
                .feeId(fee.getId())
                .receiptNo("RCPT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .amount(amount)
                .paymentMode(mode)
                .paidOn(LocalDateTime.now())
                .collectedBy(collectedBy)
                .remarks(remarks)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        feePaymentRepository.save(payment);

        // Recompute the cached paidAmount from the source-of-truth payment records.
        BigDecimal totalPaid = feePaymentRepository.findByFeeId(fee.getId()).stream()
                .map(FeePayment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        fee.setPaidAmount(totalPaid);
        recalculateStatus(fee);
        fee.setUpdatedAt(LocalDateTime.now());

        return feeRepository.save(fee);
    }

    public List<FeePayment> getPaymentsByFee(String feeId) {
        return feePaymentRepository.findByFeeId(feeId);
    }

    public List<FeePayment> getPaymentsByStudent(String studentId) {
        return feePaymentRepository.findByStudentIdOrderByPaidOnDesc(studentId);
    }

    public void deleteFee(String id) {
        FeeInvoice fee = getFeeById(id);
        feeRepository.delete(fee);
    }

    private void recalculateStatus(FeeInvoice fee) {
        if (fee.getPaidAmount().compareTo(BigDecimal.ZERO) == 0) {
            fee.setStatus(FeeStatus.UNPAID);
        } else if (fee.getPaidAmount().compareTo(fee.getAmount()) >= 0) {
            fee.setStatus(FeeStatus.PAID);
        } else {
            fee.setStatus(FeeStatus.PARTIALLY_PAID);
        }
    }
}
