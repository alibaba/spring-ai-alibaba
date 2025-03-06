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
package com.alibaba.cloud.ai.vectorstore.analyticdb;

import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;

/**
 * @author HeYQ
 */
@EnabledIfEnvironmentVariable(named = "ANALYTICDB_ACCESS_KEY_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "ANALYTICDB_ACCESS_KEY_SECRET", matches = ".+")
class AnalyticDbVectorTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(AnalyticDbVectorStoreProperties.class))
		.withUserConfiguration(Config.class)
		.withPropertyValues("spring.ai.vectorstore.analytic.accessKeyId=" + System.getenv("ANALYTICDB_ACCESS_KEY_ID"),
				"spring.ai.vectorstore.analytic.accessKeyId=" + System.getenv("ANALYTICDB_ACCESS_KEY_SECRET"));

	List<Document> documents = List.of(
			new Document("1", getText("classpath:spring.ai.txt"), Map.of("docId", "1", "spring", "great")),
			new Document("2", getText("classpath:time.shelter.txt"), Map.of("docId", "1")),
			new Document("3", getText("classpath:great.depression.txt"), Map.of("docId", "1", "depression", "bad")));

	public static String getText(String uri) {
		var resource = new DefaultResourceLoader().getResource(uri);
		try {
			return resource.getContentAsString(StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@BeforeAll
	public static void beforeAll() {
		Awaitility.setDefaultPollInterval(2, TimeUnit.SECONDS);
		Awaitility.setDefaultPollDelay(Duration.ZERO);
		Awaitility.setDefaultTimeout(Duration.ofMinutes(1));
	}

	@Test
	public void addAndSearchTest() {

		this.contextRunner
			.withPropertyValues("spring.ai.vectorstore.analytic.collectName=my_test_index",
					"spring.ai.vectorstore.analytic.regionId=cn-beijing",
					"spring.ai.vectorstore.analytic.dbInstanceId=gp-2ze41j8y0ry4spfev",
					"spring.ai.vectorstore.analytic.managerAccount=hyq",
					"spring.ai.vectorstore.analytic.managerAccountPassword=hdcHDC1997@@@",
					"spring.ai.vectorstore.analytic.namespace=llama",
					"spring.ai.vectorstore.analytic.namespacePassword=llamapassword",
					"spring.ai.vectorstore.analytic.defaultTopK=6",
					"spring.ai.vectorstore.analytic.defaultSimilarityThreshold=0.75")
			.run(context -> {

				var properties = context.getBean(AnalyticDbVectorStoreProperties.class);

				VectorStore vectorStore = context.getBean(VectorStore.class);
				TestObservationRegistry observationRegistry = context.getBean(TestObservationRegistry.class);

				assertThat(vectorStore).isInstanceOf(AnalyticDbVectorStore.class);

				vectorStore.add(this.documents);

				Awaitility.await()
					.until(() -> vectorStore.similaritySearch(SearchRequest.builder().query("Spring").topK(1).build()),
							hasSize(1));

				ObservationTestUtil.assertObservationRegistry(observationRegistry, "analytic_db",
						VectorStoreObservationContext.Operation.ADD);
				observationRegistry.clear();

				List<Document> results = vectorStore
					.similaritySearch(SearchRequest.builder().query("Spring").topK(1).build());

				assertThat(results).hasSize(1);
				Document resultDoc = results.get(0);
				assertThat(resultDoc.getId()).isEqualTo(this.documents.get(0).getId());
				assertThat(resultDoc.getText()).contains(
						"Spring AI provides abstractions that serve as the foundation for developing AI applications.");
				assertThat(resultDoc.getMetadata()).hasSize(2);
				assertThat(resultDoc.getMetadata()).containsKeys("spring", "distance");

				ObservationTestUtil.assertObservationRegistry(observationRegistry, "analytic_db",
						VectorStoreObservationContext.Operation.QUERY);
				observationRegistry.clear();

				// Remove all documents from the store
				vectorStore.delete(this.documents.stream().map(doc -> doc.getId()).toList());

				Awaitility.await()
					.until(() -> vectorStore.similaritySearch(SearchRequest.builder().query("Spring").topK(1).build()),
							hasSize(0));

				ObservationTestUtil.assertObservationRegistry(observationRegistry, "analytic_db",
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

}
