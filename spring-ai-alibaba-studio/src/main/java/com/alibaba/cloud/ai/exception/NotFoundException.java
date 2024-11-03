package com.alibaba.cloud.ai.exception;

import com.alibaba.cloud.ai.common.ReturnCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotFoundException extends RuntimeException {

	private int code;

	private String msg;

	public NotFoundException() {
		this.code = ReturnCode.RC404.getCode();
		this.msg = ReturnCode.RC404.getMsg();
	}

	public NotFoundException(String msg) {
		this.code = ReturnCode.RC404.getCode();
		this.msg = msg;
	}

}
