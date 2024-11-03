package com.alibaba.cloud.ai.common;

import lombok.Getter;

@Getter
public enum ReturnCode {

	RC200(200, "OK"), RC400(400, "INVALID_ARGUMENT"), RC404(404, "NOT_FOUND"), RC409(409, "ABORTED"),
	RC500(500, "INTERNAL"), RC501(501, "NOT_IMPLEMENTED");

	private final int code;

	private final String msg;

	ReturnCode(int code, String msg) {
		this.code = code;
		this.msg = msg;
	}

}
