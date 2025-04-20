package com.alibaba.cloud.ai.toolcalling.baidusearch;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BaiduSearchServiceTest {

	private final BaiduSearchService baiduSearchService = new BaiduSearchService();

	@Test
	void apply() {
		BaiduSearchService.Request request = new BaiduSearchService.Request("Spring AI", 10);
		BaiduSearchService.Response apply = baiduSearchService.apply(request);
		// assert that the response is not null and contains the expected number of
		// results
		Assertions.assertNotNull(apply);
		Assertions.assertEquals(10, apply.results().size());
		apply.results().forEach(result -> {
			Assertions.assertNotNull(result.title());
			Assertions.assertNotNull(result.abstractText());
		});
	}

}