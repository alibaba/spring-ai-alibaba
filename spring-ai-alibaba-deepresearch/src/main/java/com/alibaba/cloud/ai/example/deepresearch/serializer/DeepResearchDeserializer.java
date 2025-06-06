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