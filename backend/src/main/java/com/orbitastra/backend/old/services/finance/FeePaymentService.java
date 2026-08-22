package com.orbitastra.backend.old.services.finance;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orbitastra.backend.models.old.finance.FeeInvoice;
import com.orbitastra.backend.models.old.finance.FeePayment;
import com.orbitastra.backend.models.old.finance.enums.FeeStatus;
import com.orbitastra.backend.models.old.finance.enums.PaymentMode;
import com.orbitastra.backend.old.repositories.finance.FeePaymentRepository;
import com.orbitastra.backend.old.services.utils.GenerateUniqueId;

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
        String walletTxnReferenceNo = null;
        if (mode == PaymentMode.WALLET) {
            com.orbitastra.backend.models.old.finance.WalletTransaction txn = studentWalletService.debitWallet(
                    fee.getStudentDocsId(),
                    amount,
                    "Fee payment for invoice " + fee.getId() + " (" + fee.getType() + ")");
            if (txn != null) {
                walletTxnReferenceNo = txn.getReferenceNo();
            }
        }

        // Persist the collection record (receipt) — the audit trail for every mode.
        FeePayment payment = FeePayment.builder()
                .schoolId(fee.getSchoolId())
                .academicYear(fee.getAcademicYear())
                .studentDocsId(fee.getStudentDocsId())
                .feeDocsId(fee.getId())
                .receiptNo(GenerateUniqueId.generate("RPN", feePaymentRepository::existsByReceiptNo))
                .collectedFromWallet(mode == PaymentMode.WALLET)
                .walletNo(walletTxnReferenceNo)
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

    public FeePayment getPaymentByReceiptNo(String receiptNo) {
        String cleanReceiptNo = receiptNo != null && receiptNo.startsWith("/") ? receiptNo.substring(1) : receiptNo;
        return feePaymentRepository.findByReceiptNo(cleanReceiptNo)
                .orElseThrow(() -> new com.orbitastra.backend.old.exceptions.ResourceNotFoundException("Fee payment not found with receiptNo: " + cleanReceiptNo));
    }
}
