/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.document.reader.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A DocumentReader implementation that reads documents from Elasticsearch.
 * Supports basic authentication and customizable query field.
 *
 * @author xiadong
 * @since 0.0.1
 */
public class ElasticsearchDocumentReader implements DocumentReader {

    private final ElasticsearchConfig config;
    private final ElasticsearchClient client;

    /**
     * Constructor that initializes the Elasticsearch client with the provided configuration.
     *
     * @param config The Elasticsearch configuration
     */
    public ElasticsearchDocumentReader(ElasticsearchConfig config) {
        this.config = config;
        this.client = createClient();
    }

    @Override
    public List<Document> get() {
        try {
            // Get all documents
            SearchResponse<Map> response = client.search(s -> s
                    .index(config.getIndex())
                    .query(q -> q
                            .matchAll(m -> m))
                    .size(config.getMaxResults()), Map.class);

            List<Document> documents = new ArrayList<>();
            response.hits().hits().forEach(hit -> {
                Map<String, Object> source = hit.source();
                if (source != null) {
                    Document document = new Document(
                            source.getOrDefault(config.getQueryField(), "").toString(),
                            source
                    );
                    documents.add(document);
                }
            });
            return documents;
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to get documents from Elasticsearch", e);
        }
    }

    /**
     * Get a document by its ID.
     *
     * @param id The document ID
     * @return The document if found, null otherwise
     */
    public Document getById(String id) {
        try {
            var response = client.get(g -> g
                    .index(config.getIndex())
                    .id(id), Map.class);

            if (!response.found() || response.source() == null) {
                return null;
            }

            return new Document(
                    response.source().getOrDefault(config.getQueryField(), "").toString(),
                    response.source()
            );
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to get document from Elasticsearch with id: " + id, e);
        }
    }

    /**
     * Read documents matching the specified query.
     *
     * @param query The search query
     * @return List of matching documents
     */
    public List<Document> readWithQuery(String query) {
        try {
            // Build the search request with query
            SearchResponse<Map> response = client.search(s -> s
                    .index(config.getIndex())
                    .query(q -> q
                            .match(new MatchQuery.Builder()
                                    .field(config.getQueryField())
                                    .query(query)
                                    .build()))
                    .size(config.getMaxResults()), Map.class);

            List<Document> documents = new ArrayList<>();
            response.hits().hits().forEach(hit -> {
                Map<String, Object> source = hit.source();
                if (source != null) {
                    Document document = new Document(
                            source.getOrDefault(config.getQueryField(), "").toString(),
                            source
                    );
                    documents.add(document);
                }
            });
            return documents;
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to read documents from Elasticsearch with query: " + query, e);
        }
    }

    private ElasticsearchClient createClient() {
        // Create RestClient configuration
        HttpHost httpHost = new HttpHost(config.getHost(), config.getPort());
        RestClientBuilder restClientBuilder = RestClient.builder(httpHost);

        // Add authentication if credentials are provided
        if (StringUtils.hasText(config.getUsername()) && StringUtils.hasText(config.getPassword())) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(config.getUsername(), config.getPassword()));

            restClientBuilder.setHttpClientConfigCallback(
                    (HttpAsyncClientBuilder clientBuilder) -> clientBuilder
                            .setDefaultCredentialsProvider(credentialsProvider));
        }

        // Create the transport and client
        ElasticsearchTransport transport = new RestClientTransport(
                restClientBuilder.build(), new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}