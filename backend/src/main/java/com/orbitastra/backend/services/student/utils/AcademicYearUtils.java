package com.orbitastra.backend.services.student.utils;

import java.time.LocalDate;

public class AcademicYearUtils {

    // The month index when the academic year starts (e.g. June = 6)
    private static final int STARTING_MONTH = 6;

    /**
     * Dynamically calculates the current academic year in "YYYY-YYYY" format.
     */
    public static String getCurrentAcademicYear() {
        return getAcademicYearForDate(LocalDate.now());
    }

    /**
     * Calculates the academic year for a specific date in "YYYY-YYYY" format.
     * If the month of the date is before the STARTING_MONTH, it belongs to the previous year's cohort.
     */
    public static String getAcademicYearForDate(LocalDate date) {
        if (date == null) {
            return getAcademicYearForDate(LocalDate.now());
        }
        int year = date.getYear();
        int month = date.getMonthValue();
        
        if (month < STARTING_MONTH) {
            return (year - 1) + "-" + year;
        } else {
            return year + "-" + (year + 1);
        }
    }
}
