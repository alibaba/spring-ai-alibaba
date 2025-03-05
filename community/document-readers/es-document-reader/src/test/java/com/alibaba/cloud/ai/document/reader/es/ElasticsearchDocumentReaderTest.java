/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.document.reader.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.document.Document;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Test class for ElasticsearchDocumentReader. Uses local Elasticsearch instance for
 * testing with HTTPS.
 *
 * @author xiadong
 * @since 0.0.1
 */
@EnabledIfEnvironmentVariable(named = "ES_HOST", matches = ".+")
public class ElasticsearchDocumentReaderTest {

	private static final String TEST_INDEX = "spring-ai-test";

	private static final String TEST_DOC_ID = "1";

	// Get ES configuration from environment variables, use defaults if not set
	private static final String ES_HOST = System.getenv("ES_HOST") != null ? System.getenv("ES_HOST") : "localhost";

	private static final int ES_PORT = System.getenv("ES_PORT") != null ? Integer.parseInt(System.getenv("ES_PORT"))
			: 9200;

	private static final String ES_USERNAME = System.getenv("ES_USERNAME") != null ? System.getenv("ES_USERNAME")
			: "elastic";

	private static final String ES_PASSWORD = System.getenv("ES_PASSWORD") != null ? System.getenv("ES_PASSWORD")
			: "r-tooRd7RgrX_uZV0klZ";

	private static final String ES_SCHEME = System.getenv("ES_SCHEME") != null ? System.getenv("ES_SCHEME") : "https";

	private static ElasticsearchClient client;

	private static ElasticsearchDocumentReader reader;

	private static ElasticsearchDocumentReader clusterReader;

	// Flag to indicate if ES is available
	private static boolean esAvailable = false;

	static {
		if (System.getenv("ES_HOST") == null) {
			System.out.println("ES_HOST environment variable is not set. Tests will be skipped.");
		}
	}

	/**
	 * Check if Elasticsearch is available
	 * @return true if ES is available, false otherwise
	 */
	public static boolean isElasticsearchAvailable() {
		return esAvailable;
	}

	/**
	 * Try to connect to Elasticsearch
	 * @return true if connection successful, false otherwise
	 */
	private static boolean canConnectToElasticsearch() {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(ES_HOST, ES_PORT), 1000);
			return true;
		}
		catch (Exception e) {
			System.out.println("Cannot connect to Elasticsearch: " + e.getMessage());
			return false;
		}
	}

	@BeforeAll
	static void setUp() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
		// Check if ES_HOST environment variable is set
		String esHost = System.getenv("ES_HOST");
		assumeTrue(esHost != null && !esHost.isEmpty(),
				"Skipping test because ES_HOST environment variable is not set");

		// Check if we can connect to ES
		esAvailable = canConnectToElasticsearch();

		// Skip setup if ES is not available
		if (!esAvailable) {
			System.out
				.println("Skipping Elasticsearch tests because ES server is not available: " + ES_HOST + ":" + ES_PORT);
			return;
		}

		try {
			// Create SSL context that trusts all certificates
			SSLContext sslContext = SSLContextBuilder.create()
				.loadTrustMaterial(null, (chains, authType) -> true)
				.build();

			// Create client with authentication and SSL
			CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			credentialsProvider.setCredentials(AuthScope.ANY,
					new UsernamePasswordCredentials(ES_USERNAME, ES_PASSWORD));

			RestClient restClient = RestClient.builder(new HttpHost(ES_HOST, ES_PORT, ES_SCHEME))
				.setHttpClientConfigCallback(
						httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
							.setSSLContext(sslContext)
							.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE))
				.build();

			client = new ElasticsearchClient(new RestClientTransport(restClient, new JacksonJsonpMapper()));

			// Delete index if exists
			boolean indexExists = client.indices().exists(e -> e.index(TEST_INDEX)).value();
			if (indexExists) {
				DeleteIndexResponse deleteResponse = client.indices().delete(c -> c.index(TEST_INDEX));
				assertThat(deleteResponse.acknowledged()).isTrue();
			}

			// Create test index with mapping
			CreateIndexResponse createResponse = client.indices()
				.create(c -> c.index(TEST_INDEX)
					.mappings(m -> m.properties("content", p -> p.text(t -> t.analyzer("standard")))
						.properties("title", p -> p.keyword(k -> k))));
			assertThat(createResponse.acknowledged()).isTrue();

			// Configure and create single node reader
			ElasticsearchConfig config = new ElasticsearchConfig();
			config.setHost(ES_HOST);
			config.setPort(ES_PORT);
			config.setIndex(TEST_INDEX);
			config.setQueryField("content");
			config.setUsername(ES_USERNAME);
			config.setPassword(ES_PASSWORD);
			config.setScheme(ES_SCHEME);
			reader = new ElasticsearchDocumentReader(config);

			// Configure and create cluster reader
			ElasticsearchConfig clusterConfig = new ElasticsearchConfig();
			clusterConfig.setNodes(Arrays.asList(ES_HOST + ":" + ES_PORT, ES_HOST + ":9201", ES_HOST + ":9202"));
			clusterConfig.setIndex(TEST_INDEX);
			clusterConfig.setQueryField("content");
			clusterConfig.setUsername(ES_USERNAME);
			clusterConfig.setPassword(ES_PASSWORD);
			clusterConfig.setScheme(ES_SCHEME);
			clusterReader = new ElasticsearchDocumentReader(clusterConfig);

			// Index test documents
			indexTestDocuments();
		}
		catch (Exception e) {
			System.out.println("Failed to set up Elasticsearch test environment: " + e.getMessage());
			esAvailable = false;
		}
	}

	@AfterAll
	static void tearDown() throws IOException {
		// Skip cleanup if ES is not available or client is null
		if (!esAvailable || client == null) {
			return;
		}

		try {
			DeleteIndexResponse deleteResponse = client.indices().delete(c -> c.index(TEST_INDEX));
			assertThat(deleteResponse.acknowledged()).isTrue();
		}
		catch (Exception e) {
			System.out.println("Failed to clean up Elasticsearch test environment: " + e.getMessage());
		}
	}

	@Test
	@EnabledIf("isElasticsearchAvailable")
	void testGet() {
		List<Document> documents = reader.get();
		assertThat(documents).hasSize(3);
		assertThat(documents.get(0).getText()).contains("Spring Framework");
		assertThat(documents.get(0).getMetadata()).containsKey("title");
	}

	@Test
	@EnabledIf("isElasticsearchAvailable")
	void testGetWithCluster() {
		List<Document> documents = clusterReader.get();
		assertThat(documents).hasSize(3);
		assertThat(documents.get(0).getText()).contains("Spring Framework");
		assertThat(documents.get(0).getMetadata()).containsKey("title");
	}

	@Test
	@EnabledIf("isElasticsearchAvailable")
	void testGetById() {
		Document document = reader.getById(TEST_DOC_ID);
		assertThat(document).isNotNull();
		assertThat(document.getText()).contains("Spring Framework");
		assertThat(document.getMetadata()).containsEntry("title", "Spring Introduction");
	}

	@Test
	@EnabledIf("isElasticsearchAvailable")
	void testGetByIdWithCluster() {
		Document document = clusterReader.getById(TEST_DOC_ID);
		assertThat(document).isNotNull();
		assertThat(document.getText()).contains("Spring Framework");
		assertThat(document.getMetadata()).containsEntry("title", "Spring Introduction");
	}

	@Test
	@EnabledIf("isElasticsearchAvailable")
	void testGetByIdNonExistent() {
		Document document = reader.getById("non-existent-id");
		assertThat(document).isNull();
	}

	@Test
	@EnabledIf("isElasticsearchAvailable")
	void testReadWithQuery() {
		List<Document> documents = reader.readWithQuery("spring");
		assertThat(documents).hasSize(2);
		documents.forEach(doc -> {
			assertThat(doc.getText().toLowerCase()).contains("spring");
		});
	}

	@Test
	@EnabledIf("isElasticsearchAvailable")
	void testReadWithQueryWithCluster() {
		List<Document> documents = clusterReader.readWithQuery("spring");
		assertThat(documents).hasSize(2);
		documents.forEach(doc -> {
			assertThat(doc.getText().toLowerCase()).contains("spring");
		});
	}

	private static void indexTestDocuments() throws IOException {
		// First document
		Map<String, Object> doc1 = new HashMap<>();
		doc1.put("content", "Spring Framework is the most popular application development framework for Java.");
		doc1.put("title", "Spring Introduction");
		client.index(i -> i.index(TEST_INDEX).id(TEST_DOC_ID).document(doc1));

		// Second document
		Map<String, Object> doc2 = new HashMap<>();
		doc2.put("content",
				"Spring Boot makes it easy to create stand-alone, production-grade Spring based Applications.");
		doc2.put("title", "Spring Boot Guide");
		client.index(i -> i.index(TEST_INDEX).document(doc2));

		// Third document
		Map<String, Object> doc3 = new HashMap<>();
		doc3.put("content", "Java is a popular programming language and platform.");
		doc3.put("title", "Java Programming");
		client.index(i -> i.index(TEST_INDEX).document(doc3));

		// Refresh index to make documents searchable
		client.indices().refresh();
	}

}
