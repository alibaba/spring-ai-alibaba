package com.alibaba.cloud.ai.api;

import com.alibaba.cloud.ai.model.ChatModel;
import com.alibaba.cloud.ai.param.RunActionParam;
import com.alibaba.cloud.ai.service.ChatModelDelegate;
import com.alibaba.cloud.ai.vo.ChatModelRunResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "chat-model", description = "the chat-model API")
public interface ChatModelAPI {

	default ChatModelDelegate getDelegate() {
		return new ChatModelDelegate() {
		};
	}

	@Operation(summary = "list chat models", description = "", tags = { "chat-model" })
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "successful operation",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
					array = @ArraySchema(schema = @Schema(implementation = ChatModel.class)))), })
	@GetMapping(value = "", consumes = { "application/json", "application/x-www-form-urlencoded" })
	default List<ChatModel> list() {
		return getDelegate().list();
	}

	@Operation(summary = "get chat model by model name", description = "", tags = { "chat-model" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "successful operation",
					content = { @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ChatModel.class)) }),
			@ApiResponse(responseCode = "404", description = "model not found") })
	@GetMapping(value = "/{modelName}", consumes = { "application/json", "application/x-www-form-urlencoded" })
	default ChatModel get(@PathVariable String modelName) {
		return getDelegate().getByModelName(modelName);
	}

	@Operation(summary = "run model by input", description = "", tags = { "chat-model" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "successful operation",
					content = { @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ChatModelRunResult.class)) }),
			@ApiResponse(responseCode = "404", description = "model not found") })
	@PostMapping(value = "", consumes = { MediaType.APPLICATION_JSON_VALUE })
	default ChatModelRunResult run(@RequestParam RunActionParam runActionParam) {
		return getDelegate().run(runActionParam);
	}

}
