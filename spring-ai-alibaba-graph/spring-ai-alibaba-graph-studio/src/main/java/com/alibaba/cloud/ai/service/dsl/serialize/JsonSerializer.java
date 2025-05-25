package com.alibaba.cloud.ai.service.dsl.serialize;

import com.alibaba.cloud.ai.exception.SerializationException;
import com.alibaba.cloud.ai.service.dsl.Serializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("json")
public class JsonSerializer implements Serializer {

	private ObjectMapper objectMapper;

	public JsonSerializer() {
		objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	@Override
	public Map<String, Object> load(String s) {
		return objectMapper.convertValue(s, new TypeReference<>() {
		});
	}

	@Override
	public String dump(Map<String, Object> data) {
		String result;
		try {
			result = objectMapper.writeValueAsString(data);
		}
		catch (JsonProcessingException e) {
			throw new SerializationException("failed to dump data" + data, e);
		}
		return result;
	}

}
