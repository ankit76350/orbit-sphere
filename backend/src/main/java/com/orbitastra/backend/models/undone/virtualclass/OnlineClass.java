package com.orbitastra.backend.models.undone.virtualclass;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.BaseDocument;
import com.orbitastra.backend.models.undone.virtualclass.enums.OnlineClassStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "online_classes")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OnlineClass extends BaseDocument {

    private String subjectId;

    private String subjectName;

    private String teacherId;

    private String teacherName;

    private String classId;

    private String sectionId;

    private String meetingLink;

    private String date;

    private String startTime;

    private String endTime;

    private OnlineClassStatus status;
}
