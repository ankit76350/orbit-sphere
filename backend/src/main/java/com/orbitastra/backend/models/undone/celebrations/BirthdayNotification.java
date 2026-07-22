package com.orbitastra.backend.models.undone.celebrations;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.BaseDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "birthday_notifications")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BirthdayNotification extends BaseDocument {

    private String personType;

    private String personId;

    private String personName;

    private String notificationType;

    private String recipient;

    private String message;

    private String sentAt;
}
