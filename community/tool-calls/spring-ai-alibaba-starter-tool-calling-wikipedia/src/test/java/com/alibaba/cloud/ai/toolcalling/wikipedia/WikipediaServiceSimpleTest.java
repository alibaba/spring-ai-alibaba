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
package com.alibaba.cloud.ai.toolcalling.wikipedia;

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Wikipedia服务简单测试类（AI测试）
 *
 * @author AI Assistant
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WikipediaServiceSimpleTest {

	@Mock
	private WebClientTool webClientTool;

	@Mock
	private JsonParseTool jsonParseTool;

	@Mock
	private WikipediaProperties properties;

	private WikipediaService wikipediaService;

	@BeforeEach
	void setUp() {
		wikipediaService = new WikipediaService(webClientTool, jsonParseTool, properties);
	}

	@Test
	void testApply_emptyQuery_returnsError() {
		// Given
		WikipediaService.Request request = new WikipediaService.Request("", 5, false);

		// When
		WikipediaService.Response response = wikipediaService.apply(request);

		// Then
		assertNotNull(response);
		assertEquals("错误：搜索查询不能为空", response.summary());
		assertNotNull(response.pages());
		assertTrue(response.pages().isEmpty());

		// 验证没有调用网络请求
		verify(webClientTool, never()).get(anyString(), any(MultiValueMap.class));
	}

	@Test
	void testApply_nullRequest_returnsError() {
		// When
		WikipediaService.Response response = wikipediaService.apply(null);

		// Then
		assertNotNull(response);
		assertEquals("错误：搜索查询不能为空", response.summary());
		assertNotNull(response.pages());
		assertTrue(response.pages().isEmpty());

		// 验证没有调用网络请求
		verify(webClientTool, never()).get(anyString(), any(MultiValueMap.class));
	}

	@Test
	void testApply_normalSearch_success() throws JsonProcessingException {
		// Given
		WikipediaService.Request request = new WikipediaService.Request("人工智能", 5, false);

		String mockSearchResponse = """
				{
					"query": {
						"search": [
							{
								"title": "人工智能",
								"snippet": "人工智能是计算机科学的一个分支",
								"pageid": 12345,
								"size": 50000,
								"timestamp": "2024-01-01T00:00:00Z"
							}
						]
					}
				}
				""";

		Map<String, Object> mockSearchResult = createMockSearchResultMap();

		when(webClientTool.get(eq("w/api.php"), any(MultiValueMap.class))).thenReturn(Mono.just(mockSearchResponse));
		when(jsonParseTool.jsonToObject(eq(mockSearchResponse), any(TypeReference.class))).thenReturn(mockSearchResult);

		// When
		WikipediaService.Response response = wikipediaService.apply(request);

		// Then
		assertNotNull(response);
		assertEquals("找到 1 个相关页面", response.summary());
		assertNotNull(response.pages());
		assertEquals(1, response.pages().size());

		WikipediaService.WikiPage firstPage = response.pages().get(0);
		assertEquals("人工智能", firstPage.title());
		assertEquals("人工智能是计算机科学的一个分支", firstPage.snippet());
		assertNull(firstPage.content()); // 不包含详细内容
		assertEquals(12345, firstPage.pageId());
		assertEquals(50000, firstPage.size());
		assertEquals("2024-01-01T00:00:00Z", firstPage.timestamp());

		verify(webClientTool, times(1)).get(eq("w/api.php"), any(MultiValueMap.class));
		verify(jsonParseTool, times(1)).jsonToObject(eq(mockSearchResponse), any(TypeReference.class));
	}

	private Map<String, Object> createMockSearchResultMap() {
		Map<String, Object> result = new HashMap<>();
		Map<String, Object> query = new HashMap<>();

		Map<String, Object> page1 = new HashMap<>();
		page1.put("title", "人工智能");
		page1.put("snippet", "人工智能是计算机科学的一个分支");
		page1.put("pageid", 12345);
		page1.put("size", 50000);
		page1.put("timestamp", "2024-01-01T00:00:00Z");

		query.put("search", List.of(page1));
		result.put("query", query);

		return result;
	}

}