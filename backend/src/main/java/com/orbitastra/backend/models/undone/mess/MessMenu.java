package com.orbitastra.backend.models.undone.mess;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.DayOfWeek;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolDocs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The boarding mess menu for one day of the week. Allergy warnings are derived at
 * read time by cross-referencing student allergy data, not stored here.
 */
@Document(collection = "mess_menus")
@CompoundIndex(name = "school_day_uniq", def = "{'schoolId': 1, 'dayOfWeek': 1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MessMenu extends SchoolDocs {

    private DayOfWeek dayOfWeek;

    private String breakfast;

    private String lunch;

    private String dinner;
}
