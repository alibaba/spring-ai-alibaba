package com.alibaba.cloud.ai.crawler.controller;

import com.alibaba.cloud.ai.plugin.crawler.service.CrawlerService;
import com.alibaba.cloud.ai.plugin.crawler.service.impl.CrawlerFirecrawlServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@RestController
@RequestMapping("/ai/crawler")
public class CrawlerFirecrawlController {

	private final CrawlerService firecrawlService;

	private final ObjectMapper objectMapper;

	private CrawlerFirecrawlController(CrawlerFirecrawlServiceImpl firecrawlService, ObjectMapper objectMapper) {
		this.firecrawlService = firecrawlService;
		this.objectMapper = objectMapper;
	}

	@GetMapping("/firecrawl")
	public JsonNode jinaCrawler() throws JsonProcessingException {

		return objectMapper.readValue(firecrawlService.run("https://www.baidu.com"), JsonNode.class);
	}

}
