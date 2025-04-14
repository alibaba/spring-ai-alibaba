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
package com.alibaba.cloud.ai.toolcalling.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;

/**
 * @author vlsmb
 */
public class JsonParseTool {

	private final ObjectMapper objectMapper;

	JsonParseTool() {
		this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}

	public <T> String objectToJson(T obj) throws JsonProcessingException {
		return objectMapper.writeValueAsString(obj);
	}

	public <T> T jsonToObject(String json, Class<T> clazz) throws JsonProcessingException {
		return objectMapper.readValue(json, clazz);
	}

	public <T> T jsonToObject(String json, TypeReference<T> typeRef) throws JsonProcessingException {
		return objectMapper.readValue(json, typeRef);
	}

	public <T> List<T> jsonToList(String json, Class<T> clazz) throws JsonProcessingException {
		JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, clazz);
		return objectMapper.readValue(json, type);
	}

	public <T> List<T> jsonToList(String json, TypeReference<T> typeRef) throws JsonProcessingException {
		JavaType type = objectMapper.getTypeFactory().constructType(typeRef);
		return objectMapper.readValue(json, type);
	}

	public <T> T getFieldValue(String json, TypeReference<T> typeRef, String fieldName) throws JsonProcessingException {
		JsonNode rootNode = objectMapper.readTree(json);
		JsonNode fieldNode = rootNode.path(fieldName);
		return objectMapper.treeToValue(fieldNode, typeRef);
	}

	public String getFieldValueAsString(String json, String fieldName) throws JsonProcessingException {
		JsonNode rootNode = objectMapper.readTree(json);
		JsonNode fieldNode = rootNode.path(fieldName);
		return fieldNode.toString();
	}

	public String setFieldValue(String json, String fieldName, String value) throws JsonProcessingException {
		JsonNode jsonNode = objectMapper.readTree(json);
		if (!(jsonNode instanceof ObjectNode rootNode)) {
			throw new RuntimeException("no json object string");
		}
		rootNode.put(fieldName, value);
		return objectMapper.writeValueAsString(rootNode);
	}

	public String setFieldJsonObjectAsString(String json, String fieldName, String objectJson)
			throws JsonProcessingException {
		JsonNode jsonNode = objectMapper.readTree(json);
		if (!(jsonNode instanceof ObjectNode rootNode)) {
			throw new RuntimeException("no json object string");
		}
		JsonNode objectNode = objectMapper.readTree(objectJson);
		rootNode.set(fieldName, objectNode);
		return objectMapper.writeValueAsString(rootNode);
	}

	public String setFieldJsonObjectAsString(String json, String fieldName, List<String> objectJsons)
			throws JsonProcessingException {
		JsonNode jsonNode = objectMapper.readTree(json);
		if (!(jsonNode instanceof ObjectNode rootNode)) {
			throw new RuntimeException("no json object string");
		}
		ArrayNode arrayNode = objectMapper.createArrayNode();
		for (String objectJson : objectJsons) {
			JsonNode newNode = objectMapper.readTree(objectJson);
			arrayNode.add(newNode);
		}
		rootNode.set(fieldName, arrayNode);
		return objectMapper.writeValueAsString(rootNode);
	}

}
