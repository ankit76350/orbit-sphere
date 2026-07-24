package com.orbitastra.backend.repositories.core;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.core.Notification;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByRecipientDocsId(String recipientDocsId);
    List<Notification> findBySchoolId(String schoolId);
    List<Notification> findByRecipientDocsIdAndSent(String recipientDocsId, Boolean sent);
}
