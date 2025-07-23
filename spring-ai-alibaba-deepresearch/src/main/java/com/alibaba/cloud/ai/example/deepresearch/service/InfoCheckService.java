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

import com.alibaba.cloud.ai.example.deepresearch.util.ResourceUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.core.io.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class InfoCheckService {

	private static final Logger logger = LoggerFactory.getLogger(InfoCheckService.class);

	@Autowired
	private ChatClient infoCheckAgent;

	@Value("classpath:prompts/backgroundInfoCheck.md")
	private Resource backgroundInfoCheckTemplate;

	public String backgroundInfoCheck(List<Map<String, String>> results, String query)
			throws JsonProcessingException, InterruptedException {

		List<String> correctedPost = new ArrayList<>();
		// load template & replace the placeholder
		String promptTemplate = ResourceUtil.loadResourceAsString(backgroundInfoCheckTemplate);
		String fullPrompt = promptTemplate.replace("{{query}}", query);
		for (Map<String, String> result : results) {
			fullPrompt = fullPrompt.replace("{{backgroundResults}}", String.join("\n", result.get("content")));
			List<Message> messages = Arrays.asList(new SystemMessage("你严格遵守JSON格式输出规范"), new UserMessage(fullPrompt));
			String filterResult = infoCheckAgent.prompt().messages(messages).call().content();

			// handle json result
			ObjectMapper mapper = new ObjectMapper();
			JsonNode jsonNode = mapper.readTree(filterResult);
			boolean isSensitive = true;
			isSensitive = jsonNode.get("is_sensitive").asBoolean();
			boolean isIrrelevant = true;
			isIrrelevant = jsonNode.get("is_irrelevant").asBoolean();
			if (!isSensitive && !isIrrelevant) {
				String post = "title:\n" + result.get("title") + "\n" + "url:\n" + result.get("url") + "\n"
						+ "content:\n" + result.get("content") + "\n";

				correctedPost.add(post);
			}
			Thread.sleep(100);
		}
		logger.info("过滤结果: {}条", correctedPost.size());
		return String.join("\n", correctedPost);
	}

}
