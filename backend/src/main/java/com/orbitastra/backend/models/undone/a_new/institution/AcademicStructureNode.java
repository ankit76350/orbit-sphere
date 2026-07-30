package com.orbitastra.backend.models.undone.a_new.institution;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.AcademicScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "academic_structure_nodes")
@CompoundIndexes({
        @CompoundIndex(name = "tenant_year_node_code_uniq",
                def = "{'tenantId':1,'academicYearDocsId':1,'type':1,'code':1}", unique = true),
        @CompoundIndex(name = "tenant_year_parent_order_idx",
                def = "{'tenantId':1,'academicYearDocsId':1,'parentNodeDocsId':1,'sortOrder':1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicStructureNode extends AcademicScopedDocument {

    public enum NodeType {
        PHASE,
        GRADE,
        CLASS,
        SECTION,
        SUBJECT,
        HOUSE,
        DEPARTMENT,
        ROOM,
        LABORATORY,
        ELECTIVE_BLOCK
    }

    private NodeType type;
    private String parentNodeDocsId;
    private String code;
    private String name;
    private Integer sortOrder;
    private Integer capacity;
    private String leadStaffDocsId;

    @Builder.Default
    private Map<String, String> attributes = new HashMap<>();
}
