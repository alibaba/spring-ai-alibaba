/*
* Copyright 2024 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.alibaba.cloud.ai.example.rag.local;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.*;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import com.alibaba.cloud.ai.advisor.RetrievalRerankAdvisor;
import com.alibaba.cloud.ai.example.rag.RagService;
import com.alibaba.cloud.ai.model.RerankModel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.autoconfigure.vectorstore.elasticsearch.ElasticsearchVectorStoreProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* Title Local rag service.<br>
* Description Local rag service.<br>
*
* @author yuanci.ytb
* @since 1.0.0-M2
*/

@Service()
public class LocalRagService implements RagService {

    private static final Logger logger = LoggerFactory.getLogger(LocalRagService.class);

    private static final String textField = "content";

    private static final String vectorField = "embedding";

    @Value("classpath:/data/spring_ai_alibaba_quickstart.pdf")
    private Resource springAiResource;

    @Value("classpath:/prompts/system-qa.st")
    private Resource systemResource;

    private final ChatModel chatModel;

    private final VectorStore vectorStore;

    private final RerankModel rerankModel;

    private final ElasticsearchClient elasticsearchClient;

    private final ElasticsearchVectorStoreProperties options;

    public LocalRagService(ChatModel chatModel, VectorStore vectorStore, RerankModel rerankModel,
                           ElasticsearchClient elasticsearchClient, ElasticsearchVectorStoreProperties options) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
        this.rerankModel = rerankModel;
        this.elasticsearchClient = elasticsearchClient;
        this.options = options;
    }

    @Override
    public void importDocuments() {
        // 1. parse document
        DocumentReader reader = new PagePdfDocumentReader(springAiResource);
        List<Document> documents = reader.get();
        logger.info("{} documents loaded", documents.size());

        // 2. split trunks
        List<Document> splitDocuments = new TokenTextSplitter().apply(documents);
        logger.info("{} documents split", splitDocuments.size());

        // 3. create embedding and store to vector store
        logger.info("create embedding and save to vector store");
        createIndexIfNotExists();
        vectorStore.add(splitDocuments);
    }

    public Flux<ChatResponse> retrieve(String message) {
        // Enable hybrid search, both embedding and full text search
        SearchRequest searchRequest = SearchRequest.defaults()
                .withFilterExpression(new FilterExpressionBuilder().eq(textField, message).build());

        // Step3 - Retrieve and llm generate
        String promptTemplate = getPromptTemplate(systemResource);
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new RetrievalRerankAdvisor(vectorStore, rerankModel, searchRequest, promptTemplate, 0.1))
                .build();

        return chatClient.prompt().user(message).stream().chatResponse();
    }

    private void createIndexIfNotExists() {
        try {
            String indexName = options.getIndexName();
            Integer dimsLength = options.getDimensions();

            if (StringUtils.isBlank(indexName)) {
                throw new IllegalArgumentException("Elastic search index name must be provided");
            }

            boolean exists = elasticsearchClient.indices().exists(idx -> idx.index(indexName)).value();
            if (exists) {
                logger.debug("Index {} already exists. Skipping creation.", indexName);
                return;
            }

            String similarityAlgo = options.getSimilarity().name();
            IndexSettings indexSettings = IndexSettings
                    .of(settings -> settings.numberOfShards(String.valueOf(1)).numberOfReplicas(String.valueOf(1)));

            // Maybe using json directly?
            Map<String, Property> properties = new HashMap<>();
            properties.put(vectorField, Property.of(property -> property.denseVector(
                    DenseVectorProperty.of(dense -> dense.index(true).dims(dimsLength).similarity(similarityAlgo)))));
            properties.put(textField, Property.of(property -> property.text(TextProperty.of(t -> t))));

            Map<String, Property> metadata = new HashMap<>();
            metadata.put("ref_doc_id", Property.of(property -> property.keyword(KeywordProperty.of(k -> k))));

            properties.put("metadata",
                    Property.of(property -> property.object(ObjectProperty.of(op -> op.properties(metadata)))));

            CreateIndexResponse indexResponse = elasticsearchClient.indices()
                    .create(createIndexBuilder -> createIndexBuilder.index(indexName)
                            .settings(indexSettings)
                            .mappings(TypeMapping.of(mappings -> mappings.properties(properties))));

            if (!indexResponse.acknowledged()) {
                throw new RuntimeException("failed to create index");
            }

            logger.info("create elasticsearch index {} successfully", indexName);
        }
        catch (IOException e) {
            logger.error("failed to create index", e);
            throw new RuntimeException(e);
        }
    }

    private String getPromptTemplate(Resource systemResource) {
        try {
            return systemResource.getContentAsString(StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}