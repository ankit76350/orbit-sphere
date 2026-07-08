package com.orbitastra.backend.services.finance;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.finance.Fee;
import com.orbitastra.backend.models.finance.enums.FeeStatus;
import com.orbitastra.backend.models.student.Student;
import com.orbitastra.backend.repositories.finance.FeeRepository;
import com.orbitastra.backend.repositories.student.StudentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeeService {

    private final FeeRepository feeRepository;
    private final StudentRepository studentRepository;
    private final StudentWalletService studentWalletService;

    public Fee createFee(Fee fee) {
        Student student = studentRepository.findById(fee.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + fee.getStudentId()));

        fee.setSchoolId(student.getSchoolId());
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

    public Fee getFeeById(String id) {
        return feeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fee not found with id: " + id));
    }

    public List<Fee> getFeesByStudent(String studentId) {
        return feeRepository.findByStudentId(studentId);
    }

    public List<Fee> getFeesBySchool(String schoolId) {
        return feeRepository.findBySchoolId(schoolId);
    }

    public Fee updateFee(String id, Fee feeDetails) {
        Fee fee = getFeeById(id);

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
    public Fee payFee(String id, BigDecimal payAmount) {
        if (payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }
        Fee fee = getFeeById(id);
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
    public Fee payFeeViaWallet(String id, BigDecimal payAmount) {
        Fee fee = getFeeById(id);
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
        Fee fee = getFeeById(id);
        feeRepository.delete(fee);
    }

    private void recalculateStatus(Fee fee) {
        if (fee.getPaidAmount().compareTo(BigDecimal.ZERO) == 0) {
            fee.setStatus(FeeStatus.UNPAID);
        } else if (fee.getPaidAmount().compareTo(fee.getAmount()) >= 0) {
            fee.setStatus(FeeStatus.PAID);
        } else {
            fee.setStatus(FeeStatus.PARTIALLY_PAID);
        }
    }
}
