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
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for ElasticsearchDocumentReader.
 * Uses testcontainers to spin up a temporary Elasticsearch instance for testing.
 *
 * @author xiadong
 * @since 0.0.1
 */
public class ElasticsearchDocumentReaderTest {

    private static final String TEST_INDEX = "test-index";
    private static final String TEST_DOC_ID = "test-doc-1";
    private static ElasticsearchContainer container;
    private static ElasticsearchClient client;
    private static ElasticsearchDocumentReader reader;

    @BeforeAll
    static void setUp() throws IOException {
        // Start Elasticsearch container
        container = new ElasticsearchContainer(
                DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.12.1"));
        container.start();

        // Create client
        RestClient restClient = RestClient.builder(HttpHost.create(container.getHttpHostAddress())).build();
        client = new ElasticsearchClient(new RestClientTransport(restClient, new JacksonJsonpMapper()));

        // Create test index
        CreateIndexResponse createResponse = client.indices().create(c -> c.index(TEST_INDEX));
        assertThat(createResponse.acknowledged()).isTrue();

        // Configure and create reader
        ElasticsearchConfig config = new ElasticsearchConfig();
        config.setHost(container.getHost());
        config.setPort(container.getMappedPort(9200));
        config.setIndex(TEST_INDEX);
        config.setQueryField("content");
        reader = new ElasticsearchDocumentReader(config);

        // Index test documents
        indexTestDocuments();
    }

    @AfterAll
    static void tearDown() throws IOException {
        if (client != null) {
            DeleteIndexResponse deleteResponse = client.indices().delete(c -> c.index(TEST_INDEX));
            assertThat(deleteResponse.acknowledged()).isTrue();
        }
        if (container != null) {
            container.stop();
        }
    }

    @Test
    void testReadAllDocuments() {
        List<Document> documents = reader.read();
        assertThat(documents).hasSize(2);
        assertThat(documents.get(0).getContent()).contains("test document");
    }

    @Test
    void testReadWithQuery() {
        List<Document> documents = reader.readWithQuery("second");
        assertThat(documents).hasSize(1);
        assertThat(documents.get(0).getContent()).contains("second test");
    }

    @Test
    void testGetDocument() {
        Document document = reader.get(TEST_DOC_ID);
        assertThat(document).isNotNull();
        assertThat(document.getContent()).contains("test document");
        assertThat(document.getMetadata()).containsEntry("title", "Test 1");
    }

    @Test
    void testGetNonExistentDocument() {
        Document document = reader.get("non-existent-id");
        assertThat(document).isNull();
    }

    private static void indexTestDocuments() throws IOException {
        // First document
        Map<String, Object> doc1 = new HashMap<>();
        doc1.put("content", "This is a test document");
        doc1.put("title", "Test 1");
        client.index(i -> i
                .index(TEST_INDEX)
                .id(TEST_DOC_ID)
                .document(doc1));

        // Second document
        Map<String, Object> doc2 = new HashMap<>();
        doc2.put("content", "This is a second test document");
        doc2.put("title", "Test 2");
        client.index(i -> i
                .index(TEST_INDEX)
                .document(doc2));

        // Refresh index to make documents searchable
        client.indices().refresh();
    }
} 