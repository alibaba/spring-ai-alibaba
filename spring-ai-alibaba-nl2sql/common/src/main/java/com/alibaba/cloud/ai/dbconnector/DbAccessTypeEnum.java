package com.alibaba.cloud.ai.dbconnector;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum DbAccessTypeEnum {

	JDBC("jdbc"),

	SDK("sdk"),

	DATA_API("data-api"),

	FC_HTTP("fc-http"),

	MEMORY("in-memory");

	private String code;

	DbAccessTypeEnum(String code) {
		this.code = code;
	}

	public static DbAccessTypeEnum of(String code) {
		if (StringUtils.isBlank(code)) {
			return null;
		}

		Optional<DbAccessTypeEnum> any = Arrays.stream(values())
			.filter(typeEnum -> code.equals(typeEnum.getCode()))
			.findAny();

		return any.orElse(null);
	}

}
