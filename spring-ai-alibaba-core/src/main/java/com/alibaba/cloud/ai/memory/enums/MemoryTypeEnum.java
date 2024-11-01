package com.alibaba.cloud.ai.memory.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Title memory type enumeration.<br>
 * Description configure different memory type enumerations.<br>
 *
 * @author zhych1005
 * @since 1.0.0-M3
 */

@Getter
@AllArgsConstructor
public enum MemoryTypeEnum {

	BUFFERWINDOW("bufferwindow");

	private final String memoryTypeName;

}