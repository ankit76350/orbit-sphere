package com.orbitastra.backend.models.finance;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import com.orbitastra.backend.models.base.SchoolBase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "student_wallets")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StudentWallet extends SchoolBase {
    @Indexed(unique = true)
    private String studentDocsId; // unique in whole DB.
    @Indexed(unique = true, sparse = true)
    private String walletNo; // WLT/2026/05/2578 : -> WLT/YYYY/MM/DDSS : unique in whole DB.
    private BigDecimal balance;
}
