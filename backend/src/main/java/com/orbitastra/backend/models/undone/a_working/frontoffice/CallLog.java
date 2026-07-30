package com.orbitastra.backend.models.undone.a_working.frontoffice;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;


import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;
import com.orbitastra.backend.models.undone.a_working.frontoffice.enums.CallDirection;
import com.orbitastra.backend.models.undone.a_working.frontoffice.enums.CallPurpose;
import com.orbitastra.backend.models.undone.a_working.frontoffice.enums.CallStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A reception phone-call log entry. Admission-enquiry calls can be pushed to the
 * CRM as an {@code crm.Inquiry} (flagged via {@code sentToCrm}).
 */
@Document(collection = "call_logs")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CallLog extends SchoolBase {

    @Indexed
    private LocalDateTime callDateTime;

    private CallDirection direction;

    private String callerName;

    @Indexed
    private String phoneNumber;

    private CallPurpose purpose;

    /**
     * Optional free-text details.
     */
    private String notes;

    /**
     * Staff handling the call.
     */
    @Indexed
    private String handledByDocsId;

    /**
     * Optional follow-up date.
     */
    private LocalDate followUpDate;

    /**
     * If converted to Admission Inquiry.
     */
    private String inquiryDocsId;

    @Builder.Default
    private CallStatus status = CallStatus.OPEN;
}