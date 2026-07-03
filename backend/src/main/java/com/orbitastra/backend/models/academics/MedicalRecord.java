package com.orbitastra.backend.models.academics;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "medical_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRecord {

    @Id
    private String id;

    @Indexed
    private String schoolId;

    @Indexed
    private String studentId;

    private LocalDate visitDate;

    private String diagnosis;

    private List<String> medicines;

    private String doctorName;
}
