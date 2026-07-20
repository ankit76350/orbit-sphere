package com.orbitastra.backend.models.undone.communication;

import java.time.LocalDate;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.communication.enums.DiaryEntryType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A digital-diary entry pushed to a class (homework note, remark or reminder),
 * with parent acknowledgement tracking.
 */
@Document(collection = "diary_entries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryEntry {

    @CreatedDate
    private java.time.LocalDateTime createdAt;

    @LastModifiedDate
    private java.time.LocalDateTime updatedAt;

    @Id
    private String id;

    @Indexed
    private String schoolId;

    private String grade;

    private LocalDate date;

    private DiaryEntryType type;

    private String subject;

    private String text;

    private String teacher; // references Staff.id

    // Acknowledgement tracking.
    @Builder.Default
    private Integer ackCount = 0;

    private Integer totalRecipients;
}
