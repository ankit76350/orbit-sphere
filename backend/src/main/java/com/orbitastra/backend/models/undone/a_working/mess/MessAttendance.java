package com.orbitastra.backend.models.undone.a_working.mess;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.undone.a_working.mess.enums.ConsumerType;
import com.orbitastra.backend.models.undone.a_working.mess.enums.MealAttendanceMode;
import com.orbitastra.backend.models.undone.a_working.mess.enums.MealAttendanceStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "mess_attendance")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MessAttendance extends SchoolBase {

    //  "attendanceDate": "2027-07-01",
    @Indexed
    private LocalDate attendanceDate;

    //  "mealTypeDocsId": "meal_breakfast",

    @Indexed
    private String mealTypeDocsId;

    // "messHallDocsId": "hall_main",
    @Indexed
    private String messHallDocsId;

    /**
     * STUDENT / STAFF
     */
    private ConsumerType consumerType;

    /**
     * Only one of these is filled.
     */
    private String studentDocsId;
    private String staffDocsId;



    @Builder.Default
    private MealAttendanceStatus status = MealAttendanceStatus.PRESENT;

    private LocalDateTime servedAt;

    private MealAttendanceMode attendanceMode;

    private String remarks;
}
