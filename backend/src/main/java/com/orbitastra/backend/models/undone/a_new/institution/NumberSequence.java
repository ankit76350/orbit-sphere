package com.orbitastra.backend.models.undone.a_new.institution;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.TenantScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Atomic business-number sequence. Allocation must use Mongo findAndModify with
 * increment and majority write concern; random suffix generation is not safe.
 */
@Document(collection = "number_sequences")
@CompoundIndex(name = "tenant_sequence_scope_uniq",
        def = "{'tenantId':1,'sequenceKey':1,'scopeKey':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class NumberSequence extends TenantScopedDocument {

    private String sequenceKey;
    private String scopeKey;
    private String prefixTemplate;
    private String suffixTemplate;
    private Long nextValue;
    private Integer paddingWidth;
    private Long allocationSize;
}
