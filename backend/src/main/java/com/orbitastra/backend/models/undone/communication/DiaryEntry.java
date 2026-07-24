package com.orbitastra.backend.models.undone.communication;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
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
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DiaryEntry extends SchoolBase {

    private String grade;

    private LocalDate date;

    private DiaryEntryType type;

    private String subject;

    private String text;

    private String teacherName;

    // Acknowledgement tracking.
    @Builder.Default
    private Integer ackCount = 0;

    private Integer totalRecipients;
}
