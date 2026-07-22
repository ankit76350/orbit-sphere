package com.orbitastra.backend.config;

import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.mongodb.client.result.UpdateResult;

/** Shared, idempotent MongoDB field-migration operations. */
final class MongoFieldMigrationSupport {

    private MongoFieldMigrationSupport() {
    }

    static long renameWhenTargetMissing(MongoTemplate mongoTemplate, Class<?> documentType,
                                        String legacyField, String currentField) {
        Query query = Query.query(new Criteria().andOperator(
                Criteria.where(legacyField).exists(true),
                Criteria.where(currentField).exists(false)));
        UpdateResult result = mongoTemplate.updateMulti(
                query, new Update().rename(legacyField, currentField), documentType);
        return result.getModifiedCount();
    }

    static long initialiseMissingList(MongoTemplate mongoTemplate, Class<?> documentType,
                                      String field) {
        Query query = Query.query(Criteria.where(field).is(null));
        UpdateResult result = mongoTemplate.updateMulti(
                query, new Update().set(field, List.of()), documentType);
        return result.getModifiedCount();
    }
}
