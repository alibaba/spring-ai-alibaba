package com.alibaba.cloud.ai.util;

import com.alibaba.cloud.ai.constant.StreamResponseType;
import com.google.gson.Gson;

import java.util.Map;

public class JsonUtils {

	private static final Gson gson = new Gson();

	public static String toJson(StreamResponseType type, String data) {
		return gson.toJson(Map.of("type", type.getValue(), "data", data));
	}

}
