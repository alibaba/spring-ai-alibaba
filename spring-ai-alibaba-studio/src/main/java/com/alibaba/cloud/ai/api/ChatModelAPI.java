package com.alibaba.cloud.ai.api;

import com.alibaba.cloud.ai.common.R;
import com.alibaba.cloud.ai.model.ChatModel;
import com.alibaba.cloud.ai.param.RunActionParam;
import com.alibaba.cloud.ai.service.ChatModelDelegate;
import com.alibaba.cloud.ai.vo.ChatModelRunResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

@Tag(name = "chat-model", description = "the chat-model API")
public interface ChatModelAPI {

	default ChatModelDelegate getDelegate() {
		return new ChatModelDelegate() {
		};
	}

	@Operation(summary = "list chat models", description = "", tags = { "chat-model" })
	@GetMapping(value = "", consumes = { "*/*" })
	default R<List<ChatModel>> list() {
		List<ChatModel> res = getDelegate().list();
		return R.success(res);
	}

	@Operation(summary = "get chat model by model name", description = "", tags = { "chat-model" })
	@GetMapping(value = "/{modelName}", consumes = { "application/json" }, produces = { "application/json" })
	default R<ChatModel> get(@PathVariable String modelName) {
		ChatModel res = getDelegate().getByModelName(modelName);
		return R.success(res);
	}

	@Operation(summary = "run chat model by input", description = "", tags = { "chat-model" })
	@PostMapping(value = "", consumes = { MediaType.APPLICATION_JSON_VALUE })
	default R<ChatModelRunResult> run(@RequestBody RunActionParam runActionParam) {
		ChatModelRunResult res = getDelegate().run(runActionParam);
		return R.success(res);
	}

	@Operation(summary = "run image model by input", description = "", tags = { "chat-model" })
	@PostMapping(value = "/run/image-gen", consumes = { MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE })
	default void runImageGenTask(@RequestBody RunActionParam runActionParam, HttpServletResponse response) {
		String imageUrl = getDelegate().runImageGenTask(runActionParam);
		try {
			URL url = new URL(imageUrl);
			InputStream in = url.openStream();

			response.setHeader("Content-Type", MediaType.IMAGE_PNG_VALUE);
			response.getOutputStream().write(in.readAllBytes());
			response.getOutputStream().flush();
		}
		catch (IOException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

}
