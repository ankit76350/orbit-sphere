package com.orbitastra.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;

/**
 * Enables real multi-document MongoDB transactions.
 *
 * <p>Spring's {@code @Transactional} annotation is a no-op for MongoDB unless a
 * {@link MongoTransactionManager} bean is present — Spring Boot does not register
 * one automatically. With this bean, {@code @Transactional} service methods
 * (e.g. fee payment → wallet debit → invoice update) run atomically: a failure
 * part-way rolls the whole operation back instead of leaving inconsistent balances.
 *
 * <p>Requires the MongoDB deployment to be a replica set. MongoDB Atlas (used here)
 * always is; a bare single-node local {@code mongod} is not and would reject
 * transactions — start it with {@code --replSet} if running locally.
 */
@Configuration
public class MongoConfig {

    @Bean
    MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
}
