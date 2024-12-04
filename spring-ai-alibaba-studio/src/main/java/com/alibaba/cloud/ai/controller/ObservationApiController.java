package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.common.R;
import com.alibaba.cloud.ai.oltp.OtlpFileSpanExporter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.logging.Logger;

/**
 * @Description:
 * @Author: XiaoYunTao
 * @Date: 2024/12/4
 */
@CrossOrigin
@RestController
@RequestMapping("studio/api/observation")
public class ObservationApiController {

	private static final Logger logger = Logger.getLogger(ObservationApiController.class.getName());

	private final ChatClient chatClient;

	private final OtlpFileSpanExporter otlpFileSpanExporter;

	ObservationApiController(ChatClient.Builder builder, OtlpFileSpanExporter otlpFileSpanExporter) {
		this.chatClient = builder.build();
		this.otlpFileSpanExporter = otlpFileSpanExporter;
	}

	@GetMapping("/chatClient")
	R<String> chatClient(String input) {
		var reply = chatClient.prompt().user(input).call().content();
		return R.success(reply);
	}

	@GetMapping("/getAll")
	R<ArrayNode> getAll() {
		var reply = otlpFileSpanExporter.readJsonFromFile();
		logger.info("getAll: " + reply.toString());
		return R.success(reply);
	}

	@GetMapping("/detail")
	R<JsonNode> detail(String traceId) {
		var reply = otlpFileSpanExporter.getJsonNodeByTraceId(traceId);
		logger.info("detail: " + reply.toString());
		return R.success(reply);
	}

	@GetMapping("/list")
	R<List<OtlpFileSpanExporter.ListResponse>> list() {
		var reply = otlpFileSpanExporter.extractSpansWithoutParentSpanId();
		logger.info("list: " + reply);
		return R.success(reply);
	}
}
