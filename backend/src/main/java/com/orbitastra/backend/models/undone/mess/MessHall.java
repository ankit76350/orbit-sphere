package com.orbitastra.backend.models.undone.mess;

@Document(collection = "mess_halls")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MessHall extends SchoolBase {

    @Indexed(unique = true)
    private String name;

    private String location;

    private Integer capacity;

    @Builder.Default
    private Boolean active = true;
}
