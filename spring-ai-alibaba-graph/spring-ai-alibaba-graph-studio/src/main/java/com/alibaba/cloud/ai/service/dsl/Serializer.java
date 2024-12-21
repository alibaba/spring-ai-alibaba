package com.alibaba.cloud.ai.service.dsl;

import java.util.Map;

/**
 * Serializer is used to serialize/deserialize specific type of DSL. e.g. json, yaml
 */
public interface Serializer {

	Map<String, Object> load(String s);

	String dump(Map<String, Object> data);

}
