/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.api;

import com.alibaba.cloud.ai.common.ModelType;
import com.alibaba.cloud.ai.common.R;
import com.alibaba.cloud.ai.model.ChatModel;
import com.alibaba.cloud.ai.param.ModelRunActionParam;
import com.alibaba.cloud.ai.service.ChatModelDelegate;
import com.alibaba.cloud.ai.vo.ChatModelRunResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "chat-model", description = "the chat-model API")
public interface ChatModelAPI {

	default ChatModelDelegate getDelegate() {
		return new ChatModelDelegate() {
		};
	}

	@Operation(summary = "list chat models", description = "", tags = { "chat-model" })
	@GetMapping(value = "", produces = { "application/json" })
	default R<List<ChatModel>> list() {
		List<ChatModel> res = getDelegate().list();
		return R.success(res);
	}

	@Operation(summary = "get chat model by model name", description = "", tags = { "chat-model" })
	@GetMapping(value = "/{modelName}", produces = { "application/json" })
	default R<ChatModel> get(@PathVariable String modelName) {
		ChatModel res = getDelegate().getByModelName(modelName);
		return R.success(res);
	}

	@Operation(summary = "run chat model by input", description = "", tags = { "chat-model" })
	@PostMapping(value = "", consumes = { MediaType.APPLICATION_JSON_VALUE })
	default R<ChatModelRunResult> run(@RequestBody ModelRunActionParam modelRunActionParam) {
		ChatModelRunResult res = getDelegate().run(modelRunActionParam);
		return R.success(res);
	}

	@Operation(summary = "run image model by input", description = "", tags = { "chat-model" })
	@PostMapping(value = "/run/image-gen", consumes = { MediaType.APPLICATION_JSON_VALUE },
			produces = { MediaType.ALL_VALUE })
	default void runImageGenTask(@RequestBody ModelRunActionParam modelRunActionParam, HttpServletResponse response) {
		String imageUrl = getDelegate().runImageGenTask(modelRunActionParam);
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

	@Operation(summary = "run image model by input, and url", description = "", tags = { "chat-model" })
	@RequestMapping(value = "/run/image-gen/url", method = { RequestMethod.POST, RequestMethod.GET },
			consumes = { MediaType.APPLICATION_JSON_VALUE }, produces = { MediaType.ALL_VALUE })
	default R<ChatModelRunResult> runImageGenTaskAndGetUrl(@RequestBody ModelRunActionParam modelRunActionParam) {
		return R.success(getDelegate().runImageGenTaskAndGetUrl(modelRunActionParam));
	}

	@Operation(summary = "list model names", description = "", tags = { "chat-model" })
	@GetMapping(value = "model-names", produces = { "application/json" })
	default R<List<String>> listModelNames(@RequestParam("modelType") ModelType modelType) {
		List<String> res = getDelegate().listModelNames(modelType);
		return R.success(res);
	}

}
