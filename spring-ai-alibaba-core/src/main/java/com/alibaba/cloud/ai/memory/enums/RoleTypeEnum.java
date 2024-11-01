package com.alibaba.cloud.ai.memory.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Title role type enumeration.<br>
 * Description configure different role type enumerations.<br>
 *
 * @author zhych1005
 * @since 1.0.0-M3
 */

@Getter
@AllArgsConstructor
public enum RoleTypeEnum {

	USER("user"), ASSISTANT("assistant");

	private final String roleName;

	@Override
	public String toString() {
		return roleName;
	}

}