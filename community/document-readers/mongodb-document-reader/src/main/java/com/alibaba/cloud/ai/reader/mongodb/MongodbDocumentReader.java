package com.alibaba.cloud.ai.reader.mongodb;

import com.alibaba.cloud.ai.reader.mongodb.converter.DefaultDocumentConverter;
import com.alibaba.cloud.ai.reader.mongodb.converter.DocumentConverter;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.micrometer.core.instrument.Timer;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.DocumentReader;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Closeable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * MongoDB文档读取器实现类
 */
public class MongodbDocumentReader implements DocumentReader, Closeable {
    private static final Logger log = LoggerFactory.getLogger(MongodbDocumentReader.class);

    private final MongoTemplate mongoTemplate;
    private final MongodbResource properties;

    private final MongoClient mongoClient;

    private final boolean shouldCloseClient;

    private volatile boolean closed = false;

    /**
     * 转换接口
     */
    private final DocumentConverter documentConverter;

    // MongoDB URI格式验证
    private static final Pattern MONGODB_URI_PATTERN = Pattern.compile("mongodb(?:\\+srv)?://[^/]+(/[^?]+)?(\\?.*)?");


    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder模式构建器
     */
    public static class Builder {
        private MongoTemplate mongoTemplate;
        private MongodbResource resource;
        private MongoClient mongoClient;
        private DocumentConverter converter;

        public Builder withMongoTemplate(MongoTemplate mongoTemplate) {
            this.mongoTemplate = mongoTemplate;
            return this;
        }

        public Builder withResource(MongodbResource resource) {
            this.resource = resource;
            return this;
        }

        public Builder withMongoClient(MongoClient mongoClient) {
            this.mongoClient = mongoClient;
            return this;
        }


        public Builder withDocumentConverter(DocumentConverter converter) {
            this.converter = converter;
            return this;
        }

        /**
         * 创建MongoDB客户端
         * 根据配置创建具有连接池和超时设置的MongoDB客户端
         *
         * @param resource MongoDB配置资源
         * @return 配置好的MongoDB客户端实例
         */
        private static MongoClient createMongoClient(MongodbResource resource) {
            Assert.notNull(resource, "MongodbResource must not be null");
            Assert.hasText(resource.getUri(), "MongoDB URI must not be empty");

            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(resource.getUri()))
                    .applyToConnectionPoolSettings(builder ->
                            builder.maxSize(resource.getPoolSize())
                                    .minSize(1)
                                    .maxWaitTime(2000, TimeUnit.MILLISECONDS)
                                    .maxConnectionLifeTime(30, TimeUnit.MINUTES)
                    )
                    .applyToSocketSettings(builder ->
                            builder.connectTimeout(resource.getConnectTimeout(), TimeUnit.MILLISECONDS)
                                    .readTimeout(resource.getConnectTimeout(), TimeUnit.MILLISECONDS)
                    )
                    .applyToServerSettings(builder ->
                            builder.heartbeatFrequency(10000, TimeUnit.MILLISECONDS)
                    )
                    .build();

            return MongoClients.create(settings);
        }

        public MongodbDocumentReader build() {
            Assert.notNull(resource, "MongodbResource must not be null");

            if (mongoTemplate == null && mongoClient == null) {
                mongoClient = createMongoClient(resource);
                mongoTemplate = new MongoTemplate(mongoClient, resource.getDatabase());
            } else if (mongoTemplate == null) {
                mongoTemplate = new MongoTemplate(mongoClient, resource.getDatabase());
            }
            return new MongodbDocumentReader(this);
        }
    }

    private MongodbDocumentReader(Builder builder) {
        this.properties = builder.resource;
        this.mongoTemplate = builder.mongoTemplate;
        this.mongoClient = builder.mongoClient;
        this.documentConverter = Objects.isNull(builder.converter) ? new DefaultDocumentConverter() : builder.converter;
        this.shouldCloseClient = builder.mongoClient == null;


        validateConfiguration();
    }

    /**
     * 验证配置的有效性
     */
    private void validateConfiguration() {
        validateMongoDbUri(properties.getUri());
        validatePoolSettings(properties);
    }

    private void validateMongoDbUri(String uri) {
        Assert.hasText(uri, "MongoDB URI must not be empty");
        if (!MONGODB_URI_PATTERN.matcher(uri).matches()) {
            throw new IllegalArgumentException("Invalid MongoDB URI format");
        }
    }

    private void validatePoolSettings(MongodbResource resource) {
        if (resource.getPoolSize() <= 0) {
            throw new IllegalArgumentException("Pool size must be greater than 0");
        }
        if (resource.getConnectTimeout() <= 0) {
            throw new IllegalArgumentException("Connect timeout must be greater than 0");
        }
    }

    /**
     * 执行查询并记录性能指标
     */
    private <T> T executeWithMetrics(String operation, Supplier<T> query) {
        checkState();
        Timer.Sample sample = Timer.start();
        try {
            log.debug("Executing operation: {}", operation);
            T result = query.get();
            log.debug("Operation completed successfully: {}", operation);
            return result;
        } catch (MongoException e) {
            log.error("Operation failed: {}", operation, e);
            throw new RuntimeException(e);
        }
    }


    /**
     * 检查读取器状态
     */
    private void checkState() {
        if (closed) {
            throw new IllegalStateException("MongodbDocumentReader has been closed");
        }
    }

    @Override
    public void close() {
        if (!closed) {
            synchronized (this) {
                if (!closed) {
                    try {
                        log.info("Closing MongodbDocumentReader...");
                        if (shouldCloseClient && mongoClient != null) {
                            mongoClient.close();
                        }

                        log.info("MongodbDocumentReader closed successfully");
                    } catch (Exception e) {
                        log.error("Error while closing MongodbDocumentReader", e);
                    } finally {
                        closed = true;
                    }
                }
            }
        }
    }

    /**
     * 根据条件定义查询文档
     *
     * @param criteriaDefinition MongoDB查询条件定义，用于指定查询规则
     * @return 符合条件的文档列表，如果条件为空则返回空列表
     */
    public List<org.springframework.ai.document.Document> findByCriteriaDefinition(CriteriaDefinition criteriaDefinition) {
        if (criteriaDefinition == null) {
            return Collections.emptyList();
        }
        //校验集合
        validateConfiguration();

        return processDocuments(new Query(criteriaDefinition));
    }

    /**
     * 使用Query对象进行文档查询
     *
     * @param query MongoDB查询对象，可以包含各种查询条件
     * @return 符合查询条件的文档列表，如果查询对象为空则返回空列表
     */
    public List<org.springframework.ai.document.Document> findByQuery(Query query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return processDocuments(query);
    }


    /**
     * 分页查询文档
     *
     * @param query MongoDB查询对象
     * @param page  页码，从0开始
     * @param size  每页大小
     * @return 分页后的文档列表
     */
    public List<org.springframework.ai.document.Document> findWithPagination(Query query, int page, int size) {
        query.skip((long) page * size).limit(size);
        return processDocuments(query);
    }

    /**
     * 实现DocumentReader接口的get方法
     * 根据配置的查询条件获取文档
     *
     * @return 查询到的文档列表
     * @throws RuntimeException 当发生MongoDB操作异常或其他异常时抛出
     */
    @Override
    public List<org.springframework.ai.document.Document> get() {
        validateConfiguration();
        Query query = buildQuery();
        return processDocuments(query);
    }

    /**
     * 根据配置构建MongoDB查询对象
     *
     * @return 构建的查询对象
     * @throws RuntimeException 当查询字符串解析失败时抛出
     */
    private Query buildQuery() {
        Query query = new Query();
        if (StringUtils.hasText(properties.getQuery())) {
            try {
                Document queryDoc = Document.parse(properties.getQuery());
                query = new BasicQuery(queryDoc);
            } catch (Exception e) {
                throw new RuntimeException("解析查询字符串失败: " + e.getMessage(), e);
            }
        }
        return query;
    }

    /**
     * 处理MongoDB查询并转换文档格式
     */
    private List<org.springframework.ai.document.Document> processDocuments(Query query) {
        return executeWithMetrics("processDocuments", () ->
                StreamSupport.stream(
                                mongoTemplate.find(query, Document.class, properties.getCollection())
                                        .spliterator(), false)
                        .map(doc -> documentConverter.convert(doc, properties.getDatabase(),
                                properties.getCollection(), properties)
                        )
                        .collect(Collectors.toList())
        );
    }

    /**
     * 在指定数据库和集合中执行查询
     *
     * @param database   要查询的数据库名
     * @param collection 要查询的集合名
     * @param query      查询条件
     * @return 查询结果文档列表
     */
    public List<org.springframework.ai.document.Document> findInDatabaseAndCollection(String database, String collection, Query query) {
        Assert.hasText(collection, "Collection name must not be empty");
        Assert.hasText(database, "Database name must not be empty");
        Assert.notNull(query, "Query must not be null");

        return executeWithMetrics("findInDatabaseAndCollection", () -> {
            List<org.springframework.ai.document.Document> results = StreamSupport.stream(
                            mongoTemplate.getMongoDatabaseFactory()
                                    .getMongoDatabase(database)
                                    .getCollection(collection, Document.class)
                                    .find(query.getQueryObject())
                                    .spliterator(), false)
                    .map(doc -> documentConverter.convert(doc, database, collection, properties))
                    .collect(Collectors.toList());

            return results;
        });
    }

    /**
     * 并行查询多个集合
     */
    public List<org.springframework.ai.document.Document> findInDatabaseAndCollectionParallel(
            String database, List<String> collections, Query query) {
        Assert.notEmpty(collections, "Collections list must not be empty");
        Assert.hasText(database, "Database name must not be empty");
        Assert.notNull(query, "Query must not be null");

        return collections.parallelStream()
                .map(collection -> findInDatabaseAndCollection(database, collection, query))
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /**
     * 在指定集合中执行分页查询
     *
     * @param collection 要查询的集合名
     * @param query      查询条件
     * @param page       页码（从0开始）
     * @param size       每页大小
     * @return 分页查询结果文档列表
     */
    public List<org.springframework.ai.document.Document> findInDatabaseAndCollectionWithPagination(
            String collection, Query query, int page, int size) {
        return findInDatabaseAndCollectionWithPagination(properties.getDatabase(), collection, query, page, size);
    }

    /**
     * 在指定数据库和集合中执行分页查询
     *
     * @param database   要查询的数据库名
     * @param collection 要查询的集合名
     * @param query      查询条件
     * @param page       页码（从0开始）
     * @param size       每页大小
     * @return 分页查询结果文档列表
     */
    public List<org.springframework.ai.document.Document> findInDatabaseAndCollectionWithPagination(
            String database, String collection, Query query, int page, int size) {
        Assert.hasText(collection, "Collection name must not be empty");
        Assert.hasText(database, "Database name must not be empty");
        Assert.notNull(query, "Query must not be null");
        Assert.isTrue(page >= 0, "Page index must not be negative");
        Assert.isTrue(size > 0, "Page size must be greater than 0");

        String cacheKey = String.format("%s:%s:%s:page=%d:size=%d",
                database, collection, query.toString(), page, size);


        return executeWithMetrics("findInDatabaseAndCollectionWithPagination", () -> {
            List<org.springframework.ai.document.Document> results = StreamSupport.stream(
                            mongoTemplate.getMongoDatabaseFactory()
                                    .getMongoDatabase(database)
                                    .getCollection(collection, Document.class)
                                    .find(query.getQueryObject())
                                    .skip(page * size)
                                    .limit(size)
                                    .spliterator(), false)
                    .map(doc -> documentConverter.convert(doc, database, collection, properties))
                    .collect(Collectors.toList());


            return results;
        });
    }

//    /**
//     * 使用重试模板执行操作
//     */
//    private <T> T executeWithRetry(String operation, Supplier<T> supplier) {
//        if (retryTemplate != null) {
//            return retryTemplate.execute(context -> {
//                log.debug("Attempting operation: {} (Attempt: {})",
//                        operation, context.getRetryCount() + 1);
//                return supplier.get();
//            });
//        }
//        return supplier.get();
//    }


}
