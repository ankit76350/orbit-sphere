package com.orbitastra.backend.models.undone.gate;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolDocs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "visitors")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Visitor extends SchoolDocs {

    private String visitorName;

    private String relationship;

    private String studentId;

    private String studentName;

    private String entryTime;

    private String exitTime;

    private String purpose;
}
