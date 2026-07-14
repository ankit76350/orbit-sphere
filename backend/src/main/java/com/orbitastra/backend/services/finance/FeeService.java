package com.orbitastra.backend.services.finance;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.finance.FeeInvoice;
import com.orbitastra.backend.models.finance.enums.FeeStatus;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.repositories.finance.FeeRepository;
import com.orbitastra.backend.repositories.student.StudentRepository;
import com.orbitastra.backend.services.core.AcademicYearResolver;

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
    private final StudentRepository studentRepository;
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
