package com.alibaba.cloud.ai.example.manus.recorder.converter;

import com.alibaba.cloud.ai.example.manus.recorder.entity.PlanExecutionRecord;
import com.alibaba.fastjson.JSON;
import jakarta.persistence.AttributeConverter;
import org.apache.commons.lang3.StringUtils;

public class StringAttributeConverter implements AttributeConverter<PlanExecutionRecord, String> {

	@Override
	public String convertToDatabaseColumn(PlanExecutionRecord attribute) {
		if (attribute == null) {
			return null;
		}
		return JSON.toJSONString(attribute);
	}

	@Override
	public PlanExecutionRecord convertToEntityAttribute(String json) {
		if (StringUtils.isBlank(json)) {
			return null;
		}
		return JSON.parseObject(json, PlanExecutionRecord.class);
	}

}
