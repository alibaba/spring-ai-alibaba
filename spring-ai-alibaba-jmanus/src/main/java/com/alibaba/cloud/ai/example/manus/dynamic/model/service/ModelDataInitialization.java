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
package com.alibaba.cloud.ai.example.manus.dynamic.model.service;

import com.alibaba.cloud.ai.example.manus.dynamic.model.entity.DynamicModelEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.model.model.enums.ModelType;
import com.alibaba.cloud.ai.example.manus.dynamic.model.repository.DynamicModelRepository;
import com.alibaba.cloud.ai.example.manus.event.JmanusEventPublisher;
import com.alibaba.cloud.ai.example.manus.event.ModelChangeEvent;
import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author lizhenning
 * @date 2025/7/8
 */
@Service
public class ModelDataInitialization implements IModelDataInitialization {

	@Value("${spring.ai.openai.api-key}")
	private String openAiApiKey;

	@Value("${spring.ai.openai.base-url}")
	private String baseUrl;

	@Value("${spring.ai.openai.chat.options.model}")
	private String model;

	@Autowired
	private OpenAiChatModel openAiChatModel;

	// 为了保障llmService先初始化
	@Autowired
	private LlmService llmService;

	@Autowired
	private JmanusEventPublisher jmanusEventPublisher;

	private final DynamicModelRepository repository;

	public ModelDataInitialization(DynamicModelRepository repository) {
		this.repository = repository;
	}

	@PostConstruct
	public void init() {

		OpenAiChatOptions defaultOptions = (OpenAiChatOptions) openAiChatModel.getDefaultOptions();
		// 保持固定id，每次启动配置的模型都将覆盖存储
		DynamicModelEntity dynamicModelEntity = new DynamicModelEntity();
		dynamicModelEntity.setBaseUrl(baseUrl);
		Map<String, String> httpHeaders = defaultOptions.getHttpHeaders();
		if (httpHeaders.isEmpty()) {
			httpHeaders = null;
		}
		dynamicModelEntity.setHeaders(httpHeaders);
		dynamicModelEntity.setApiKey(openAiApiKey);
		dynamicModelEntity.setModelName(model);
		dynamicModelEntity.setModelDescription("base model");
		dynamicModelEntity.setType(ModelType.GENERAL.name());
		DynamicModelEntity existingModel = repository.findByModelName(model);
		if (existingModel != null) {
			dynamicModelEntity.setId(existingModel.getId());
		}
		DynamicModelEntity save = repository.save(dynamicModelEntity);
		jmanusEventPublisher.publish(new ModelChangeEvent(save));

	}

}
