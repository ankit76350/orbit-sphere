package com.orbitastra.backend.models.undone.mess;

import java.time.DayOfWeek;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The boarding mess menu for one day of the week. Allergy warnings are derived at
 * read time by cross-referencing student allergy data, not stored here.
 */
@Document(collection = "mess_menus")
@CompoundIndex(name = "school_day_uniq", def = "{'schoolId': 1, 'dayOfWeek': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessMenu {

    @CreatedDate
    private java.time.LocalDateTime createdAt;

    @LastModifiedDate
    private java.time.LocalDateTime updatedAt;

    @Id
    private String id;

    @Indexed
    private String schoolId;

    private DayOfWeek dayOfWeek;

    private String breakfast;

    private String lunch;

    private String dinner;
}
