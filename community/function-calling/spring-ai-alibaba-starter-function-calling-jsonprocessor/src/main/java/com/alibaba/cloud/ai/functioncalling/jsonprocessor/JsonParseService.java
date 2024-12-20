package com.alibaba.cloud.ai.functioncalling.jsonprocessor;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.function.Function;

/**
 * @author 北极星
 */
public class JsonParseService implements Function<JsonParseService.JsonParseRequest, Object> {

	@Override
	public Object apply(JsonParseRequest request) throws JsonParseException {
		String content = request.content;
		String field = request.field;
		JsonElement jsonElement = JsonParser.parseString(content);
		return jsonElement.getAsJsonObject().get(field).getAsString();
	}

	record JsonParseRequest(@JsonProperty("content") String content, @JsonProperty("value") String field) {
	}

}
