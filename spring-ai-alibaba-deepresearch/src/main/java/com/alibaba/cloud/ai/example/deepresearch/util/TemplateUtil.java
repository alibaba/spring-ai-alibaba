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

package com.alibaba.cloud.ai.example.deepresearch.util;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yingzi
 * @since 2025/5/17 17:20
 */

public class TemplateUtil {

	public static Message getMessage(String promptName) throws IOException {
		// 读取 resources/prompts 下的 md 文件
		ClassPathResource resource = new ClassPathResource("prompts/" + promptName + ".md");
		String template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

		// 替换 {{ CURRENT_TIME }} 占位符
		String systemPrompt = template.replace("{{ CURRENT_TIME }}", LocalDateTime.now().toString());
		SystemMessage systemMessage = new SystemMessage(systemPrompt);
		return systemMessage;
	}

	public static Message getMessage(String promptName, OverAllState state) throws IOException {
		// 读取 resources/prompts 下的 md 文件
		ClassPathResource resource = new ClassPathResource("prompts/" + promptName + ".md");
		String template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
		// 替换 {{ CURRENT_TIME }} 占位符
		String systemPrompt = template.replace("{{ CURRENT_TIME }}", LocalDateTime.now().toString());
		// 替换 {{ max_step_num }} 占位符
		systemPrompt = systemPrompt.replace("{{ max_step_num }}", StateUtil.getMaxStepNum(state).toString());

		SystemMessage systemMessage = new SystemMessage(systemPrompt);
		return systemMessage;
	}

	public static Message getOptQuryMessage(OverAllState state) throws IOException {
		List<String> queries = StateUtil.getOptimizeQueries(state);
		assert queries != null && !queries.isEmpty();
		String originalQuery = StateUtil.getQuery(state);

		List<String> results = queries.stream()
			.map(optimizeQuery -> "original query:" + originalQuery + "optimize query:" + optimizeQuery + "\n")
			.collect(Collectors.toList());

		Message userMessage = new UserMessage(String.valueOf(results));
		return userMessage;
	}

}
