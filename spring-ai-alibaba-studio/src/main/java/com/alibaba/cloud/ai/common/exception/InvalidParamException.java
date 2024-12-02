package com.alibaba.cloud.ai.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidParamException extends RuntimeException {

	public InvalidParamException(String message) {
		super("invalid param: " + message);
	}

}
