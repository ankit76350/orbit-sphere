package com.orbitastra.backend.models.undone.mess;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.DayOfWeek;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The boarding mess menu for one day of the week. Allergy warnings are derived at
 * read time by cross-referencing student allergy data, not stored here.
 */
@Document(collection = "mess_menus")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MessMenu extends SchoolBase {

    @Indexed
    private LocalDate menuDate;

    @Indexed
    private String mealTypeDocsId;

    @Indexed
    private String messHallDocsId;

    /**
     * Example:
     * Rice
     * Dal Fry
     * Paneer Butter Masala
     * Salad
     */
    private List<String> menuItems;

    @Builder.Default
    private Boolean vegetarian = true;

    private String remarks;
}
