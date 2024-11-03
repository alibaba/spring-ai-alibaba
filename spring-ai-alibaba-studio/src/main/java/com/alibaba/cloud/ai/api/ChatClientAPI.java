package com.alibaba.cloud.ai.api;

import com.alibaba.cloud.ai.model.ChatClient;
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

import java.util.List;

@Tag(name = "chat-client", description = "the chat-client API")
public interface ChatClientAPI {

	@Operation(summary = "list chat clients", description = "", tags = { "chat-client" })
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "successful operation",
			content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
					array = @ArraySchema(schema = @Schema(implementation = ChatClient.class)))), })
	@GetMapping(value = "", consumes = { "application/json", "application/x-www-form-urlencoded" })
	default List<ChatClient> list() {
		return null;
	}

	@Operation(summary = "get chat client by name", description = "", tags = { "chat-client" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "successful operation",
					content = { @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
							schema = @Schema(implementation = ChatClient.class)) }),
			@ApiResponse(responseCode = "404", description = "chat client not found") })
	@GetMapping(value = "/{clientName}", consumes = { "application/json", "application/x-www-form-urlencoded" })
	default ChatClient get(@PathVariable String clientName) {
		return null;
	}

}
