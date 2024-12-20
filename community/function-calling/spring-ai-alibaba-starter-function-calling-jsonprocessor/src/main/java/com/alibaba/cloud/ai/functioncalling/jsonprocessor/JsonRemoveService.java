package com.alibaba.cloud.ai.functioncalling.jsonprocessor;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.function.Function;

/**
 * @author 北极星
 */
public class JsonRemoveService implements Function<JsonRemoveService.JsonRemoveRequest, Object> {

	@Override
	public Object apply(JsonRemoveRequest request) throws JsonParseException {
		String content = request.content;
		String field = request.field;
		JsonElement jsonElement = JsonParser.parseString(content);
		if (!jsonElement.isJsonObject())
			throw new IllegalArgumentException("Content is not a valid JSON object .");
		JsonObject jsonObject = jsonElement.getAsJsonObject();
		return jsonObject.remove(field);
	}

	record JsonRemoveRequest(@JsonProperty("content") String content, @JsonProperty("value") String field) {
	}

}
