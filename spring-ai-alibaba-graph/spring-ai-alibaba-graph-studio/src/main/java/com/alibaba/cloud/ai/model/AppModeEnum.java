package com.alibaba.cloud.ai.model;

import java.util.Arrays;
import java.util.Objects;

public enum AppModeEnum {

	WORKFLOW("workflow");

	private String value;

	AppModeEnum(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public static AppModeEnum of(String val) {
		return Arrays.stream(AppModeEnum.values())
			.filter(appMode -> Objects.equals(appMode.value, val))
			.findFirst()
			.orElse(null);
	}

}
