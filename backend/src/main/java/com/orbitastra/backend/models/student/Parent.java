package com.orbitastra.backend.models.student;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "parents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Parent {

    @Id
    private String id;

    private String schoolId;

    private String fatherName;

    private String motherName;

    private String phone;

    private String alternatePhone;

    private String email;

    private String address;

    @Builder.Default
    private List<String> studentIds = new ArrayList<>();
}