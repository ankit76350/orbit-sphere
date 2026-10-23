package com.orbitastra.backend.models.mess;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One place where food is served.
 *
 * <p>Most schools have one. A large boarding school has two or three, sometimes split
 * by building or by age, which is why this is a collection rather than a setting.
 *
 * <p>{@code capacity} is how many can sit at once. It is what tells a school it needs
 * two sittings for a meal rather than one.
 */
@Document(collection = "mess_halls")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_mess_hall_name_uniq",
                def = "{'schoolId': 1, 'name': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_mess_hall_active_idx",
                def = "{'schoolId': 1, 'active': 1, 'name': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MessHall extends SchoolBase {

    // Example: "Main Dining Hall"
    @NotBlank
    private String name;

    // Where it is. Example: "Ground floor, next to Tagore House."
    private String location;

    // How many can sit at once. Two sittings are needed above this. Example: 240
    @Positive
    private Integer capacity;

    // Links to Staff.id for whoever runs it. Example: "67aa15d9dc3f7d0044444444"
    private String messManagerStaffDocsId;

    // Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
