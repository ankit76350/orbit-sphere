package com.orbitastra.backend.models.new_new.gate;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.new_new.gate.enums.GateType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One way in or out of the school.
 *
 * <p>A school has two or three of these, so this is a small collection. It exists
 * anyway rather than letting movements hold a gate name as free text, because a
 * log where half the rows say "Main Gate" and the rest say "main gate" cannot be
 * counted or grouped, and nobody notices until somebody asks how many people came
 * through the main gate last month.
 *
 * <p>{@code hasCardReader} says whether the gate can identify people by itself.
 * A gate without one is worked by a guard typing names, and the movements from it
 * carry IdentificationMethod.MANUAL. Knowing which gates those are matters when
 * somebody argues about a time.
 *
 * <p>{@code active} being false closes a gate without deleting it, so the
 * movements already recorded there still make sense.
 */
@Document(collection = "gates")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_gate_code_uniq",
                def = "{'schoolId': 1, 'gateCode': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_gate_active_idx",
                def = "{'schoolId': 1, 'active': 1, 'name': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Gate extends SchoolBase {

    // Stable key movements point at. Example: "MAIN"
    @NotBlank
    private String gateCode;

    // Name staff and guards see. Example: "Main Gate, Link Road side"
    @NotBlank
    private String name;

    // What the gate is mainly used for. Example: GateType.MAIN
    @NotNull
    private GateType gateType;

    // Whether a card reader is fitted, so the gate can identify people itself.
    // Example: true
    @NotNull
    @Builder.Default
    private Boolean hasCardReader = false;

    // Whether visitors may be let in here, or only staff and students.
    // Example: true
    @NotNull
    @Builder.Default
    private Boolean visitorsAllowed = true;

    // Whether the gate is in use. Turning it off leaves old movements readable.
    // Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;

    // Example: "Kept locked after 6pm; night entry is through the service gate."
    private String remarks;
}
