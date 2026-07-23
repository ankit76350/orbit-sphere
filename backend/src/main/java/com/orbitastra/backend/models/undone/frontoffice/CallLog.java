package com.orbitastra.backend.models.undone.frontoffice;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.frontoffice.enums.CallStatus;
import com.orbitastra.backend.models.undone.frontoffice.enums.CallType;

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

    private LocalDate date;

    private LocalTime time;

    private String caller;

    private String phone;

    private CallType type;

    // Reason for the call, e.g. "Admission Enquiry", "Fee Query", "Complaint".
    private String purpose;

    private String assignedTo; // references Staff.id

    private LocalDate followUp;

    @Builder.Default
    private CallStatus status = CallStatus.OPEN;

    @Builder.Default
    private Boolean sentToCrm = false;
}
