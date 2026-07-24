package com.orbitastra.backend.services.finance;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.finance.FeeInvoice;
import com.orbitastra.backend.models.finance.enums.FeeStatus;
import com.orbitastra.backend.repositories.finance.FeeRepository;
import com.orbitastra.backend.services.utils.AcademicYearResolver;
import com.orbitastra.backend.services.utils.StudentValidator;

import lombok.RequiredArgsConstructor;

/**
 * Owns the fee invoice lifecycle (create / read / update / delete). Collecting
 * payments against an invoice lives in {@link FeePaymentService}; the cached
 * paidAmount/status fields here are derived from those payment records and are
 * updated through {@link #applyPaidAmount(FeeInvoice, BigDecimal)}.
 */
@Service
@RequiredArgsConstructor
public class FeeService {

    private final FeeRepository feeRepository;
    private final StudentValidator studentValidator;
    private final AcademicYearResolver academicYearResolver;

    public FeeInvoice createFee(FeeInvoice fee) {
        studentValidator.validateStudent(fee.getStudentDocsId(), fee.getSchoolId());

        fee.setAcademicYear(academicYearResolver
                .resolve(fee.getSchoolId(), fee.getAcademicYear(), fee.getDueDate())
                .getName());
        if (fee.getDiscount() == null) {
            fee.setDiscount(BigDecimal.ZERO);
        }
        if (fee.getPaidAmount() == null) {
            fee.setPaidAmount(BigDecimal.ZERO);
        }
        if (fee.getInvoiceNo() == null || fee.getInvoiceNo().isBlank()) {
            fee.setInvoiceNo("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
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

    public List<FeeInvoice> getFeesByStudent(String studentDocsId) {
        return feeRepository.findByStudentDocsId(studentDocsId);
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
        if (feeDetails.getDiscount() != null) {
            fee.setDiscount(feeDetails.getDiscount());
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

    public void deleteFee(String id) {
        FeeInvoice fee = getFeeById(id);
        feeRepository.delete(fee);
    }

    /**
     * Sets the invoice's cached paidAmount to the given total (the sum of its
     * payment records), recomputes the status and persists it. Called by
     * {@link FeePaymentService} whenever a payment is collected — the payment
     * records remain the source of truth.
     */
    public FeeInvoice applyPaidAmount(FeeInvoice fee, BigDecimal totalPaid) {
        fee.setPaidAmount(totalPaid != null ? totalPaid : BigDecimal.ZERO);
        recalculateStatus(fee);
        fee.setUpdatedAt(LocalDateTime.now());
        return feeRepository.save(fee);
    }

    /** Net amount the student actually owes after any concession: amount - discount (never negative). */
    public BigDecimal netPayable(FeeInvoice fee) {
        BigDecimal amount = fee.getAmount() != null ? fee.getAmount() : BigDecimal.ZERO;
        BigDecimal discount = fee.getDiscount() != null ? fee.getDiscount() : BigDecimal.ZERO;
        BigDecimal net = amount.subtract(discount);
        return net.compareTo(BigDecimal.ZERO) > 0 ? net : BigDecimal.ZERO;
    }

    /** Outstanding balance still to be collected: netPayable - paidAmount (never negative). */
    public BigDecimal remainingBalance(FeeInvoice fee) {
        BigDecimal paid = fee.getPaidAmount() != null ? fee.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal remaining = netPayable(fee).subtract(paid);
        return remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO;
    }

    private void recalculateStatus(FeeInvoice fee) {
        BigDecimal paid = fee.getPaidAmount() != null ? fee.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal net = netPayable(fee);
        // paid >= net covers the fully-waived case (net == 0) as PAID too.
        if (paid.compareTo(net) >= 0) {
            fee.setStatus(FeeStatus.PAID);
        } else if (paid.compareTo(BigDecimal.ZERO) == 0) {
            fee.setStatus(FeeStatus.UNPAID);
        } else {
            fee.setStatus(FeeStatus.PARTIALLY_PAID);
        }
    }
}
