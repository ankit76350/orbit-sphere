package com.orbitastra.backend.models.celebrations;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "birthday_notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BirthdayNotification {

    @Id
    private String id;

    private String schoolId;

    private String personType;

    private String personId;

    private String personName;

    private String notificationType;

    private String recipient;

    private String message;

    private String sentAt;
}
