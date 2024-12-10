/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.crawler.controller;

import com.alibaba.cloud.ai.plugin.crawler.entity.JinaResponse;
import com.alibaba.cloud.ai.plugin.crawler.service.CrawlerService;
import com.alibaba.cloud.ai.plugin.crawler.service.impl.CrawlerJinaServiceImpl;
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

	private CrawlerJinaController(CrawlerJinaServiceImpl jinaService, ObjectMapper objectMapper) {
		this.jinaService = jinaService;
		this.objectMapper = objectMapper;
	}

	@GetMapping("/jina")
	public JinaResponse jinaCrawler() throws JsonProcessingException {

		return this.objectMapper.readValue(jinaService.run("https://www.baidu.com"), JinaResponse.class);
	}

}
