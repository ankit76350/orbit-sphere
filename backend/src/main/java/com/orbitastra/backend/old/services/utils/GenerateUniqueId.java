package com.orbitastra.backend.old.services.utils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.function.Predicate;

/**
 * Utility for generating unique human-readable IDs in the format:
 *   PREFIX/YYYY/MM/DDSS  (e.g. INV/2026/07/2507)
 *
 * Pass the repository's existsBy… method reference as the uniqueness check.
 * The generator retries internally until a collision-free ID is produced.
 *
 * Usage:
 *   GenerateUniqueId.generate("INV", feeRepository::existsByInvoiceNo)
 */
public class GenerateUniqueId {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates a unique ID with the given prefix.
     *
     * @param prefix      e.g. "INV", "RPN", "WLT", "WTR"
     * @param existsCheck repository method that returns true if the candidate already exists
     * @return a guaranteed-unique ID string
     */
    public static String generate(String prefix, Predicate<String> existsCheck) {
        LocalDateTime now = LocalDateTime.now();
        String id;
        do {
            int suffix = RANDOM.nextInt(100); // 00–99
            id = String.format("%s/%d/%02d/%02d%02d",
                    prefix,
                    now.getYear(),
                    now.getMonthValue(),
                    now.getDayOfMonth(),
                    suffix);
        } while (existsCheck.test(id));
        return id;
    }

    private GenerateUniqueId() {
        // utility class — no instances
    }
}