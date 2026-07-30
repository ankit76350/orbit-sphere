package com.orbitastra.backend.models.undone.a_new.mess;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.a_new.base.CampusScopedDocument;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Document(collection = "meal_recipes")
@CompoundIndex(name = "tenant_recipe_code_version_uniq",
        def = "{'tenantId':1,'recipeCode':1,'recipeVersion':1}", unique = true)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MealRecipe extends CampusScopedDocument {

    private String recipeCode;
    private Integer recipeVersion;
    private String name;
    private Integer servings;
    private BigDecimal caloriesPerServing;
    private Boolean vegetarian;
    private Boolean vegan;
    private Boolean active;

    @Builder.Default
    private List<Ingredient> ingredients = new ArrayList<>();

    @Builder.Default
    private List<String> allergenCodes = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Ingredient {
        private String kitchenStockItemDocsId;
        private BigDecimal quantity;
        private String unitCode;
    }
}
