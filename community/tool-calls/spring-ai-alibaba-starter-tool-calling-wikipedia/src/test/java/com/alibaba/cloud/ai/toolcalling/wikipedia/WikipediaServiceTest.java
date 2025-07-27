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

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallAutoConfiguration;
import com.alibaba.cloud.ai.toolcalling.common.interfaces.SearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.logging.Logger;

/**
 * Wikipedia服务完整测试类
 *
 * @author Makoto
 */
@SpringBootTest(classes = { CommonToolCallAutoConfiguration.class, WikipediaAutoConfiguration.class })
@DisplayName("Wikipedia Search Test")
public class WikipediaServiceTest {

	@Autowired
	private WikipediaService wikipediaService;

	private static final Logger log = Logger.getLogger(WikipediaServiceTest.class.getName());

	@Test
	@DisplayName("Tool-Calling Test")
	public void testWikipediaSearch() {
		var request = new WikipediaService.Request("人工智能", 5, false);
		var response = wikipediaService.apply(request);

		assert response != null;
		assert response.pages() != null;
		log.info("Wikipedia搜索结果: " + response.summary());
		log.info("页面数量: " + response.pages().size());

		if (!response.pages().isEmpty()) {
			WikipediaService.WikiPage firstPage = response.pages().get(0);
			log.info("第一个页面标题: " + firstPage.title());
			log.info("第一个页面摘要: " + firstPage.snippet());
		}
	}

	@Test
	@DisplayName("Tool-Calling Test with Content")
	public void testWikipediaSearchWithContent() {
		var request = new WikipediaService.Request("Spring Boot", 3, true);
		var response = wikipediaService.apply(request);

		assert response != null;
		assert response.pages() != null;
		log.info("包含内容的Wikipedia搜索结果: " + response.summary());

		if (!response.pages().isEmpty()) {
			WikipediaService.WikiPage firstPage = response.pages().get(0);
			log.info("第一个页面标题: " + firstPage.title());
			if (firstPage.content() != null) {
				log.info("第一个页面内容长度: " + firstPage.content().length());
			}
		}
	}

	@Autowired
	private SearchService searchService;

	@Test
	@DisplayName("Abstract Search Service Test")
	public void testAbstractSearch() {
		var response = searchService.query("Spring AI Alibaba");

		assert response != null;
		assert response.getSearchResult() != null;
		assert response.getSearchResult().results() != null;

		log.info("抽象搜索服务结果数量: " + response.getSearchResult().results().size());

		if (!response.getSearchResult().results().isEmpty()) {
			var firstResult = response.getSearchResult().results().get(0);
			log.info("第一个搜索结果标题: " + firstResult.title());
			log.info("第一个搜索结果内容: " + firstResult.content());
			log.info("第一个搜索结果URL: " + firstResult.url());
		}
	}

	@Test
	@DisplayName("Empty Query Test")
	public void testEmptyQuery() {
		var request = new WikipediaService.Request("", 5, false);
		var response = wikipediaService.apply(request);

		assert response != null;
		assert response.summary().contains("错误");
		assert response.pages() != null;
		assert response.pages().isEmpty();
		log.info("空查询测试结果: " + response.summary());
	}

	@Test
	@DisplayName("Null Request Test")
	public void testNullRequest() {
		var response = wikipediaService.apply(null);

		assert response != null;
		assert response.summary().contains("错误");
		assert response.pages() != null;
		assert response.pages().isEmpty();
		log.info("空请求测试结果: " + response.summary());
	}

}
