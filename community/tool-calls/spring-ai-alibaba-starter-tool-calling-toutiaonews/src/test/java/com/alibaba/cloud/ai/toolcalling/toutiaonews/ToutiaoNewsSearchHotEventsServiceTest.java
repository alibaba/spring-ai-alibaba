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

package com.alibaba.cloud.ai.toolcalling.toutiaonews;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * ToutiaoNewsSearchHotEventsServiceTest
 *
 * @author zhangshenghang
 */
class ToutiaoNewsSearchHotEventsServiceTest {

	private ToutiaoNewsSearchHotEventsService toutiaoNewsSearchHotEventsService = new ToutiaoNewsSearchHotEventsService();

	@Test
	void apply() {
		ToutiaoNewsSearchHotEventsService.Request request = new ToutiaoNewsSearchHotEventsService.Request();
		ToutiaoNewsSearchHotEventsService.Response apply = toutiaoNewsSearchHotEventsService.apply(request);
		Assertions.assertNotNull(apply);
		// Verify the size of events and the title of each event
		Assertions.assertEquals(50, apply.events().size());
		apply.events().forEach(event -> {
			Assertions.assertNotNull(event.title());
		});
	}

	@Test
	void testFetchDataFromApi() {
		// Test API data retrieval method
		JsonNode result = toutiaoNewsSearchHotEventsService.fetchDataFromApi();
		Assertions.assertNotNull(result);
		Assertions.assertTrue(result.has("data"));
		Assertions.assertTrue(result.get("data").isArray());
	}

	@Test
	void testParseHotEvents() {
		// Create mock JsonNode data
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode rootNode = mapper.createObjectNode();
		ArrayNode dataArray = mapper.createArrayNode();

		// Add test data
		ObjectNode item1 = mapper.createObjectNode();
		item1.put("Title", "Test News Title 1");
		dataArray.add(item1);

		ObjectNode item2 = mapper.createObjectNode();
		item2.put("Title", "Test News Title 2");
		dataArray.add(item2);

		// Add an item without Title
		ObjectNode itemNoTitle = mapper.createObjectNode();
		itemNoTitle.put("OtherField", "Other Field");
		dataArray.add(itemNoTitle);

		rootNode.set("data", dataArray);

		// Parse and verify results
		List<ToutiaoNewsSearchHotEventsService.HotEvent> hotEvents = toutiaoNewsSearchHotEventsService
			.parseHotEvents(rootNode);

		Assertions.assertEquals(2, hotEvents.size());
		Assertions.assertEquals("Test News Title 1", hotEvents.get(0).title());
		Assertions.assertEquals("Test News Title 2", hotEvents.get(1).title());
	}

	@Test
	void testParseHotEventsWithNullData() {
		// Test empty data scenario
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode rootNode = mapper.createObjectNode();

		// Verify exception thrown
		Exception exception = Assertions.assertThrows(RuntimeException.class, () -> {
			toutiaoNewsSearchHotEventsService.parseHotEvents(rootNode);
		});

		Assertions.assertTrue(exception.getMessage().contains("Failed to retrieve or parse response data"));
	}

	@Test
	void testIntegrationWithMock() {
		// Use spy to partially mock the service
		ToutiaoNewsSearchHotEventsService spyService = spy(toutiaoNewsSearchHotEventsService);

		// Create mock JsonNode data
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode rootNode = mapper.createObjectNode();
		ArrayNode dataArray = mapper.createArrayNode();

		for (int i = 0; i < 10; i++) {
			ObjectNode item = mapper.createObjectNode();
			item.put("Title", "Mock News Title " + i);
			dataArray.add(item);
		}

		rootNode.set("data", dataArray);

		// Mock fetchDataFromApi method to return our created data
		when(spyService.fetchDataFromApi()).thenReturn(rootNode);

		// Execute apply method
		ToutiaoNewsSearchHotEventsService.Request request = new ToutiaoNewsSearchHotEventsService.Request();
		ToutiaoNewsSearchHotEventsService.Response response = spyService.apply(request);

		// Verify results
		Assertions.assertNotNull(response);
		Assertions.assertEquals(10, response.events().size());
		Assertions.assertEquals("Mock News Title 0", response.events().get(0).title());
		Assertions.assertEquals("Mock News Title 9", response.events().get(9).title());
	}

}
