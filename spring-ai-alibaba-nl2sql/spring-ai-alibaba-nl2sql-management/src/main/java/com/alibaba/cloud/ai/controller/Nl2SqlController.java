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
package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.entity.Nl2SqlProcess;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.service.Nl2SqlService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * NL2SQL接口预留
 *
 * @author vlsmb
 * @since 2025/7/27
 */
@RestController
@RequestMapping("/nl2sql")
public class Nl2SqlController {

	private static final Logger logger = LoggerFactory.getLogger(Nl2SqlController.class);

	private final Nl2SqlService nl2SqlService;

	private final ExecutorService executorService;

	public Nl2SqlController(Nl2SqlService nl2SqlService) {
		this.nl2SqlService = nl2SqlService;
		this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	}

	/**
	 * 直接返回NL2SQL的结果
	 * @param query 自然语言
	 * @param agentId Agent Id
	 * @return sql结果
	 */
	@GetMapping("/nl2sql")
	public String nl2sql(@RequestParam(value = "query") String query,
			@RequestParam(value = "agentId", required = false, defaultValue = "") String agentId) {
		try {
			return this.nl2SqlService.nl2sql(query, agentId);
		}
		catch (Exception e) {
			logger.error("nl2sql Exception: {}", e.getMessage(), e);
			return "Error: " + e.getMessage();
		}
	}

	/**
	 * 执行NL2SQL的过程（带中间过程输出）
	 * @param query 自然语言
	 * @param agentId Agent Id
	 * @param response Servlet Response
	 * @return NL2SQL执行过程
	 */
	@GetMapping(value = "/stream/nl2sql", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<Nl2SqlProcess>> nl2sqlWithProcess(@RequestParam(value = "query") String query,
			@RequestParam(value = "agentId", required = false, defaultValue = "") String agentId,
			HttpServletResponse response) {
		// Set SSE-related HTTP headers
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/event-stream");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Connection", "keep-alive");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Headers", "Cache-Control");

		logger.info("Starting nl2sql for query: {} with agentId: {}", query, agentId);

		Sinks.Many<ServerSentEvent<Nl2SqlProcess>> sink = Sinks.many().unicast().onBackpressureBuffer();
		Consumer<Nl2SqlProcess> consumer = (process) -> {
			sink.tryEmitNext(ServerSentEvent.builder(process).build());
			if (process.getFinished()) {
				sink.tryEmitComplete();
			}
		};

		executorService.submit(() -> {
			try {
				this.nl2SqlService.nl2sqlWithProcess(consumer, query, agentId);
			}
			catch (Exception e) {
				logger.error("nl2sql Exception: {}", e.getMessage(), e);
				sink.tryEmitNext(
						ServerSentEvent.builder(Nl2SqlProcess.fail(e.getMessage(), StateGraph.END, e.getMessage()))
							.build());
				sink.tryEmitError(e);
			}
		});
		return sink.asFlux()
			.doOnSubscribe(subscription -> logger.info("Client subscribed to stream"))
			.doOnCancel(() -> logger.info("Client disconnected from stream"))
			.doOnError(e -> logger.error("Error occurred during streaming: ", e))
			.doOnComplete(() -> logger.info("Stream completed successfully"));
	}

}
