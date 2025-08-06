package com.alibaba.cloud.ai.example.manus.inhouse.mcp;

/**
 * McpPlan转换异常类
 */
public class McpPlanConversionException extends RuntimeException {

	public McpPlanConversionException(String message) {
		super(message);
	}

	public McpPlanConversionException(String message, Throwable cause) {
		super(message, cause);
	}

}
