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
package com.alibaba.cloud.ai.advisor;

import com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.function.Consumer;

/**
 * 使用 qwen-long 模型解析文档
 *
 * 使用条件： 模型为qwen-long 将advisor加入到链路中
 * advisor添加名为resource的param，其值为Resource类型，可以为本地文件，也可以为URL
 *
 * 若无附件则视为普通对话，不上传文件和修改提示词
 *
 * use qwen-long model to parse document use condition: model is qwen-long, add advisor to
 * chain add param named resource, its value is Resource type, can be local file or URL if
 * no attachment, treat as normal conversation, no need to upload file and modify prompt
 *
 * @author HunterPorter
 * @author <a href="mailto:zongpeng_hzp@163.com">HunterPorter</a>
 */
public class DashScopeDocumentAnalysisAdvisor implements BaseAdvisor {

	public static final String UPLOAD_RESPONSE = "upload_response";

	public static final String RESOURCE = "resource";

	private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = new PromptTemplate("""
			fileid://{id}""");

	private final int order;

	WebClient webClient;

	public DashScopeDocumentAnalysisAdvisor(ApiKey apiKey) {
		this(0, apiKey);
	}

	public DashScopeDocumentAnalysisAdvisor(int order, ApiKey apiKey) {
		Assert.notNull(apiKey, "Invalid api key");
		this.order = order;

		// Check API Key in headers.
		Consumer<HttpHeaders> finalHeaders = h -> {
			if (!(apiKey instanceof NoopApiKey)) {
				h.setBearerAuth(apiKey.getValue());
			}

			h.setContentType(MediaType.MULTIPART_FORM_DATA);
		};

		this.webClient = WebClient.builder()
			.baseUrl(DashScopeApiConstants.DEFAULT_BASE_URL)
			.defaultHeaders(finalHeaders)
			.build();
	}

	@Override
	public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
		var context = chatClientRequest.context();
		Resource resource = (Resource) context.get(RESOURCE);
		if (resource != null) {

			ResponseEntity<UploadResponse> uploadResponse = upload(resource);
			context.put(UPLOAD_RESPONSE, uploadResponse);

			Assert.notNull(uploadResponse.getBody(), "upload response body is null");

			String augmentSystemMessage = DEFAULT_PROMPT_TEMPLATE.render(Map.of("id", uploadResponse.getBody().id));
			return chatClientRequest.mutate()
				.prompt(chatClientRequest.prompt().augmentSystemMessage(augmentSystemMessage))
				.build();
		}
		return chatClientRequest;
	}

	@Override
	public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
		if (chatClientResponse.chatResponse() == null || chatClientResponse.chatResponse().getResults() == null) {
			return chatClientResponse;
		}
		ChatResponse.Builder chatResponseBuilder = ChatResponse.builder().from(chatClientResponse.chatResponse());
		if (chatClientResponse.context().containsKey(UPLOAD_RESPONSE)) {
			chatResponseBuilder.metadata(UPLOAD_RESPONSE, chatClientResponse.context().get(UPLOAD_RESPONSE));
		}
		return ChatClientResponse.builder()
			.chatResponse(chatResponseBuilder.build())
			.context(chatClientResponse.context())
			.build();
	}

	public ResponseEntity<UploadResponse> upload(Resource resource) {

		MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
		formData.add("file", resource);
		formData.add("purpose", "file-extract");

		return this.webClient.post()
			.uri("/compatible-mode/v1/files")
			.body(BodyInserters.fromMultipartData(formData))
			.retrieve()
			.toEntity(UploadResponse.class)
			.block();
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record UploadResponse(@JsonProperty("id") String id, @JsonProperty("object") String object,
			@JsonProperty("bytes") int bytes, @JsonProperty("filename") String filename,
			@JsonProperty("purpose") String purpose, @JsonProperty("status") String status,
			@JsonProperty("created_at") String createdAt) {
	}

}
