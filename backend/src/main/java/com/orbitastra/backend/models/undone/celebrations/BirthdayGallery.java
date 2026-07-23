package com.orbitastra.backend.models.undone.celebrations;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolDocs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "birthday_gallery")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BirthdayGallery extends SchoolDocs {

    private String personType;

    private String personId;

    private String personName;

    private String mediaUrl;

    private String caption;

}
