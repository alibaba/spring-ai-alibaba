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
package com.alibaba.cloud.ai.toolcalling.openalex;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallAutoConfiguration;
import com.alibaba.cloud.ai.toolcalling.common.interfaces.SearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OpenAlex tool calling
 */
@DisplayName("OpenAlex Tool Calling Tests")
public class OpenAlexTest {

	private static final Logger log = LoggerFactory.getLogger(OpenAlexTest.class);

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(OpenAlexAutoConfiguration.class, CommonToolCallAutoConfiguration.class));

	@Test
	public void testAutoConfiguration() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(OpenAlexService.class);
			assertThat(context).hasSingleBean(OpenAlexProperties.class);
		});
	}

	@Test
	public void testSearchFunctionality() {
		this.contextRunner.run((context) -> {
			OpenAlexService service = context.getBean(OpenAlexService.class);
			assertThat(service).isNotNull();

			// Create test requests
			OpenAlexService.Request worksRequest = OpenAlexService.Request.simpleQuery("machine learning");
			assertThat(worksRequest.getQuery()).isEqualTo("machine learning");
			assertThat(worksRequest.entityType()).isEqualTo("works");

			OpenAlexService.Request authorRequest = OpenAlexService.Request.authorSearch("Einstein");
			assertThat(authorRequest.getQuery()).isEqualTo("Einstein");
			assertThat(authorRequest.entityType()).isEqualTo("authors");

			OpenAlexService.Request institutionRequest = OpenAlexService.Request.institutionSearch("MIT");
			assertThat(institutionRequest.getQuery()).isEqualTo("MIT");
			assertThat(institutionRequest.entityType()).isEqualTo("institutions");

			// Note: Actual network testing should be done separately with proper test
			// environment
			// This is just to verify the service can be instantiated and basic
			// functionality works
		});
	}

	@Test
	public void testAbstractSearchService() {
		this.contextRunner.run((context) -> {
			// Test abstract SearchService interface
			SearchService searchService = context.getBean(SearchService.class);
			assertThat(searchService).isNotNull();
			assertThat(searchService).isInstanceOf(OpenAlexService.class);

			// Test that it implements SearchService correctly
			assertThat(searchService).isInstanceOf(SearchService.class);

			// Note: Actual search testing would require network access
			// This test verifies the service is properly configured as SearchService
		});
	}

	@Test
	public void testCustomConfiguration() {
		this.contextRunner
			.withPropertyValues("spring.ai.alibaba.toolcalling.openalex.per-page=50",
					"spring.ai.alibaba.toolcalling.openalex.timeout=60000",
					"spring.ai.alibaba.toolcalling.openalex.include-abstract=true",
					"spring.ai.alibaba.toolcalling.openalex.max-pages=3")
			.run((context) -> {
				OpenAlexProperties properties = context.getBean(OpenAlexProperties.class);
				assertThat(properties.getPerPage()).isEqualTo(50);
				assertThat(properties.getTimeout()).isEqualTo(60000);
				assertThat(properties.isIncludeAbstract()).isTrue();
				assertThat(properties.getMaxPages()).isEqualTo(3);
			});
	}

	@Test
	public void testDisabledConfiguration() {
		this.contextRunner.withPropertyValues("spring.ai.alibaba.toolcalling.openalex.enabled=false").run((context) -> {
			assertThat(context).doesNotHaveBean(OpenAlexService.class);
		});
	}

	@Test
	public void testRequestBuilding() {
		// Test complex request building
		OpenAlexService.Request complexRequest = new OpenAlexService.Request("artificial intelligence", "works",
				"A123456789", // author ID
				"I123456789", // institution ID
				2020, // from year
				2024, // to year
				true, // open access only
				100, // per page
				"publication_date:desc" // sort by
		);

		assertThat(complexRequest.getQuery()).isEqualTo("artificial intelligence");
		assertThat(complexRequest.entityType()).isEqualTo("works");
		assertThat(complexRequest.author()).isEqualTo("A123456789");
		assertThat(complexRequest.institution()).isEqualTo("I123456789");
		assertThat(complexRequest.fromYear()).isEqualTo(2020);
		assertThat(complexRequest.toYear()).isEqualTo(2024);
		assertThat(complexRequest.isOpenAccess()).isTrue();
		assertThat(complexRequest.perPage()).isEqualTo(100);
		assertThat(complexRequest.sortBy()).isEqualTo("publication_date:desc");
	}

	@Test
	@DisplayName("Actual API Call Test - Works Search")
	public void testActualWorksSearch() {
		this.contextRunner.withPropertyValues("spring.ai.alibaba.toolcalling.openalex.per-page=5" // 限制结果数量以加快测试
		).run((context) -> {
			OpenAlexService service = context.getBean(OpenAlexService.class);
			assertThat(service).isNotNull();

			// Test simple works search
			log.info("Testing OpenAlex works search with query: 'machine learning'");
			OpenAlexService.Request request = OpenAlexService.Request.simpleQuery("machine learning");
			OpenAlexService.Response response = service.apply(request);

			// Verify response structure
			assertThat(response).isNotNull();
			assertThat(response.query()).isEqualTo("machine learning");

			// Check if API call was successful (no error)
			if (response.error() != null) {
				log.warn("API call returned error: {}", response.error());
				// If there's an error, we still want to verify the response structure
				assertThat(response.results()).isNull();
			}
			else {
				// If successful, verify results
				assertThat(response.results()).isNotNull();
				log.info("Found {} results for 'machine learning'", response.results().size());

				if (!response.results().isEmpty()) {
					// Verify first result structure
					OpenAlexService.OpenAlexResult firstResult = response.results().get(0);
					assertThat(firstResult).isNotNull();
					assertThat(firstResult.id()).isNotEmpty();
					assertThat(firstResult.title()).isNotEmpty();
					assertThat(firstResult.entityType()).isEqualTo("works");

					log.info("First result: {} - {}", firstResult.title(), firstResult.description());
				}
			}
		});
	}

	@Test
	@DisplayName("Actual API Call Test - Author Search")
	public void testActualAuthorSearch() {
		this.contextRunner.withPropertyValues("spring.ai.alibaba.toolcalling.openalex.per-page=3").run((context) -> {
			OpenAlexService service = context.getBean(OpenAlexService.class);
			assertThat(service).isNotNull();

			// Test author search
			log.info("Testing OpenAlex author search with query: 'Geoffrey Hinton'");
			OpenAlexService.Request request = OpenAlexService.Request.authorSearch("Geoffrey Hinton");
			OpenAlexService.Response response = service.apply(request);

			// Verify response structure
			assertThat(response).isNotNull();
			assertThat(response.query()).isEqualTo("Geoffrey Hinton");

			// Check results
			if (response.error() != null) {
				log.warn("Author search returned error: {}", response.error());
			}
			else {
				assertThat(response.results()).isNotNull();
				log.info("Found {} authors for 'Geoffrey Hinton'", response.results().size());

				if (!response.results().isEmpty()) {
					OpenAlexService.OpenAlexResult firstResult = response.results().get(0);
					assertThat(firstResult.entityType()).isEqualTo("authors");
					log.info("First author result: {} - {}", firstResult.displayName(), firstResult.description());
				}
			}
		});
	}

	@Test
	@DisplayName("Actual API Call Test - Abstract SearchService Interface")
	public void testActualSearchServiceInterface() {
		this.contextRunner.withPropertyValues("spring.ai.alibaba.toolcalling.openalex.per-page=3").run((context) -> {
			// Test using the abstract SearchService interface
			SearchService searchService = context.getBean(SearchService.class);
			assertThat(searchService).isNotNull();

			log.info("Testing SearchService interface with query: 'artificial intelligence'");
			SearchService.Response response = searchService.query("artificial intelligence");

			// Verify SearchService response
			assertThat(response).isNotNull();
			SearchService.SearchResult searchResult = response.getSearchResult();
			assertThat(searchResult).isNotNull();
			assertThat(searchResult.results()).isNotNull();

			log.info("SearchService returned {} results", searchResult.results().size());

			// Verify SearchContent structure if results exist
			if (!searchResult.results().isEmpty()) {
				SearchService.SearchContent firstContent = searchResult.results().get(0);
				assertThat(firstContent).isNotNull();
				assertThat(firstContent.title()).isNotEmpty();
				log.info("First SearchContent: {}", firstContent.title());
			}
		});
	}

	@Test
	@DisplayName("Error Handling Test")
	public void testErrorHandling() {
		this.contextRunner.run((context) -> {
			OpenAlexService service = context.getBean(OpenAlexService.class);
			assertThat(service).isNotNull();

			// Test with empty query
			log.info("Testing error handling with empty query");
			OpenAlexService.Request emptyRequest = OpenAlexService.Request.simpleQuery("");
			OpenAlexService.Response response = service.apply(emptyRequest);

			// Should return error response
			assertThat(response).isNotNull();
			assertThat(response.error()).isNotNull();
			assertThat(response.results()).isNull();
			log.info("Empty query returned expected error: {}", response.error());
		});
	}

}
