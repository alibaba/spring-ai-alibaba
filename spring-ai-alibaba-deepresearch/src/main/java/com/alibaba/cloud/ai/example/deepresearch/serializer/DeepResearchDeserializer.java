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
package com.alibaba.cloud.ai.example.deepresearch.serializer;

import com.alibaba.cloud.ai.example.deepresearch.model.Plan;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.toolcalling.tavily.TavilySearchService;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.ai.chat.messages.Message;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DeepResearchDeserializer extends JsonDeserializer<OverAllState> {

	private final ObjectMapper objectMapper;

	public DeepResearchDeserializer(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public OverAllState deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		ObjectNode node = objectMapper.readTree(p);

		Map<String, Object> data = objectMapper.convertValue(node.get("data"), new TypeReference<>() {
		});

		Plan currentPlan = objectMapper.convertValue(data.get("current_plan"), Plan.class);
		List<TavilySearchService.SearchContent> backgroundInvestigationResults = objectMapper
			.convertValue(data.get("background_investigation_results"), objectMapper.getTypeFactory()
				.constructCollectionType(List.class, TavilySearchService.SearchContent.class));
		// use objectMapper to Deserialize Message
		List<Message> messages = objectMapper.readValue(objectMapper.writeValueAsString(data.get("messages")),
				objectMapper.getTypeFactory().constructCollectionType(List.class, Message.class));

		Map<String, Object> newData = new HashMap<>();
		newData.put("current_plan", currentPlan);
		newData.put("background_investigation_results", backgroundInvestigationResults);
		newData.put("messages", messages);

		data.forEach((key, value) -> {
			if (!newData.containsKey(key)) {
				newData.put(key, value);
			}
		});

		return new OverAllState(newData);
	}

}
