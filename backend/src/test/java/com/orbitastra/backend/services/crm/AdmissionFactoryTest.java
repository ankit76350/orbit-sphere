package com.orbitastra.backend.services.crm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class AdmissionFactoryTest {

    @Test
    void generateAdmissionNo_usesYearMonthDayAndSecond() {
        LocalDateTime dateTime = LocalDateTime.of(2026, 5, 5, 10, 30, 19);

        assertEquals("ADM/2026/05/0519", AdmissionFactory.generateAdmissionNo(dateTime));
    }
}
