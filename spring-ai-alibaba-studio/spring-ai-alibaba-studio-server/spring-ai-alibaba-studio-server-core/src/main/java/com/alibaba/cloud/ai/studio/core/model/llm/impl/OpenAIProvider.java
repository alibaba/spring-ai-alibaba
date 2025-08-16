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
package com.alibaba.cloud.ai.studio.core.model.llm.impl;

import com.google.common.collect.Lists;
import com.alibaba.cloud.ai.studio.runtime.domain.model.CredentialSpec;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.enums.ParameterTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.core.model.llm.ModelProvider;
import com.alibaba.cloud.ai.studio.core.model.llm.domain.ModelConfigInfo;
import com.alibaba.cloud.ai.studio.core.model.llm.domain.ParameterRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * OpenAI provider implementation for LLM service
 */
@Slf4j
@Component("OpenAIProvider")
public class OpenAIProvider implements ModelProvider {

	@Override
	public String getCode() {
		return "OpenAI";
	}

	@Override
	public String getName() {
		return "OpenAI";
	}

	@Override
	public String getDescription() {
		return "";
	}

	@Override
	public List<ModelConfigInfo> getPresetModels() {
		return List.of();
	}

	@Override
	public String getProtocol() {
		return "OpenAI";
	}

	@Override
	public String getEndpoint() {
		return "";
	}

	@Override
	public boolean validateCredentials(List<CredentialSpec> credentialSpecs, Map<String, Object> credentialMap) {
		return false;
	}

	@Override
	public List<CredentialSpec> getCredentialSpecs() {
		return List.of(
				new CredentialSpec().setCode("api_key")
					.setDisplayName("API Key")
					.setDescription("API key required to authenticate with the remote model service")
					.setPlaceHolder("Enter API key")
					.setSensitive(true),
				new CredentialSpec().setCode("endpoint")
					.setDisplayName("Endpoint")
					.setDescription("Endpoint required to call the remote model service")
					.setPlaceHolder("Enter endpoint")
					.setSensitive(false));
	}

	@Override
	public List<ParameterRule> getParameterRules(String modelId, String modelType) {
		ModelConfigInfo.ModelTypeEnum modelTypeEnum = ModelConfigInfo.ModelTypeEnum.valueOf(modelType);
		if (modelTypeEnum == null) {
			throw new BizException(ErrorCode.INVALID_PARAMS.toError("modelType", "modelType is invalid"));
		}
		switch (modelTypeEnum) {
			case llm -> {
				return defaultLLMParameterRules;
			}
			default -> {
				return Lists.newArrayList();
			}
		}
	}

	/**
	 * Default parameter rules for LLM models
	 */
	private final static List<ParameterRule> defaultLLMParameterRules = Lists.newArrayList(new ParameterRule()
		.setCode("temperature")
		.setName("temperature")
		.setPrecision(2)
		.setRequired(false)
		.setType(ParameterTypeEnum.NUMBER.getCode())
		.setDefaultValue(0)
		.setMin(0)
		.setMax(1)
		.setDescription(
				"Controls randomness. Lower temperature results in less random completions. As the temperature approaches zero, the model will become deterministic and repetitive. Higher temperature results in more random completions.")
		.setHelp(Map.of("en_US",
				"Controls randomness. Lower temperature results in less random completions. As the temperature approaches zero, the model will become deterministic and repetitive. Higher temperature results in more random completions.",
				"zh_Hans", "温度控制随机性。较低的温度会导致较少的随机完成。随着温度接近零，模型将变得确定性和重复性。较高的温度会导致更多的随机完成。")),
			new ParameterRule().setCode("top_p")
				.setName("top_p")
				.setPrecision(2)
				.setRequired(false)
				.setType(ParameterTypeEnum.NUMBER.getCode())
				.setDefaultValue(1)
				.setMin(0)
				.setMax(1)
				.setDescription(
						"Controls diversity via nucleus sampling: 0.5 means half of all likelihood-weighted options are considered.")
				.setHelp(Map.of("en_US",
						"Controls diversity via nucleus sampling: 0.5 means half of all likelihood-weighted options are considered.",
						"zh_Hans", "通过核心采样控制多样性：0.5表示考虑了一半的所有可能性加权选项。")),
			new ParameterRule().setCode("presence_penalty")
				.setName("presence_penalty")
				.setPrecision(2)
				.setRequired(false)
				.setType(ParameterTypeEnum.NUMBER.getCode())
				.setDefaultValue(0)
				.setMin(0)
				.setMax(1)
				.setDescription("Applies a penalty to the log-probability of tokens already in the text.")
				.setHelp(Map.of("en_US", "Applies a penalty to the log-probability of tokens already in the text.",
						"zh_Hans", "对文本中已有的标记的对数概率施加惩罚。")),
			new ParameterRule().setCode("frequency_penalty")
				.setName("frequency_penalty")
				.setPrecision(2)
				.setRequired(false)
				.setType(ParameterTypeEnum.NUMBER.getCode())
				.setDefaultValue(0)
				.setMin(0)
				.setMax(1)
				.setDescription("Applies a penalty to the log-probability of tokens that appear in the text.")
				.setHelp(Map.of("en_US", "Applies a penalty to the log-probability of tokens that appear in the text.",
						"zh_Hans", "对文本中出现的标记的对数概率施加惩罚。")),
			new ParameterRule().setCode("max_tokens")
				.setName("max_tokens")
				.setPrecision(0)
				.setRequired(false)
				.setType(ParameterTypeEnum.NUMBER.getCode())
				.setDefaultValue(512)
				.setMin(1)
				.setMax(4096)
				.setDescription(
						"Specifies the upper limit on the length of generated results. If the generated results are truncated, you can increase this parameter.")
				.setHelp(Map.of("en_US",
						"Specifies the upper limit on the length of generated results. If the generated results are truncated, you can increase this parameter.",
						"zh_Hans", "指定生成结果长度的上限。如果生成结果截断，可以调大该参数。")),
			new ParameterRule().setCode("seed")
				.setName("seed")
				.setRequired(false)
				.setType(ParameterTypeEnum.NUMBER.getCode())
				.setDescription(
						"If specified, model will make a best effort to sample deterministically, such that repeated requests with the same seed and parameters should return the same result. Determinism is not guaranteed, and you should refer to the system_fingerprint response parameter to monitor changes.")
				.setHelp(Map.of("en_US",
						"If specified, model will make a best effort to sample deterministically, such that repeated requests with the same seed and parameters should return the same result. Determinism is not guaranteed, and you should refer to the system_fingerprint response parameter to monitor changes in the backend.",
						"zh_Hans",
						"如��指定，模型将尽最大努力进行确定性采样，使得重复的具有相同种子和参数的请求应该返回相同的结果。不能保证确定性，您应该参考 system_fingerprint 响应参数来监视变化。")));

}
