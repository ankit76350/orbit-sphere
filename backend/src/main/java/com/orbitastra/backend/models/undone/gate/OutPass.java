package com.orbitastra.backend.models.undone.gate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "outpasses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutPass {
    @org.springframework.data.annotation.CreatedDate
    private java.time.LocalDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    private java.time.LocalDateTime updatedAt;


    @Id
    private String id;

    private String schoolId;

    private String studentId;

    private String studentName;

    private String parentName;

    private String reason;

    private String leaveDate;

    private String returnDate;

    private String status;

    private String approvedBy;
}
