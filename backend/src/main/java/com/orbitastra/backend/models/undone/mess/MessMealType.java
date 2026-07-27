package com.orbitastra.backend.models.undone.mess;

@Document(collection = "mess_meal_types")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MessMealType extends SchoolBase {

    @Indexed(unique = true)
    private String name;

    private LocalTime servingFrom;

    private LocalTime servingTo;

    @Builder.Default
    private Integer sortOrder = 0;

    @Builder.Default
    private Boolean active = true;
}
    
}
