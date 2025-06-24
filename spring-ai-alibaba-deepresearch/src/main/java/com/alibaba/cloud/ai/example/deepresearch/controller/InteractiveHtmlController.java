/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.deepresearch.controller;

import com.alibaba.cloud.ai.example.deepresearch.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import reactor.core.publisher.Flux;

/**
 * @author fangxin
 * @since 2025/6/20 交互式HTML报告控制器，提供生成和展示交互式HTML报告的API Interactive HTML Report Controller,
 * providing APIs for generating and displaying interactive HTML reports
 */
@Controller
@RequestMapping("/api/interactive-html")
public class InteractiveHtmlController {

	private static final Logger log = LoggerFactory.getLogger(InteractiveHtmlController.class);

	@Autowired
	private ReportService reportService;

	@Autowired
	private ChatClient interactionAgent;

	/**
	 * building an interactive html report(构建交互式HTML报告)
	 * @param reportId 报告ID
	 * @return Return a Flux stream containing events from the build
	 * process(返回一个Flux流，包含构建过程中的事件)
	 */
	@RequestMapping(value = "/build", method = RequestMethod.GET, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ChatResponse> buildInteractiveHtml(String reportId) {
		if (reportId == null || reportId.isEmpty()) {
			log.error("Report ID is null or empty");
			return Flux.error(new IllegalArgumentException("Report ID cannot be null or empty"));
		}
		log.info("Building interactive HTML report");
		String reportInfo = reportService.getReport(reportId);
		// 使用ChatClient来构建HTML报告
		return interactionAgent.prompt(reportInfo).stream().chatResponse();
	}

}
