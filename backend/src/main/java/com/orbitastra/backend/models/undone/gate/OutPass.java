package com.orbitastra.backend.models.undone.gate;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "outpasses")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OutPass extends SchoolBase {

    private String studentDocsId;

    private String studentName;

    private String parentName;

    private String reason;

    private String leaveDate;

    private String returnDate;

    private String status;

    private String approvedByName;
}
