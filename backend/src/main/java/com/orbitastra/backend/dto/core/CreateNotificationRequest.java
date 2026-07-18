package com.orbitastra.backend.dto.core;

import com.orbitastra.backend.models.core.enums.NotificationChannel;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Client payload for queuing a notification. {@code sent}/{@code sentAt} are
 * managed by the service (defaulting to not-sent) and are not accepted from the
 * request body, along with {@code id} and audit timestamps.
 */
@Data
public class CreateNotificationRequest {

    @NotBlank(message = "schoolId is required")
    private String schoolId;

    @NotBlank(message = "recipientId is required")
    private String recipientId;

    private NotificationChannel channel;

    private String message;
}
