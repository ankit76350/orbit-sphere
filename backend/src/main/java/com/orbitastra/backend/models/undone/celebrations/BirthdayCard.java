package com.orbitastra.backend.models.undone.celebrations;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "birthday_cards")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BirthdayCard extends SchoolBase {

    private String personType;

    private String personId;

    private String personName;

    private String cardUrl;

    private String message;

    private String theme;

}
