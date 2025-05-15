package com.alibaba.cloud.ai.toolcalling.serpapi;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallAutoConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "debug=true",
		classes = { SerpApiAutoConfiguration.class, CommonToolCallAutoConfiguration.class })
public class SerpApiServiceTest {

	@Autowired
	private SerpApiService serpApiService;

	@Test
	void testSerpApiServiceInjected() {
		Assertions.assertNotNull(serpApiService);
	}

	@Test
	void testApplyWithValidRequest() {

		SerpApiService.Request request = new SerpApiService.Request("Alibaba Cloud");

		SerpApiService.Response response = serpApiService.apply(request);

		Assertions.assertNotNull(response, "Response should not be null");
		Assertions.assertFalse(response.results().isEmpty(), "Results should not be empty");

		SerpApiService.SearchResult firstResult = response.results().get(0);
		Assertions.assertNotNull(firstResult.title(), "Title should not be null");
		Assertions.assertFalse(firstResult.title().isEmpty(), "Title should not be empty");
		Assertions.assertNotNull(firstResult.text(), "Text should not be null");
		Assertions.assertFalse(firstResult.text().isEmpty(), "Text should not be empty");
	}

	@Test
	void testApplyWithInvalidRequest() {
		SerpApiService.Request invalidRequest = new SerpApiService.Request("");

		SerpApiService.Response response = serpApiService.apply(invalidRequest);

		Assertions.assertNull(response, "Response should be null for invalid request");
	}

}