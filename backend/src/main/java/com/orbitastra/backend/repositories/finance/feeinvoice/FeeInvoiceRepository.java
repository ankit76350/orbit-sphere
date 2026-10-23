package com.orbitastra.backend.repositories.finance.feeinvoice;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.orbitastra.backend.models.finance.billing.FeeInvoice;

/**
 * Reads of {@code fee_invoices}.
 *
 * <p><b>The first repository in {@code finance}, and it exists for one check in another module.</b>
 * #29 lets an offer name the deposit invoice a family has to settle, and an id nothing verifies is
 * an id that can be anything — {@code "13212313"} was stored happily until this existed.
 *
 * <p><b>Nothing writes this collection yet.</b> The finance module has models and no service, so
 * every lookup through here answers "not found" today. That is the honest answer rather than a
 * missing one: an offer pointing at an invoice that does not exist is a broken reference whether
 * the collection is empty because the module is unbuilt or because somebody typed a number.
 */
public interface FeeInvoiceRepository extends MongoRepository<FeeInvoice, String> {

    /**
     * One invoice, for #29's deposit check.
     *
     * <p><b>Scoped by {@code schoolId} in the query, never by id alone.</b> An id from another
     * school is a real id, and an offer naming it would tell this school's family to settle
     * another school's bill.
     */
    Optional<FeeInvoice> findByIdAndSchoolId(String id, String schoolId);
}
