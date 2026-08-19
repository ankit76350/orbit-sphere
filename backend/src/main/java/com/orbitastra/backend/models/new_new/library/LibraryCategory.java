package com.orbitastra.backend.models.new_new.library;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.new_new.base.SchoolBase;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * One section of the library, as the school divides it up.
 *
 * <p>It is a collection rather than a fixed list because schools do not agree on their
 * sections. One has Fiction and Non-Fiction, another has Marathi Literature and
 * Competitive Exams, a third organises by class. A platform-wide list would fit none of
 * them.
 *
 * <p>It is a collection rather than a plain string on the book for the same reason
 * Gate is: a shelf label typed by hand becomes "Fiction", "fiction" and "Ficton", and
 * nobody notices until somebody asks how many fiction books the school owns.
 *
 * <p>The service checks that a category still used by a book is not deleted.
 */
@Document(collection = "library_categories")
@CompoundIndexes({
        @CompoundIndex(
                name = "school_library_category_name_uniq",
                def = "{'schoolId': 1, 'name': 1}",
                unique = true),
        @CompoundIndex(
                name = "school_library_category_active_idx",
                def = "{'schoolId': 1, 'active': 1, 'sortOrder': 1}")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LibraryCategory extends SchoolBase {

    // Name shown to children and staff. Example: "Competitive Exams"
    @NotBlank
    private String name;

    // Example: "Books for scholarship and entrance tests, reference only."
    private String description;

    // Where this section sits on the shelves, so a child can be sent to it.
    // Example: "Rack 7, ground floor"
    private String shelfArea;

    // Order the sections appear in on screens. Example: 30
    @Builder.Default
    private Integer sortOrder = 0;

    // Whether new books may still be filed here. Example: true
    @NotNull
    @Builder.Default
    private Boolean active = true;
}
