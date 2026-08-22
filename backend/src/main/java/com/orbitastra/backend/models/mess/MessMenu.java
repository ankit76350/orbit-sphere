package com.orbitastra.backend.models.mess;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * What is being served, for one meal, on one day, in one hall.
 *
 * <p>Parents of boarders ask what their child is eating, and a menu published a week
 * ahead is one of the things a boarding school is judged on. That is the main reason
 * this exists rather than the kitchen keeping it on a whiteboard.
 *
 * <p>{@code items} is a plain list of dish names. The reference sketch tied each dish to
 * a recipe with ingredients, quantities and allergen codes. That belongs to kitchen
 * stock management, which is a different job from telling a family what is for lunch,
 * and a kitchen that has to maintain a recipe database before it can publish a menu will
 * publish no menu at all.
 *
 * <p>{@code containsAllergens} is written as plain words for the same reason. It matters
 * because a child's allergy is recorded in the health package, and a warden comparing
 * the two needs something readable, not a code list nobody fills in.
 *
 * <p>The service checks that one menu exists per hall, meal and day, and that a menu is
 * not published for a day the school is closed unless boarders are in residence.
 */
@Document(collection = "mess_menus")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_mess_menu_uniq",
                def = "{'schoolId': 1, 'menuDate': 1, 'messHallDocsId': 1, 'messMealTypeDocsId': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_mess_menu_week_idx",
                def = "{'schoolId': 1, 'menuDate': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MessMenu extends SchoolBase {

    // The day this is served. Example: 2026-08-19
    @NotNull
    private LocalDate menuDate;

    // Links to MessMealType.id. Example: "67bb1122dc3f7d0011223344"
    @NotBlank
    private String messMealTypeDocsId;

    // Links to MessHall.id. Example: "67bb1123dc3f7d0022334455"
    @NotBlank
    private String messHallDocsId;

    // The dishes, in the order they are served. Plain names, not recipes.
    // Example: ["Poha", "Boiled egg", "Banana", "Milk"]
    @NotEmpty
    @Builder.Default
    private List<String> items = new ArrayList<>();

    // Whether everything on this menu is vegetarian. Example: false
    @NotNull
    @Builder.Default
    private Boolean vegetarian = true;

    // What is in it that some children must avoid, in plain words. Compared by hand
    // against the allergy alerts in a child's health profile.
    // Example: "Contains egg and peanuts."
    private String containsAllergens;

    // Whether families can see it yet. Example: true
    @NotNull
    @Builder.Default
    private Boolean published = false;

    // Example: "Special lunch for Independence Day."
    private String remarks;
}
