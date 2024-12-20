package com.alibaba.cloud.ai.dashscope.common;

/**
 * @author nuocheng.lxm
 * @since 1.0.0-M2
 */
public class DashScopeException extends RuntimeException {

	public DashScopeException(String message) {
		super(message);
	}

	public DashScopeException(String message, Throwable cause) {
		super(message, cause);
	}

	public DashScopeException(ErrorCodeEnum error) {
		super(error.getCode() + ":" + error.message());
	}

}
