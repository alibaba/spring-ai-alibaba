package com.alibaba.cloud.ai.studio.runtime.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enumeration for API plugin value sources
 *
 * @author guning.lt
 * @since 1.0.0-M1
 */
@Getter
public enum APIPluginValueSourceEnum {

	/**
	 * Model recognition source
	 */
	MODEL("model", "Model Recognition", "aiVars"),

	/**
	 * Business pass-through source
	 */
	BIZ("biz", "Business Pass-through", "bizVars");

	/**
	 * Source code identifier
	 */
	private final String code;

	/**
	 * Source description
	 */
	private final String desc;

	/**
	 * SDK field mapping
	 */
	private String sdkMapping;

	APIPluginValueSourceEnum(String code, String desc, String sdkMapping) {
		this.code = code;
		this.desc = desc;
		this.sdkMapping = sdkMapping;
	}

	/**
	 * Get enum value by code
	 * @param code source code
	 * @return corresponding enum value or null if not found
	 */
	public static APIPluginValueSourceEnum valueOfByCode(String code) {
		if (StringUtils.isBlank(code)) {
			return null;
		}
		Optional<APIPluginValueSourceEnum> anyOptional = Arrays.stream(values())
			.filter(valueFrom -> valueFrom.getCode().equals(code))
			.findAny();
		return anyOptional.orElse(null);
	}

}
