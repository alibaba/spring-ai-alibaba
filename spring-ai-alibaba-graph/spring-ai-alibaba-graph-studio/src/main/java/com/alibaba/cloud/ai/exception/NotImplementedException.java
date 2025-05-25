package com.alibaba.cloud.ai.exception;

import com.alibaba.cloud.ai.common.ReturnCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotImplementedException extends RuntimeException {

	private int code;

	private String msg;

	public NotImplementedException() {
		super();
		this.code = ReturnCode.RC501.getCode();
		this.msg = ReturnCode.RC501.getMsg();
	}

	public NotImplementedException(String msg) {
		super(msg);
		this.code = ReturnCode.RC501.getCode();
		this.msg = msg;
	}

}
