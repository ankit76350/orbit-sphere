package com.orbitastra.backend.model;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.model.enums.InquiryStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "inquiries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inquiry {

    @Id
    private String id;

    private String schoolId;

    private String parentName;

    private String studentName;

    private String phone;

    private String email;

    private String source;

    private String counselorId;

    private InquiryStatus status;

    private LocalDate nextFollowUp;

    private String notes;
}
