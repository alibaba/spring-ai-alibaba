package com.alibaba.cloud.ai.toolcalling.commontoolcall;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;

public class JsonParseService {

	private final ObjectMapper objectMapper;

	JsonParseService() {
		this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}

	public String objectToJson(Object obj) throws JsonProcessingException {
		return objectMapper.writeValueAsString(obj);
	}

	public <T> T jsonToObject(String json, Class<T> clazz) throws JsonProcessingException {
		return objectMapper.readValue(json, clazz);
	}

	public <T> List<T> jsonToList(String json, Class<T> clazz) throws JsonProcessingException {
		JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, clazz);
		return objectMapper.readValue(json, type);
	}

	public <T> T getFieldValue(String json, Class<T> clazz, String fieldName) throws JsonProcessingException {
		JsonNode rootNode = objectMapper.readTree(json);
		JsonNode fieldNode = rootNode.path(fieldName);
		return objectMapper.treeToValue(fieldNode, clazz);
	}

}
