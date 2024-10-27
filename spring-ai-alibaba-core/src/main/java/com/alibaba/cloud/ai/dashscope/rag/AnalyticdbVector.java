package com.alibaba.cloud.ai.dashscope.rag;

import com.alibaba.cloud.ai.autoconfigure.redis.GetBeanUtil;
import com.alibaba.cloud.ai.autoconfigure.redis.RedisClientService;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.gpdb20160503.Client;
import com.aliyun.gpdb20160503.models.CreateCollectionRequest;
import com.aliyun.gpdb20160503.models.CreateNamespaceRequest;
import com.aliyun.gpdb20160503.models.DeleteCollectionDataRequest;
import com.aliyun.gpdb20160503.models.DeleteCollectionDataResponse;
import com.aliyun.gpdb20160503.models.DeleteCollectionRequest;
import com.aliyun.gpdb20160503.models.DescribeCollectionRequest;
import com.aliyun.gpdb20160503.models.DescribeNamespaceRequest;
import com.aliyun.gpdb20160503.models.InitVectorDatabaseRequest;
import com.aliyun.gpdb20160503.models.QueryCollectionDataRequest;
import com.aliyun.gpdb20160503.models.QueryCollectionDataResponse;
import com.aliyun.gpdb20160503.models.QueryCollectionDataResponseBody;
import com.aliyun.gpdb20160503.models.UpsertCollectionDataRequest;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.tea.TeaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSON;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author HeYQ
 * @version 1.0
 * @ date 2024-10-23 20:29
 * @ describe
 */
public class AnalyticdbVector implements VectorStore {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticdbVector.class);

    private static volatile AnalyticdbVector instance;

    private static Boolean initialized = false;

    private final String collectionName;

    private AnalyticdbConfig config;

    private Client client;

    private RedisClientService redisClientService ;

    public static AnalyticdbVector getInstance(String collectionName, AnalyticdbConfig config) throws Exception {
        if (instance == null) {
            synchronized (AnalyticdbVector.class) {
                if (instance == null) {
                    instance = new AnalyticdbVector(collectionName, config);
                }
            }
        }
        return instance;
    }

    private AnalyticdbVector(String collectionName, AnalyticdbConfig config) throws Exception {
         // collection_name must be updated every time
        this.collectionName = collectionName.toLowerCase();
        if (AnalyticdbVector.initialized) {
            return;
        }
        this.config = config;
        this.redisClientService = GetBeanUtil.getBean(RedisClientService.class);
        try{
            Config clientConfig = Config.build(this.config.toAnalyticdbClientParams());
            this.client = new Client(clientConfig);
        }catch(Exception e){
           logger.debug("create Analyticdb client error", e);
        }
        initialize();
        AnalyticdbVector.initialized  = Boolean.TRUE;
    }

    /**
     * initialize vector db
     */
    private void initialize() throws Exception {
        initializeVectorDataBase();
        createNameSpaceIfNotExists();
    }

    private void initializeVectorDataBase() {
        InitVectorDatabaseRequest request = new InitVectorDatabaseRequest()
                .setDBInstanceId(config.getDBInstanceId())
                .setRegionId(config.getRegionId())
                .setManagerAccount(config.getManagerAccount())
                .setManagerAccountPassword(config.getManagerAccountPassword());
        try{
            client.initVectorDatabase(request);
        }catch(Exception e){
           logger.error("init Vector data base error",e);
        }
    }

    private void createNameSpaceIfNotExists() throws Exception {
        try{
            DescribeNamespaceRequest request = new DescribeNamespaceRequest()
                    .setDBInstanceId(this.config.getDBInstanceId())
                    .setRegionId(this.config.getRegionId())
                    .setNamespace(this.config.getNamespace())
                   .setManagerAccount(this.config.getManagerAccount())
                   .setManagerAccountPassword(this.config.getManagerAccountPassword());
            this.client.describeNamespace(request);
        }catch(TeaException e){
           if (Objects.equals(e.getStatusCode(), 404)){
               CreateNamespaceRequest request = new CreateNamespaceRequest()
                       .setDBInstanceId(this.config.getDBInstanceId())
                       .setRegionId(this.config.getRegionId())
                       .setNamespace(this.config.getNamespace())
                       .setManagerAccount(this.config.getManagerAccount())
                       .setManagerAccountPassword(this.config.getManagerAccountPassword())
                       .setNamespacePassword(this.config.getNamespacePassword());
               this.client.createNamespace(request);
           }else {
               throw new Exception("failed to create namespace:{}", e);
           }
        }
    }

    private void createCollectionIfNotExists(Long embeddingDimension) {
        String cacheKey = "vector_indexing_" + this.collectionName;
        String lockName = cacheKey + "_lock";
        try {
           boolean lock  = redisClientService.lock(lockName, 20);// Acquire the lock
           if(lock) {
               // Check if the collection exists in the cache
               if ("1".equals(redisClientService.getValueByKey(cacheKey))) {
                   redisClientService.deleteLock(lockName);
                   return;
               }
               // Describe the collection to check if it exists
               DescribeCollectionRequest describeRequest = new DescribeCollectionRequest()
                       .setDBInstanceId(this.config.getDBInstanceId())
                       .setRegionId(this.config.getRegionId())
                       .setNamespace(this.config.getNamespace())
                       .setNamespacePassword(this.config.getNamespacePassword())
                       .setCollection(this.collectionName);
               try {
                   this.client.describeCollection(describeRequest);
               } catch (TeaException e) {
                   if (e.getStatusCode() == 404) {
                       // Collection does not exist, create it
                       String metadata = JSON.toJSONString(new JSONObject()
                               .fluentPut("refDocId", "text")
                               .fluentPut("content", "text")
                               .fluentPut("metadata", "jsonb"));

                       String fullTextRetrievalFields = "content";
                       CreateCollectionRequest createRequest = new CreateCollectionRequest()
                               .setDBInstanceId(this.config.getDBInstanceId())
                               .setRegionId(this.config.getRegionId())
                               .setManagerAccount(this.config.getManagerAccount())
                               .setManagerAccountPassword(this.config.getManagerAccountPassword())
                               .setNamespace(this.config.getNamespace())
                               .setCollection(this.collectionName)
                               .setDimension(embeddingDimension)
                               .setMetrics(this.config.getMetrics())
                               .setMetadata(metadata)
                               .setFullTextRetrievalFields(fullTextRetrievalFields);
                       this.client.createCollection(createRequest);
                   } else {
                       throw new RuntimeException("Failed to create collection " + this.collectionName + ": " + e.getMessage());
                   }
                  // Set the cache key to indicate the collection exists
                  redisClientService.setKeyValue(cacheKey, "1", 3600);
               }
           }
        }catch(Exception e){
            redisClientService.deleteLock(lockName);
            throw new RuntimeException("Failed to create collection " + this.collectionName + ": " + e.getMessage());
         }finally {
            redisClientService.deleteLock(lockName);
        }
    }

    public void create(List<Document> texts, List<List<Double>> embeddings) throws Exception {
        long dimension = embeddings.get(0).size();
        createCollectionIfNotExists(dimension);
        addTexts(texts, embeddings);
    }

    @Override
    public void add(List<Document> texts) {
        try {
            createCollectionIfNotExists(this.config.getEmbeddingDimension());
            addTexts(texts);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addTexts(List<Document> documents, List<List<Double>> embeddings) throws Exception {
        List<UpsertCollectionDataRequest.UpsertCollectionDataRequestRows> rows = new ArrayList<>(10);
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            List<Double> embedding = embeddings.get(i);

            Map<String, String> metadata = new HashMap<>();
            metadata.put("id", (String) doc.getMetadata().get("id"));
            metadata.put("content", doc.getContent());
            metadata.put("metadata", JSON.toJSONString(doc.getMetadata()));
            rows.add(new UpsertCollectionDataRequest.UpsertCollectionDataRequestRows().setVector(embedding).setMetadata(metadata));
        }
        UpsertCollectionDataRequest request = new UpsertCollectionDataRequest()
                .setDBInstanceId(this.config.getDBInstanceId())
                .setRegionId(this.config.getRegionId())
                .setNamespace(this.config.getNamespace())
                .setNamespacePassword(this.config.getNamespacePassword())
                .setCollection(this.collectionName)
                .setRows(rows);
        this.client.upsertCollectionData(request);
    }

    public void addTexts(List<Document> documents) throws Exception {
        List<UpsertCollectionDataRequest.UpsertCollectionDataRequestRows> rows = new ArrayList<>(10);
        for (Document doc : documents) {
            float[] floatEmbeddings = doc.getEmbedding();
            List<Double> embedding = new ArrayList<>(floatEmbeddings.length);
            for (float floatEmbedding : floatEmbeddings) {
                embedding.add((double) floatEmbedding);
            }
            Map<String, String> metadata = new HashMap<>();
            metadata.put("refDocId", (String) doc.getMetadata().get("docId"));
            metadata.put("content", doc.getContent());
            metadata.put("metadata", JSON.toJSONString(doc.getMetadata()));
            rows.add(new UpsertCollectionDataRequest.UpsertCollectionDataRequestRows().setVector(embedding).setMetadata(metadata));
        }
        UpsertCollectionDataRequest request = new UpsertCollectionDataRequest()
                .setDBInstanceId(this.config.getDBInstanceId())
                .setRegionId(this.config.getRegionId())
                .setNamespace(this.config.getNamespace())
                .setNamespacePassword(this.config.getNamespacePassword())
                .setCollection(this.collectionName)
                .setRows(rows);
        this.client.upsertCollectionData(request);
    }

    public boolean textExists(String id) {
        QueryCollectionDataRequest request = new QueryCollectionDataRequest()
                .setDBInstanceId(this.config.getDBInstanceId())
                .setRegionId(this.config.getRegionId())
                .setNamespace(this.config.getNamespace())
                .setNamespacePassword(this.config.getNamespacePassword())
                .setCollection(this.collectionName)
                .setMetrics(this.config.getMetrics())
                .setIncludeValues(true)
                .setVector(null)
                .setContent(null)
                .setTopK(1L)
                .setFilter("refDocId='" + id + "'");

        try {
            QueryCollectionDataResponse response = this.client.queryCollectionData(request);
            return response.getBody().getMatches().getMatch().size() > 0;
        } catch (Exception e) {
            throw new RuntimeException("Failed to query collection data: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Boolean> delete(List<String> ids) {
        if (ids.isEmpty()) {
            return Optional.of(false);
        }
        String idsStr = ids.stream()
                .map(id -> "'" + id + "'")
                .collect(Collectors.joining(", ", "(", ")"));
        DeleteCollectionDataRequest request = new DeleteCollectionDataRequest()
                .setDBInstanceId(this.config.getDBInstanceId())
                .setRegionId(this.config.getRegionId())
                .setNamespace(this.config.getNamespace())
                .setNamespacePassword(this.config.getNamespacePassword())
                .setCollection(this.collectionName)
                .setCollectionData(null)
                .setCollectionDataFilter("refDocId IN " + idsStr);
        try {
            DeleteCollectionDataResponse deleteCollectionDataResponse = this.client.deleteCollectionData(request);
            return deleteCollectionDataResponse.statusCode.equals(200) ? Optional.of(true) : Optional.of(false);
            // Handle response if needed
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete collection data by IDs: " + e.getMessage(), e);
        }
    }

    public void deleteByMetadataField(String key, String value) {
        DeleteCollectionDataRequest request = new DeleteCollectionDataRequest()
                .setDBInstanceId(this.config.getDBInstanceId())
                .setRegionId(this.config.getRegionId())
                .setNamespace(this.config.getNamespace())
                .setNamespacePassword(this.config.getNamespacePassword())
                .setCollection(this.collectionName)
                .setCollectionData(null)
                .setCollectionDataFilter("metadata ->> '" + key + "' = '" + value + "'");
        try {
            this.client.deleteCollectionData(request);
            // Handle response if needed
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete collection data by metadata field: " + e.getMessage(), e);
        }
    }


    public List<Document> searchByVector(List<Double> queryVector, Map<String, Object> kwargs) {
        Double scoreThreshold = (Double) kwargs.getOrDefault("scoreThreshold", 0.0d);
        Boolean includeValues = (Boolean) kwargs.getOrDefault("includeValues", true);
        Long topK = (Long) kwargs.getOrDefault("topK", 4);

        QueryCollectionDataRequest request = new QueryCollectionDataRequest()
                .setDBInstanceId(this.config.getDBInstanceId())
                .setRegionId(this.config.getRegionId())
                .setNamespace(this.config.getNamespace())
                .setNamespacePassword(this.config.getNamespacePassword())
                .setCollection(this.collectionName)
                .setIncludeValues(includeValues)
                .setMetrics(this.config.getMetrics())
                .setVector(queryVector)
                .setContent(null)
                .setTopK(topK)
                .setFilter(null);

        try {
            QueryCollectionDataResponse response = this.client.queryCollectionData(request);
            List<Document> documents = new ArrayList<>();
            for (QueryCollectionDataResponseBody.QueryCollectionDataResponseBodyMatchesMatch match : response.getBody().getMatches().getMatch()) {
                if (match.getScore() != null && match.getScore() > scoreThreshold) {
                    Map<String, String> metadata = match.getMetadata();
                    String pageContent = metadata.get("content");
                    Map<String, Object> metadataJson = JSONObject.parseObject(metadata.get("metadata"), HashMap.class);
                    Document doc = new Document(pageContent, metadataJson);
                    documents.add(doc);
                }
            }
            return documents;
        } catch (Exception e) {
            throw new RuntimeException("Failed to search by vector: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Document> similaritySearch(String query) {

        return similaritySearch(SearchRequest.query(query));

    }

    @Override
    public List<Document> similaritySearch(SearchRequest searchRequest) {
        double scoreThreshold = searchRequest.getSimilarityThreshold();
        boolean includeValues = searchRequest.hasFilterExpression();
        int topK = searchRequest.getTopK();

        QueryCollectionDataRequest request = new QueryCollectionDataRequest()
                .setDBInstanceId(this.config.getDBInstanceId())
                .setRegionId(this.config.getRegionId())
                .setNamespace(this.config.getNamespace())
                .setNamespacePassword(this.config.getNamespacePassword())
                .setCollection(this.collectionName)
                .setIncludeValues(includeValues)
                .setMetrics(this.config.getMetrics())
                .setVector(null)
                .setContent(searchRequest.query)
                .setTopK((long) topK)
                .setFilter(null);
        try {
            QueryCollectionDataResponse response = this.client.queryCollectionData(request);
            List<Document> documents = new ArrayList<>();
            for (QueryCollectionDataResponseBody.QueryCollectionDataResponseBodyMatchesMatch match : response.getBody().getMatches().getMatch()) {
                if (match.getScore() != null && match.getScore() > scoreThreshold) {
//                    System.out.println(match.getScore());
                    Map<String, String> metadata = match.getMetadata();
                    String pageContent = metadata.get("content");
                    Map<String, Object> metadataJson = JSONObject.parseObject(metadata.get("metadata"), HashMap.class);
                    Document doc = new Document(pageContent, metadataJson);
                    documents.add(doc);
                }
            }
            return documents;
        } catch (Exception e) {
            throw new RuntimeException("Failed to search by full text: " + e.getMessage(), e);
        }
    }

    public void deleteAll() {
        DeleteCollectionRequest request = new DeleteCollectionRequest()
                .setCollection(this.collectionName)
                .setDBInstanceId(this.config.getDBInstanceId())
                .setNamespace(this.config.getNamespace())
                .setNamespacePassword(this.config.getNamespacePassword())
                .setRegionId(this.config.getRegionId());

        try {
            this.client.deleteCollection(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete collection: " + e.getMessage(), e);
        }
    }

}
