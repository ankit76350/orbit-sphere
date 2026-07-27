package com.orbitastra.backend.models.undone.mess;

@Document(collection = "mess_attendance")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MessAttendance extends SchoolBase {

    @Indexed
    private LocalDate attendanceDate;

    @Indexed
    private String mealTypeDocsId;

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
