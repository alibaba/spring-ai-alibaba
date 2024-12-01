package com.alibaba.cloud.ai.crawler.controller;

import com.alibaba.cloud.ai.plugin.crawler.entity.JinaResponse;
import com.alibaba.cloud.ai.plugin.crawler.service.CrawlerService;
import com.fasterxml.jackson.core.JsonProcessingException;
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
public class CrawlerJinaController {

	private final CrawlerService jinaService;

	private final ObjectMapper objectMapper;

	private CrawlerJinaController(CrawlerService jinaService, ObjectMapper objectMapper) {
		this.jinaService = jinaService;
		this.objectMapper = objectMapper;
	}

	@GetMapping("/jina")
	public JinaResponse jinaCrawler() throws JsonProcessingException {

		return this.objectMapper.readValue(jinaService.run("https://www.baidu.com"), JinaResponse.class);
	}

}
