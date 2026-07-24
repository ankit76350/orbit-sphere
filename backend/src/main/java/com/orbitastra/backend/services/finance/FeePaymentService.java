package com.orbitastra.backend.services.finance;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.models.finance.FeeInvoice;
import com.orbitastra.backend.models.finance.FeePayment;
import com.orbitastra.backend.models.finance.enums.FeeStatus;
import com.orbitastra.backend.models.finance.enums.PaymentMode;
import com.orbitastra.backend.repositories.finance.FeePaymentRepository;

import lombok.RequiredArgsConstructor;

/**
 * Collects payments against fee invoices. This is the single entry point for
 * all payment modes (cash, wallet, online, cheque). Every collection produces
 * a {@link FeePayment} (receipt) as the audit trail; the invoice's cached
 * paidAmount/status are then recomputed from the sum of all its payments —
 * the payment records are the source of truth. Invoice CRUD lives in
 * {@link FeeService}.
 */
@Service
@RequiredArgsConstructor
public class FeePaymentService {

    private final FeePaymentRepository feePaymentRepository;
    private final FeeService feeService;
    private final StudentWalletService studentWalletService;

    @Transactional
    public FeeInvoice recordPayment(String feeDocsId, BigDecimal amount, PaymentMode mode,
                                    String remarks, String collectedByDocsId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }
        if (mode == null) {
            throw new IllegalArgumentException("Payment mode is required.");
        }
        FeeInvoice fee = feeService.getFeeById(feeDocsId);
        if (fee.getStatus() == FeeStatus.PAID) {
            throw new IllegalArgumentException("Invoice is already fully paid.");
        }

        // Balance is measured against the net payable (amount - discount), owned by FeeService.
        if (amount.compareTo(feeService.remainingBalance(fee)) > 0) {
            throw new IllegalArgumentException("Payment amount exceeds remaining invoice balance.");
        }

        // Wallet mode debits the student's wallet, which records its own WalletTransaction.
        if (mode == PaymentMode.WALLET) {
            studentWalletService.debitWallet(
                    fee.getStudentDocsId(),
                    amount,
                    "Fee payment for invoice " + fee.getId() + " (" + fee.getType() + ")");
        }

        // Persist the collection record (receipt) — the audit trail for every mode.
        FeePayment payment = FeePayment.builder()
                .schoolId(fee.getSchoolId())
                .academicYear(fee.getAcademicYear())
                .studentDocsId(fee.getStudentDocsId())
                .feeDocsId(fee.getId())
                .receiptNo("RCPT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .amount(amount)
                .paymentMode(mode)
                .paidOn(LocalDateTime.now())
                .collectedByDocsId(collectedByDocsId)
                .remarks(remarks)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        feePaymentRepository.save(payment);

        // Recompute the cached paidAmount from the source-of-truth payment records.
        BigDecimal totalPaid = feePaymentRepository.findByFeeDocsId(fee.getId()).stream()
                .map(FeePayment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return feeService.applyPaidAmount(fee, totalPaid);
    }

    public List<FeePayment> getPaymentsByFee(String feeDocsId) {
        return feePaymentRepository.findByFeeDocsId(feeDocsId);
    }

    public List<FeePayment> getPaymentsByStudent(String studentDocsId) {
        return feePaymentRepository.findByStudentDocsIdOrderByPaidOnDesc(studentDocsId);
    }
}
