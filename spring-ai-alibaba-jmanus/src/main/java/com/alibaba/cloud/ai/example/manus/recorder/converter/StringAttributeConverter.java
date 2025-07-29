/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus.recorder.converter;

import com.alibaba.cloud.ai.example.manus.recorder.JManusSpringEnvironmentHolder;
import com.alibaba.cloud.ai.example.manus.recorder.SerializeType;
import com.alibaba.cloud.ai.example.manus.recorder.entity.PlanExecutionRecord;
import com.alibaba.fastjson2.JSON;
import jakarta.persistence.AttributeConverter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.util.json.JsonParser;

public class StringAttributeConverter implements AttributeConverter<PlanExecutionRecord, String> {

	private final static String SERIALIZE_TYPE_KEY = "agent.serialize";

	@Override
	public String convertToDatabaseColumn(PlanExecutionRecord attribute) {
		if (attribute == null) {
			return null;
		}
		if (SerializeType.FASTJSON
			.equalsIgnoreCase(JManusSpringEnvironmentHolder.getEnvironment().getProperty(SERIALIZE_TYPE_KEY))) {
			return JSON.toJSONString(attribute);
		}
		return JsonParser.toJson(attribute);
	}

	@Override
	public PlanExecutionRecord convertToEntityAttribute(String json) {
		if (StringUtils.isBlank(json)) {
			return null;
		}
		if (SerializeType.FASTJSON
			.equalsIgnoreCase(JManusSpringEnvironmentHolder.getEnvironment().getProperty(SERIALIZE_TYPE_KEY))) {
			return JSON.parseObject(json, PlanExecutionRecord.class);
		}
		return JsonParser.fromJson(json, PlanExecutionRecord.class);
	}

}
