package com.orbitastra.backend.models.undone.a_working.frontoffice;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.undone.a_working.frontoffice.enums.ComplaintCategory;
import com.orbitastra.backend.models.undone.a_working.frontoffice.enums.ComplaintPriority;
import com.orbitastra.backend.models.undone.a_working.frontoffice.enums.ComplaintRaisedByType;
import com.orbitastra.backend.models.undone.a_working.frontoffice.enums.ComplaintStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "complaints")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Complaint extends SchoolBase {

    @Indexed
    private LocalDateTime complaintDateTime;

    /**
     * Student
     * Parent
     * Staff
     * Visitor
     * Other
     */
    private ComplaintRaisedByType raisedByType;

    /**
     * Optional reference.
     */
    private String raisedByDocsId;

    /**
     * Name if not a system user.
     */
    private String raisedByName;

    private String mobileNumber;

    private ComplaintCategory category;

    private ComplaintPriority priority;

    private String subject;

    private String description;

    @Indexed
    private String assignedToDocsId;

    private String resolutionRemarks;

    private LocalDateTime resolvedAt;

    @Builder.Default
    private ComplaintStatus status = ComplaintStatus.OPEN;
}