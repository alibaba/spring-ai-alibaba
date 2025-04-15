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
package com.alibaba.cloud.ai.vectorstore.oceanbase;

import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.observation.DefaultVectorStoreObservationConvention;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationContext;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.oceanbase.OceanBaseCEContainer;
import org.testcontainers.utility.DockerLoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;

/**
 * OceanBase vector store test. This class tests adding, searching, and deleting documents
 * in OceanBase.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(named = "OCEANBASE_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "OCEANBASE_USERNAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "OCEANBASE_PASSWORD", matches = ".+")
class OceanBaseVectorTest {

	private static final String IMAGE = "oceanbase/oceanbase-ce:4.3.5.1-101000042025031818";

	private static final String HOSTNAME = "oceanbase_test";

	private static final int PORT = 2881;

	public static final Network NETWORK = Network.newNetwork();

	private static final String USERNAME = "root@test";

	private static final String PASSWORD = "";

	private static final String OCEANBASE_DATABASE = "test";

	private static final String OCEANBASE_DRIVER_CLASS = "com.oceanbase.jdbc.Driver";

	private Connection connection;

	private OceanBaseCEContainer oceanBaseContainer;

	private ApplicationContextRunner contextRunner;

	List<Document> documents = List.of(
			new Document("1", getText("classpath:spring.ai.txt"), Map.of("docId", "1", "spring", "great")),
			new Document("2", getText("classpath:time.shelter.txt"), Map.of("docId", "1")),
			new Document("3", getText("classpath:great.depression.txt"), Map.of("docId", "1", "depression", "bad")));

	@BeforeAll
	public void setUp() throws Exception {
		oceanBaseContainer = initOceanbaseContainer();
		Startables.deepStart(Stream.of(oceanBaseContainer)).join();
		initializeJdbcConnection(getJdbcUrl());
		createSchemaIfNeeded();
		contextRunner = initApplicationContextRunner();
		Awaitility.setDefaultPollInterval(2, TimeUnit.SECONDS);
		Awaitility.setDefaultPollDelay(Duration.ZERO);
		Awaitility.setDefaultTimeout(Duration.ofMinutes(1));
	}

	@AfterAll
	public void tearDown() {
		if (oceanBaseContainer != null) {
			oceanBaseContainer.stop();
		}
	}

	public static String getText(String uri) {
		var resource = new DefaultResourceLoader().getResource(uri);
		try {
			return resource.getContentAsString(StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public ApplicationContextRunner initApplicationContextRunner() {
		return new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(OceanBaseVectorStoreAutoConfiguration.class))
			.withUserConfiguration(Config.class)
			.withPropertyValues("spring.ai.vectorstore.oceanbase.url=" + getJdbcUrl(),
					"spring.ai.vectorstore.oceanbase.username=" + System.getenv("OCEANBASE_USERNAME"),
					"spring.ai.vectorstore.oceanbase.password=" + System.getenv("OCEANBASE_PASSWORD"),
					"spring.ai.vectorstore.oceanbase.tableName=" + System.getenv("OCEANBASE_TABLENAME"));
	}

	@Test
	public void addAndSearchTest() {
		this.contextRunner
			.withPropertyValues("spring.ai.vectorstore.oceanbase.url=" + getJdbcUrl(),
					"spring.ai.vectorstore.oceanbase.username=" + USERNAME,
					"spring.ai.vectorstore.oceanbase.password=" + PASSWORD,
					"spring.ai.vectorstore.oceanbase.tableName=" + OCEANBASE_DATABASE)
			.run(context -> {
				var properties = context.getBean(OceanBaseVectorStoreProperties.class);

				VectorStore vectorStore = context.getBean(VectorStore.class);
				TestObservationRegistry observationRegistry = context.getBean(TestObservationRegistry.class);

				assertThat(vectorStore).isInstanceOf(OceanBaseVectorStore.class);

				vectorStore.add(this.documents);

				Awaitility.await()
					.until(() -> vectorStore.similaritySearch(SearchRequest.builder().query("Spring").topK(1).build()),
							hasSize(1));

				ObservationTestUtil.assertObservationRegistry(observationRegistry, "oceanbase",
						VectorStoreObservationContext.Operation.ADD);
				observationRegistry.clear();

				List<Document> results = vectorStore
					.similaritySearch(SearchRequest.builder().query("Spring").topK(1).build());

				assertThat(results).hasSize(1);
				Document resultDoc = results.get(0);
				assertThat(resultDoc.getId()).isEqualTo(this.documents.get(0).getId());
				assertThat(resultDoc.getText()).contains(
						"Spring AI provides abstractions that serve as the foundation for developing AI applications.");
				assertThat(resultDoc.getMetadata()).hasSize(3);
				assertThat(resultDoc.getMetadata()).containsKeys("spring", "distance");

				ObservationTestUtil.assertObservationRegistry(observationRegistry, "oceanbase",
						VectorStoreObservationContext.Operation.QUERY);
				observationRegistry.clear();

				// Remove all documents from the store
				vectorStore.delete(this.documents.stream().map(Document::getId).toList());

				Awaitility.await()
					.until(() -> vectorStore.similaritySearch(SearchRequest.builder().query("Spring").topK(1).build()),
							hasSize(0));

				ObservationTestUtil.assertObservationRegistry(observationRegistry, "oceanbase",
						VectorStoreObservationContext.Operation.DELETE);
				observationRegistry.clear();
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		public TestObservationRegistry observationRegistry() {
			return TestObservationRegistry.create();
		}

		@Bean
		public EmbeddingModel embeddingModel() {
			return new TransformersEmbeddingModel();
		}

	}

	static class ObservationTestUtil {

		private ObservationTestUtil() {
		}

		public static void assertObservationRegistry(TestObservationRegistry observationRegistry,
				String vectorStoreProvider, VectorStoreObservationContext.Operation operation) {
			TestObservationRegistryAssert.assertThat(observationRegistry)
				.doesNotHaveAnyRemainingCurrentObservation()
				.hasObservationWithNameEqualTo(DefaultVectorStoreObservationConvention.DEFAULT_NAME)
				.that()
				.hasContextualNameEqualTo(vectorStoreProvider + " " + operation.value())
				.hasBeenStarted()
				.hasBeenStopped();
		}

	}

	private String getJdbcUrl() {
		return "jdbc:oceanbase://" + oceanBaseContainer.getHost() + ":" + oceanBaseContainer.getMappedPort(PORT) + "/"
				+ OCEANBASE_DATABASE;
	}

	private OceanBaseCEContainer initOceanbaseContainer() {
		return new OceanBaseCEContainer(IMAGE).withEnv("MODE", "slim")
			.withEnv("OB_DATAFILE_SIZE", "2G")
			.withNetwork(NETWORK)
			.withNetworkAliases(HOSTNAME)
			.withExposedPorts(PORT)
			.withImagePullPolicy(PullPolicy.alwaysPull())
			.waitingFor(Wait.forLogMessage(".*boot success!.*", 1))
			.withStartupTimeout(Duration.ofMinutes(5))
			.withLogConsumer(new Slf4jLogConsumer(DockerLoggerFactory.getLogger(IMAGE)));
	}

	private void initializeJdbcConnection(String jdbcUrl)
			throws SQLException, InstantiationException, IllegalAccessException {
		Driver driver = (Driver) loadDriverClass().newInstance();
		Properties props = new Properties();

		props.put("user", USERNAME);
		props.put("password", PASSWORD);

		if (oceanBaseContainer != null) {
			jdbcUrl = jdbcUrl.replace(HOSTNAME, oceanBaseContainer.getHost());
		}

		this.connection = driver.connect(jdbcUrl, props);
		connection.setAutoCommit(false);
	}

	private Class<?> loadDriverClass() {
		try {
			return Class.forName(OCEANBASE_DRIVER_CLASS);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to load driver class: " + OCEANBASE_DRIVER_CLASS, e);
		}
	}

	private void createSchemaIfNeeded() {
		String sql = "CREATE DATABASE IF NOT EXISTS " + OCEANBASE_DATABASE;
		executeSql(sql);
	}

	private void executeSql(String sql) {
		try {
			connection.prepareStatement(sql).executeUpdate();
		}
		catch (Exception e) {
			throw new RuntimeException("Fail to execute sql " + sql, e);
		}
	}

}
