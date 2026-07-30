package com.orbitastra.backend.models.undone.a_working.mess;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The boarding mess menu for one day of the week. Allergy warnings are derived
 * at
 * read time by cross-referencing student allergy data, not stored here.
 */
@Document(collection = "mess_menus")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MessMenu extends SchoolBase {

    // "menuDate": "2027-07-02",
    @Indexed
    private LocalDate menuDate;

    // "mealTypeDocsId": "meal_lunch",
    @Indexed
    private String mealTypeDocsId;

    // "messHallDocsId": "hall_main",
    @Indexed
    private String messHallDocsId;

    // "menuItems": [
    // "Rice",
    // "Dal",
    // "Aloo Gobi",
    // "Chapati",
    // "Papad",
    // "Pickle"
    // ],
    private List<String> menuItems;

    @Builder.Default
    private Boolean vegetarian = true;

    private String remarks;
}
