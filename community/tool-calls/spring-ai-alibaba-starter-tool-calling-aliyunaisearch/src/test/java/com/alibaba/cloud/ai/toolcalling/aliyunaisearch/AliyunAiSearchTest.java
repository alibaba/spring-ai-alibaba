package com.alibaba.cloud.ai.toolcalling.aliyunaisearch;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallAutoConfiguration;
import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants;
import com.alibaba.cloud.ai.toolcalling.common.interfaces.SearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.logging.Logger;

@SpringBootTest(classes = { CommonToolCallAutoConfiguration.class, AliyunAiSearchAutoConfiguration.class })
@DisplayName("Aliyun AI Search Test")
public class AliyunAiSearchTest {

	@Autowired
	private AliyunAiSearchService aliyunAiSearchService;

	private static final Logger log = Logger.getLogger(AliyunAiSearchTest.class.getName());

	@Test
	@DisplayName("Tool-Calling Test")
	@EnabledIfEnvironmentVariable(named = AliyunAiSearchConstants.API_KEY_ENV,
			matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	@EnabledIfEnvironmentVariable(named = AliyunAiSearchConstants.BASE_URL_ENV,
			matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	public void testAliyunAiSearch() {
		var resp = aliyunAiSearchService.apply(AliyunAiSearchService.Request.simplyQuery("Spring AI Alibaba"));
		assert resp != null && resp.results() != null;
		log.info("results: " + resp.results());
	}

	@Autowired
	private SearchService searchService;

	@Test
	@DisplayName("Abstract Search Service Test")
	@EnabledIfEnvironmentVariable(named = AliyunAiSearchConstants.API_KEY_ENV,
			matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	@EnabledIfEnvironmentVariable(named = AliyunAiSearchConstants.BASE_URL_ENV,
			matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	public void testAbstractSearch() {
		var resp = searchService.query("Spring AI Alibaba");
		assert resp != null && resp.getSearchResult() != null && resp.getSearchResult().results() != null
				&& !resp.getSearchResult().results().isEmpty();
		log.info("results: " + resp.getSearchResult());
	}

}
