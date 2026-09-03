package com.orbitastra.backend.config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;

/**
 * Builds the MongoDB indexes from the model annotations, but only when we ask it to.
 *
 * <p>We used to let Spring do this on every boot with
 * {@code spring.data.mongodb.auto-index-creation=true}. The models carry 707 index
 * definitions across 147 collections, and Spring creates them one at a time. Each one is a
 * separate round trip to the Atlas cluster taking roughly half a second, so **every single
 * start and every devtools reload sat there for over six minutes** before Tomcat came up.
 *
 * <p>So auto creation is off now, and this runner does the same job on demand instead. Set
 * {@code app.mongo.sync-indexes=true} and start the app once; it builds everything and then you
 * turn the flag back off. Building an index that is already there is harmless, so it is safe to
 * run again whenever you add or change an index annotation.
 *
 * <p>It also builds many indexes at the same time rather than one after another, which is why
 * this takes well under a minute where the old startup took six.
 */
@Configuration
@ConditionalOnProperty(name = "app.mongo.sync-indexes", havingValue = "true")
public class MongoIndexSyncRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MongoIndexSyncRunner.class);

    // How many indexes we build at the same time. The wait is almost all network, not work on
    // our side, so a fair few at once is fine and it is what makes this quick.
    private static final int INDEXES_AT_A_TIME = 16;

    private final MongoTemplate mongoTemplate;
    private final MongoMappingContext mappingContext;

    // Goes true if the app started reloading while we were still building. Once the connection
    // to Mongo is closed every index left in the queue would fail for the same reason, so we
    // say it once and skip the rest instead of printing hundreds of identical errors.
    private volatile boolean appIsReloading = false;

    public MongoIndexSyncRunner(MongoTemplate mongoTemplate, MongoMappingContext mappingContext) {
        this.mongoTemplate = mongoTemplate;
        this.mappingContext = mappingContext;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        long startedAt = System.currentTimeMillis();

        log.info("[syncIndexes] Step 1: Reading the index annotations off every model");
        List<PendingIndex> pending = collectIndexes();
        log.info("[syncIndexes] Found {} index(es) to make sure of", pending.size());

        log.info("[syncIndexes] Step 2: Building them, {} at a time", INDEXES_AT_A_TIME);
        int failed = buildAll(pending);

        long seconds = (System.currentTimeMillis() - startedAt) / 1000;
        if (appIsReloading) {
            log.warn("[syncIndexes] Stopped after {}s because the app reloaded part way through."
                    + " Nothing is broken, but the indexes are only half built — start the app"
                    + " again with the flag still on to finish them.", seconds);
        } else if (failed == 0) {
            log.info("[syncIndexes] Done. All {} index(es) are in place, took {}s",
                    pending.size(), seconds);
        } else {
            // Not fatal on purpose: the usual cause is old rows that break a new unique index,
            // and we would rather the app start so you can go and look at them.
            log.warn("[syncIndexes] Done with problems. {} of {} index(es) could not be built,"
                    + " took {}s. See the errors above.", failed, pending.size(), seconds);
        }
    }

    /** Works out every index the models ask for, and which collection each one belongs to. */
    private List<PendingIndex> collectIndexes() {
        IndexResolver resolver = IndexResolver.create(mappingContext);
        List<PendingIndex> pending = new ArrayList<>();

        for (MongoPersistentEntity<?> entity : mappingContext.getPersistentEntities()) {
            // Skip the nested types that only live inside another document. They have no
            // collection of their own, so there is nothing to index.
            if (!entity.isAnnotationPresent(Document.class)) {
                continue;
            }
            for (IndexDefinition definition : resolver.resolveIndexFor(entity.getType())) {
                pending.add(new PendingIndex(entity.getType(), entity.getCollection(), definition));
            }
        }
        return pending;
    }

    /** Builds all the indexes and returns how many of them failed. */
    private int buildAll(List<PendingIndex> pending) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(INDEXES_AT_A_TIME);
        try {
            List<Callable<Boolean>> jobs = new ArrayList<>();
            for (PendingIndex index : pending) {
                jobs.add(() -> buildOne(index));
            }

            int failed = 0;
            for (Future<Boolean> result : pool.invokeAll(jobs)) {
                try {
                    if (!result.get()) {
                        failed++;
                    }
                } catch (Exception e) {
                    failed++;
                }
            }
            return failed;
        } finally {
            pool.shutdown();
        }
    }

    /** Makes sure one index exists. Returns false if Mongo refused it. */
    private boolean buildOne(PendingIndex index) {
        if (appIsReloading) {
            return false;
        }
        try {
            mongoTemplate.indexOps(index.entityType).createIndex(index.definition);
            return true;
        } catch (RuntimeException e) {
            if (isConnectionClosed(e)) {
                appIsReloading = true;
                return false;
            }
            log.error("[syncIndexes] Could not build an index on '{}' ({}): {}",
                    index.collection, index.definition.getIndexKeys().toJson(), e.getMessage());
            return false;
        }
    }

    /**
     * Tells us the app is shutting down under us rather than Mongo refusing the index. The
     * driver says "state should be: open" once its connection has been closed.
     */
    private boolean isConnectionClosed(RuntimeException e) {
        String message = e.getMessage();
        return message != null && message.contains("state should be: open");
    }

    /** One index we still have to build, and the collection it goes on. */
    private static final class PendingIndex {

        private final Class<?> entityType;
        private final String collection;
        private final IndexDefinition definition;

        private PendingIndex(Class<?> entityType, String collection, IndexDefinition definition) {
            this.entityType = entityType;
            this.collection = collection;
            this.definition = definition;
        }
    }
}
