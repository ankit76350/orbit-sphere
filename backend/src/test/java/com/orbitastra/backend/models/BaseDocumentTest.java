package com.orbitastra.backend.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;

import com.orbitastra.backend.models.base.SchoolDocs;

class BaseDocumentTest {

    private static final Set<String> COMMON_FIELDS =
            Set.of("createdAt", "updatedAt", "id", "schoolId");

    @Test
    void everyMongoDocumentExtendsBaseDocumentWithoutRedeclaringCommonFields() throws ClassNotFoundException {
        Set<Class<?>> documentTypes = findDocumentTypes();

        assertFalse(documentTypes.isEmpty());
        for (Class<?> documentType : documentTypes) {
            assertTrue(SchoolDocs.class.isAssignableFrom(documentType),
                    () -> documentType.getName() + " must extend BaseDocument");
            assertTrue(Arrays.stream(documentType.getDeclaredFields())
                            .noneMatch(field -> COMMON_FIELDS.contains(field.getName())),
                    () -> documentType.getName() + " redeclares a BaseDocument field");
        }
    }

    @Test
    void springDataMapsInheritedCommonFieldsForEveryDocument() throws Exception {
        Set<Class<?>> documentTypes = findDocumentTypes();
        MongoMappingContext mappingContext = new MongoMappingContext();
        mappingContext.setSimpleTypeHolder(
                MongoCustomConversions.create(adapter -> { }).getSimpleTypeHolder());
        mappingContext.setInitialEntitySet(documentTypes);
        mappingContext.afterPropertiesSet();

        for (Class<?> documentType : documentTypes) {
            MongoPersistentEntity<?> entity = mappingContext.getPersistentEntity(documentType);
            assertNotNull(entity, () -> "Missing Mongo mapping for " + documentType.getName());
            assertNotNull(entity.getIdProperty(), () -> "Missing inherited id for " + documentType.getName());
            assertEquals("id", entity.getIdProperty().getName());
            for (String field : COMMON_FIELDS) {
                assertNotNull(entity.getPersistentProperty(field),
                        () -> "Missing inherited " + field + " for " + documentType.getName());
            }
        }
    }

    private Set<Class<?>> findDocumentTypes() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Document.class));

        Set<BeanDefinition> candidates = scanner.findCandidateComponents("com.orbitastra.backend.models");
        return candidates.stream()
                .map(BeanDefinition::getBeanClassName)
                .map(this::loadClass)
                .collect(Collectors.toSet());
    }

    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Could not load document class " + className, ex);
        }
    }
}
