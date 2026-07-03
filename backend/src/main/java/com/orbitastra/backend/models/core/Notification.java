package com.orbitastra.backend.models.core;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.core.enums.NotificationChannel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    private String id;

    @Indexed
    private String schoolId;

    private String recipientId;

    private NotificationChannel channel;

    private String message;

    private Boolean sent;

    private LocalDateTime sentAt;
}
