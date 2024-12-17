package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.common.R;
import com.alibaba.cloud.ai.oltp.StudioObservabilityProperties;
import com.alibaba.cloud.ai.service.StudioObservabilityService;
import com.alibaba.cloud.ai.service.impl.StudioObservabilityServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.logging.Logger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

	private final ChatModel chatModel;

	private final StudioObservabilityService studioObservabilityService;

	ObservationApiController(ChatClient.Builder builder, ChatModel chatModel,
			StudioObservabilityProperties studioObservabilityProperties) {
		this.chatClient = builder.build();
		this.chatModel = chatModel;
		this.studioObservabilityService = new StudioObservabilityServiceImpl(studioObservabilityProperties);
	}

	@GetMapping("/getAll")
	R<ArrayNode> getAll() {
		var res = studioObservabilityService.readObservabilityFile();
		return R.success(res);
	}

	@GetMapping("/getAITraceInfo")
	R<ArrayNode> getAITraceInfo() {
		var res = studioObservabilityService.getAITraceInfo();
		return R.success(res);
	}

	@GetMapping("/detail")
	R<JsonNode> detail(String traceId) {
		var res = studioObservabilityService.getTraceByTraceId(traceId);
		return R.success(res);
	}

	@GetMapping("/clearAll")
	R<String> clearAll() {
		var res = studioObservabilityService.clearExportContent();
		return R.success(res);
	}

	@GetMapping("/chatClient")
	R<String> chatClient(String input) {
		var reply = chatClient.prompt().user(input).call().content();
		return R.success(reply);
	}

	@GetMapping("/chatModel")
	R<String> chatModel(String input) {
		var reply = chatModel.call(new Prompt(input)).getResult().getOutput().getContent();
		return R.success(reply);
	}

}
