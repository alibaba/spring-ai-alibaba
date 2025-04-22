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

package com.alibaba.cloud.ai.toolcalling.sinanews;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test class for SinaNewsService.
 *
 * @author zhangshenghang
 */
class SinaNewsServiceTest {

	private SinaNewsService service = new SinaNewsService();

	@Test
	void apply() {
		SinaNewsService.Request request = new SinaNewsService.Request();
		SinaNewsService.Response apply = service.apply(request);
		assertNotNull(apply);
		// Assert that the events list is not empty and contains at least one event
		assertTrue(apply.events().size() > 0);
		// Assert that each event has a title
		apply.events().forEach(event -> assertNotNull(event.title()));
	}

	@Test
	void testFetchDataFromApi() {
		// Mock WebClient and response
		WebClient mockWebClient = Mockito.mock(WebClient.class);
		WebClient.RequestHeadersUriSpec mockRequest = Mockito.mock(WebClient.RequestHeadersUriSpec.class);
		WebClient.ResponseSpec mockResponse = Mockito.mock(WebClient.ResponseSpec.class);

		when(mockWebClient.get()).thenReturn(mockRequest);
		when(mockRequest.uri(anyString())).thenReturn(mockRequest);
		when(mockRequest.retrieve()).thenReturn(mockResponse);

		// Mock JSON response
		String jsonResponse = "{\"data\":{\"hotList\":[{\"info\":{\"title\":\"Test News 1\"}}]}}";
		JsonNode mockNode = new ObjectMapper().valueToTree(jsonResponse);
		when(mockResponse.bodyToMono(JsonNode.class)).thenReturn(Mono.just(mockNode));

		// Create service instance with mocked client
		SinaNewsService service = new SinaNewsService() {
			@Override
			protected WebClient getWebClient() {
				return mockWebClient;
			}
		};

		// Test fetchDataFromApi
		JsonNode result = service.fetchDataFromApi();
		assertNotNull(result);
		assertTrue(result.has("data"));
	}

}
