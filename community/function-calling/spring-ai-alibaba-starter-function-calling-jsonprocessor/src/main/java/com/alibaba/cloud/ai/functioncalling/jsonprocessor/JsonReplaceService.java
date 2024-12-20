package com.alibaba.cloud.ai.functioncalling.jsonprocessor;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.util.Assert;

import java.util.function.Function;

/**
 * @author 北极星
 */
public class JsonReplaceService implements Function<JsonReplaceService.JsonReplaceRequest, Object> {

	@Override
	public Object apply(JsonReplaceRequest request) {
		String content = request.content;
		String field = request.field;
		JsonElement value = request.value;
		JsonElement jsonElement = JsonParser.parseString(content);
		if (!jsonElement.isJsonObject())
			throw new IllegalArgumentException("Content is not a valid JSON object .");
		JsonObject jsonObject = jsonElement.getAsJsonObject();
		Assert.notNull(field, "replace json field can not be null");
		Assert.notNull(value, "replace json fieldValue can not be null");
		jsonObject.add(field, value);
		return jsonObject;
	}

	record JsonReplaceRequest(@JsonProperty("content") String content, @JsonProperty("field") String field,
			@JsonProperty("value") JsonElement value) {
	}

}
