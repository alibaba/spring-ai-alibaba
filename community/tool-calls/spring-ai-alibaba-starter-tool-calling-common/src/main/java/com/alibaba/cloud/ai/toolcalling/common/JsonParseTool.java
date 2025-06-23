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
import java.util.Map;

/**
 * @author vlsmb
 */
public class JsonParseTool {

	private final ObjectMapper objectMapper;

	JsonParseTool() {
		this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}

	public JsonParseTool(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
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

	/**
	 * convert json string to List
	 * @param json json string
	 * @param clazz class in List
	 */
	public <T> List<T> jsonToList(String json, Class<T> clazz) throws JsonProcessingException {
		JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, clazz);
		return objectMapper.readValue(json, type);
	}

	/**
	 * convert json string to List
	 * @param json json string
	 * @param typeRef class in List, TypeReference object
	 */
	public <T> List<T> jsonToList(String json, TypeReference<T> typeRef) throws JsonProcessingException {
		JavaType type = objectMapper.getTypeFactory().constructType(typeRef);
		return objectMapper.readValue(json, type);
	}

	/**
	 * convert json string to map
	 * @param json json string
	 * @param clazz class in Map Value
	 */
	public <T> Map<String, T> jsonToMap(String json, Class<T> clazz) throws JsonProcessingException {
		JavaType type = objectMapper.getTypeFactory().constructMapType(Map.class, String.class, clazz);
		return objectMapper.readValue(json, type);
	}

	/**
	 * Deserialize the JSON string's 'fieldName' key into an object of type T
	 * @param json json string
	 * @param typeRef target class
	 * @param fieldName keyName
	 */
	public <T> T getFieldValue(String json, TypeReference<T> typeRef, String fieldName) throws JsonProcessingException {
		JsonNode rootNode = objectMapper.readTree(json);
		JsonNode fieldNode = rootNode.path(fieldName);
		return objectMapper.treeToValue(fieldNode, typeRef);
	}

	/**
	 * Deserialize the JSON string's 'fieldName' key into an object of type T
	 * @param json json string
	 * @param clazz target class
	 * @param fieldName keyName
	 */
	public <T> T getFieldValue(String json, Class<T> clazz, String fieldName) throws JsonProcessingException {
		JsonNode rootNode = objectMapper.readTree(json);
		JsonNode fieldNode = rootNode.path(fieldName);
		return objectMapper.treeToValue(fieldNode, clazz);
	}

	/**
	 * Get the string value of 'fieldName' from the JSON.
	 * @param json json string
	 * @param fieldName keyName
	 */
	public String getFieldValueAsString(String json, String fieldName) throws JsonProcessingException {
		JsonNode rootNode = objectMapper.readTree(json);
		JsonNode fieldNode = rootNode.path(fieldName);
		return fieldNode.toString();
	}

	/**
	 * Get the text value of 'fieldName' from the JSON.
	 * @param json json string
	 * @param fieldName keyName
	 */
	public String getFieldValueAsText(String json, String fieldName) throws JsonProcessingException {
		JsonNode rootNode = objectMapper.readTree(json);
		JsonNode fieldNode = rootNode.get(fieldName);
		return fieldNode.asText();
	}

	/**
	 * Get the object of obj.fieldName1.fileName2... from the JSON.
	 * @param json json string
	 * @param fieldNames keyNames
	 */
	public <T> T getDepthFieldValue(String json, TypeReference<T> typeRef, String... fieldNames)
			throws JsonProcessingException {
		return this.jsonToObject(this.getDepthFieldValueAsString(json, fieldNames), typeRef);
	}

	/**
	 * Get the string value of obj.fieldName1.fileName2... from the JSON.
	 * @param json json string
	 * @param fieldNames keyNames
	 */
	public String getDepthFieldValueAsString(String json, String... fieldNames) throws JsonProcessingException {
		for (String fieldName : fieldNames) {
			json = getFieldValueAsString(json, fieldName);
		}
		return json;
	}

	/**
	 * Assign 'value' to 'fieldName' in the JSON
	 * @param json json string
	 * @param fieldName fieldName
	 * @param value value
	 */
	public String setFieldValue(String json, String fieldName, String value) throws JsonProcessingException {
		JsonNode jsonNode = objectMapper.readTree(json);
		if (!(jsonNode instanceof ObjectNode rootNode)) {
			throw new RuntimeException("no json object string");
		}
		rootNode.put(fieldName, value);
		return objectMapper.writeValueAsString(rootNode);
	}

	/**
	 * Assign 'value' to 'fieldName' in the JSON, then return JsonNode
	 * @param json json string
	 * @param fieldName fieldName
	 * @param value JsonNode value
	 */
	public Object setFieldValue(String json, String fieldName, JsonNode value) throws JsonProcessingException {
		JsonNode jsonNode = objectMapper.readTree(json);
		if (!(jsonNode instanceof ObjectNode rootNode)) {
			throw new RuntimeException("no json object string");
		}
		return rootNode.set(fieldName, value);
	}

	/**
	 * Remove 'fieldName' in the JSON, then return JsonNode
	 * @param json json string
	 * @param fieldName fieldName
	 */
	public Object removeFieldValue(String json, String fieldName) throws JsonProcessingException {
		JsonNode jsonNode = objectMapper.readTree(json);
		if (!(jsonNode instanceof ObjectNode rootNode)) {
			throw new RuntimeException("no json object string");
		}
		return rootNode.remove(fieldName);
	}

	/**
	 * Replace 'value' to 'fieldName' in the JSON, then return JsonNode
	 * @param json json string
	 * @param fieldName fieldName
	 * @param value JsonNode value
	 */
	public Object replaceFieldValue(String json, String fieldName, JsonNode value) throws JsonProcessingException {
		JsonNode jsonNode = objectMapper.readTree(json);
		if (!(jsonNode instanceof ObjectNode rootNode)) {
			throw new RuntimeException("no json object string");
		}
		rootNode.replace(fieldName, value);
		return jsonNode;
	}

	/**
	 * Parse and merge the sub-JSON into the main JSON's 'fieldName', then stringify the
	 * result Ensure the JSON is not an array
	 * @param json main json string
	 * @param fieldName fieldName
	 * @param objectJson sub json string
	 */
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

	/**
	 * Parse and merge multiple sub-JSONs into the main JSON's 'fieldName', then stringify
	 * Ensure the JSON is not an array
	 * @param json main json string
	 * @param fieldName fieldName
	 * @param objectJsons sub json string list
	 */
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

	/**
	 * Get first element from json array string, then return its json string.
	 * @param arrayJson array json string
	 * @return element json string
	 */
	public String getFirstElementFromJsonArrayString(String arrayJson) throws JsonProcessingException {
		JsonNode jsonNode = objectMapper.readTree(arrayJson);
		if (jsonNode.isArray() && !jsonNode.isEmpty()) {
			JsonNode firstElement = jsonNode.get(0);
			return objectMapper.writeValueAsString(firstElement);
		}
		else {
			return null;
		}
	}

}
