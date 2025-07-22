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
package com.alibaba.cloud.ai.example.deepresearch.service;

import com.alibaba.cloud.ai.example.deepresearch.agents.AgentFactory;
import com.alibaba.cloud.ai.example.deepresearch.repository.ModelParamRepository;
import com.alibaba.cloud.ai.example.deepresearch.repository.ModelParamRepositoryImpl;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.util.List;

@Service
public class ModelConfigService {

	private static final String CONFIG_PATH = "model-config.json";

	private final ModelParamRepository modelParamRepository;

	private final AgentFactory AgentFactory;

	public ModelConfigService(ModelParamRepository modelParamRepository, AgentFactory AgentFactory) {
		this.modelParamRepository = modelParamRepository;
		this.AgentFactory = AgentFactory;
	}

	@PostConstruct
	private void init() throws IOException {
		URL resource = getClass().getClassLoader().getResource(CONFIG_PATH);
		if (resource == null) {
			throw new IOException("model-config.json not found in resources");
		}
	}

	public List<ModelParamRepositoryImpl.AgentModel> getModelConfigs() throws IOException {
		return modelParamRepository.loadModels();
	}

	public void updateModelConfigs(List<ModelParamRepositoryImpl.AgentModel> models) throws IOException {
		// todo: 这里可以增加修改配置文件的逻辑
		AgentFactory.batchUpdateAgents(models);
	}

}
