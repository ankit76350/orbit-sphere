package com.orbitastra.backend.services.finance;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.finance.FeeInvoice;
import com.orbitastra.backend.models.finance.enums.FeeStatus;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.repositories.finance.FeeRepository;
import com.orbitastra.backend.repositories.student.StudentRepository;
import com.orbitastra.backend.services.core.AcademicYearResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeeService {

    private final FeeRepository feeRepository;
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
        if (feeDetails.getPaidAmount() != null) {
            fee.setPaidAmount(feeDetails.getPaidAmount());
        }
        if (feeDetails.getDueDate() != null) {
            fee.setDueDate(feeDetails.getDueDate());
        }
        if (feeDetails.getStatus() != null) {
            fee.setStatus(feeDetails.getStatus());
        } else {
            // Recalculate status based on amount & paid amount
            recalculateStatus(fee);
        }

        fee.setUpdatedAt(LocalDateTime.now());
        return feeRepository.save(fee);
    }

    @Transactional
    public FeeInvoice payFee(String id, BigDecimal payAmount) {
        if (payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }
        FeeInvoice fee = getFeeById(id);
        if (fee.getStatus() == FeeStatus.PAID) {
            throw new IllegalArgumentException("Fee is already fully paid.");
        }

        BigDecimal newPaidAmount = fee.getPaidAmount().add(payAmount);
        if (newPaidAmount.compareTo(fee.getAmount()) > 0) {
            throw new IllegalArgumentException("Payment amount exceeds remaining fee balance.");
        }

        fee.setPaidAmount(newPaidAmount);
        recalculateStatus(fee);
        fee.setUpdatedAt(LocalDateTime.now());

        return feeRepository.save(fee);
    }

    @Transactional
    public FeeInvoice payFeeViaWallet(String id, BigDecimal payAmount) {
        FeeInvoice fee = getFeeById(id);
        if (payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }
        if (fee.getStatus() == FeeStatus.PAID) {
            throw new IllegalArgumentException("Fee is already fully paid.");
        }

        BigDecimal newPaidAmount = fee.getPaidAmount().add(payAmount);
        if (newPaidAmount.compareTo(fee.getAmount()) > 0) {
            throw new IllegalArgumentException("Payment amount exceeds remaining fee balance.");
        }

        // Debit the wallet balance
        studentWalletService.debitWallet(
                fee.getStudentId(), 
                payAmount, 
                "Payment for Fee ID: " + fee.getId() + " (Type: " + fee.getType() + ")"
        );

        fee.setPaidAmount(newPaidAmount);
        recalculateStatus(fee);
        fee.setUpdatedAt(LocalDateTime.now());

        return feeRepository.save(fee);
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
