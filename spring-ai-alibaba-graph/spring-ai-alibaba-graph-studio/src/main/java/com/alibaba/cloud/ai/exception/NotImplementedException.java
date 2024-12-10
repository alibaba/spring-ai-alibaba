package com.alibaba.cloud.ai.exception;

import com.alibaba.cloud.ai.common.ReturnCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotImplementedException {

	private int code;

	private String msg;

	public NotImplementedException() {
		this.code = ReturnCode.RC501.getCode();
		this.msg = ReturnCode.RC501.getMsg();
	}

	public NotImplementedException(String msg) {
		this.code = ReturnCode.RC501.getCode();
		this.msg = msg;
	}

}
