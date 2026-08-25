package com.orbitastra.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;

/**
 * Makes {@code @Transactional} actually work against MongoDB.
 *
 * <p>Spring Boot does not register a transaction manager for MongoDB on its own. Without this
 * bean, a method annotated {@code @Transactional} runs with **no transaction at all** and every
 * write commits independently — which is the failure mode school provisioning cannot tolerate,
 * because a half-created tenant is worse than none.
 *
 * <p>Multi-document transactions need a replica set or a sharded cluster. A standalone
 * {@code mongod} rejects them, so a local single-node setup must be started as a one-member
 * replica set. The configured Atlas cluster is a replica set already.
 */
@Configuration
public class MongoTransactionConfig {

    @Bean
    public MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory databaseFactory) {
        return new MongoTransactionManager(databaseFactory);
    }
}
