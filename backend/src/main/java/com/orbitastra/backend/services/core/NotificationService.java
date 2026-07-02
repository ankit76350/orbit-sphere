package com.orbitastra.backend.services.core;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.core.Notification;
import com.orbitastra.backend.repositories.core.NotificationRepository;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Notification createNotification(Notification notification) {
        if (notification.getSent() == null) {
            notification.setSent(false);
        }
        return notificationRepository.save(notification);
    }

    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    public Notification getNotificationById(String id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
    }

    public List<Notification> getNotificationsByRecipient(String recipientId) {
        return notificationRepository.findByRecipientId(recipientId);
    }

    public List<Notification> getNotificationsByRecipientAndSentStatus(String recipientId, Boolean sent) {
        return notificationRepository.findByRecipientIdAndSent(recipientId, sent);
    }

    public List<Notification> getNotificationsBySchool(String schoolId) {
        return notificationRepository.findBySchoolId(schoolId);
    }

    public Notification markAsSent(String id) {
        Notification notification = getNotificationById(id);
        notification.setSent(true);
        notification.setSentAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    public void deleteNotification(String id) {
        Notification notification = getNotificationById(id);
        notificationRepository.delete(notification);
    }
}
