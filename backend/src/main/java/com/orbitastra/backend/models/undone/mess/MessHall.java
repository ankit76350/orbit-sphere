package com.orbitastra.backend.models.undone.mess;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "mess_halls")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MessHall extends SchoolBase {

    // Main Mess Hall
    @Indexed(unique = true)
    private String name;

    //Ground Floor, Hostel Block
    private String location;

    // 500
    private Integer capacity;

    @Builder.Default
    private Boolean active = true;
}
