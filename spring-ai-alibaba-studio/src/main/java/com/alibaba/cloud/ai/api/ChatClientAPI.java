package com.alibaba.cloud.ai.api;

import com.alibaba.cloud.ai.common.R;
import com.alibaba.cloud.ai.model.ChatClient;
import com.alibaba.cloud.ai.service.ChatClientDelegate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Tag(name = "chat-client", description = "the chat-client API")
public interface ChatClientAPI {

	default ChatClientDelegate getDelegate() {
		return new ChatClientDelegate() {
		};
	}

	@Operation(summary = "list chat clients", description = "", tags = { "chat-client" })
	@GetMapping(value = "", consumes = { "application/json", "application/x-www-form-urlencoded" })
	default R<List<ChatClient>> list() {
		List<ChatClient> res = getDelegate().list();
		return R.success(res);
	}

	@Operation(summary = "get chat client by name", description = "", tags = { "chat-client" })
	@GetMapping(value = "/{clientName}", consumes = { "application/json", "application/x-www-form-urlencoded" })
	default R<ChatClient> get(@PathVariable String clientName) {
		ChatClient chatClient = getDelegate().get(clientName);
		return R.success(chatClient);
	}

}
