package com.orbitastra.backend.models.mess;

import java.time.LocalTime;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One meal in the school's day, and when it is served.
 *
 * <p>A collection rather than a fixed list of breakfast, lunch and dinner, because
 * schools genuinely differ. One serves a morning snack and an evening one; a school with
 * younger boarders adds a bedtime milk; a school in another state runs different timings
 * altogether. A platform-wide list would fit none of them.
 *
 * <p>{@code servingFrom} and {@code servingTo} are what let a card tap at the mess door
 * be attributed to the right meal without anybody choosing it from a list.
 */
@Document(collection = "mess_meal_types")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_meal_type_name_uniq",
                def = "{'schoolId': 1, 'name': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_meal_type_order_idx",
                def = "{'schoolId': 1, 'active': 1, 'sortOrder': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MessMealType extends SchoolBase {

    // Name children see. Example: "Breakfast"
    @NotBlank
    private String name;

    // When serving starts. Example: 07:15
    @NotNull
    private LocalTime servingFrom;

    // When it stops. A tap between these two is counted against this meal.
    // Example: 08:30
    @NotNull
    private LocalTime servingTo;

    // Order through the day, so a menu reads in the right sequence. Example: 1
    @NotNull
    @Builder.Default
    private Integer sortOrder = 0;

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
