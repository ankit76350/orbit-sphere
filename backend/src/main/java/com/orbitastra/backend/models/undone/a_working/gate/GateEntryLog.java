package com.orbitastra.backend.models.undone.a_working.gate;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.a_working.gate.enums.GateEntryType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "gate_entry_logs")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class GateEntryLog extends SchoolBase {

    //     2:01 PM

    // Visitor Entered

    // ↓

    // 3:05 PM

    // Visitor Exited

    // ↓

    // 4:20 PM

    // Student Exited

    // ↓

    // 5:00 PM

    // Student Returned

    private GateEntryType entryType;

    private String referenceDocsId;

    private LocalDateTime timestamp;

    private String gateName;

    private String securityStaffDocsId;

    private String remarks;
}