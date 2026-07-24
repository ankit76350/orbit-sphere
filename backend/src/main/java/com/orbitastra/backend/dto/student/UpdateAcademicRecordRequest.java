package com.orbitastra.backend.dto.student;

import java.util.LinkedHashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.orbitastra.backend.models.student.enums.StudentStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Strict PATCH payload for an existing student academic record.
 *
 * <p>The provided-field set distinguishes an omitted property (preserve the
 * stored value) from an explicit {@code null} (clear an optional string).
 */
@Getter
@NoArgsConstructor
public class UpdateAcademicRecordRequest {

    @JsonIgnore
    private final Set<String> providedFields = new LinkedHashSet<>();

    private String identityNo;
    private String rollNo;
    private String classDocsId;
    private String sectionNo;
    private String hostelRoomNo;
    private StudentStatus status;

    @JsonSetter("identityNo")
    public void setIdentityNo(String identityNo) {
        providedFields.add("identityNo");
        this.identityNo = identityNo;
    }

    @JsonSetter("rollNo")
    public void setRollNo(String rollNo) {
        providedFields.add("rollNo");
        this.rollNo = rollNo;
    }

    @JsonSetter("classDocsId")
    public void setClassDocsId(String classDocsId) {
        providedFields.add("classDocsId");
        this.classDocsId = classDocsId;
    }

    @JsonSetter("sectionNo")
    public void setSectionNo(String sectionNo) {
        providedFields.add("sectionNo");
        this.sectionNo = sectionNo;
    }

    @JsonSetter("hostelRoomNo")
    public void setHostelRoomNo(String hostelRoomNo) {
        providedFields.add("hostelRoomNo");
        this.hostelRoomNo = hostelRoomNo;
    }

    @JsonSetter("status")
    public void setStatus(StudentStatus status) {
        providedFields.add("status");
        this.status = status;
    }

    public boolean isProvided(String fieldName) {
        return providedFields.contains(fieldName);
    }

    public boolean hasUpdates() {
        return !providedFields.isEmpty();
    }

    @JsonAnySetter
    public void rejectUnsupportedField(String fieldName, Object value) {
        throw new IllegalArgumentException(
                "Unsupported academic-record update field '" + fieldName
                        + "'. Allowed fields are identityNo, rollNo, classDocsId, "
                        + "sectionNo, hostelRoomNo, and status.");
    }
}
