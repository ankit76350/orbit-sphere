package com.orbitastra.backend.models.core;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolDocs;
import com.orbitastra.backend.models.core.enums.NotificationChannel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "notifications")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends SchoolDocs {

    private String recipientId;

    private NotificationChannel channel;

    private String message;

    private Boolean sent;

    private LocalDateTime sentAt;
}
