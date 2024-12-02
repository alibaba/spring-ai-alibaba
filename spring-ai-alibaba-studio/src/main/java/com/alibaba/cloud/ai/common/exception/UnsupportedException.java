package com.alibaba.cloud.ai.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class UnsupportedException extends RuntimeException {

	public UnsupportedException(String message) {
		super("unsupported: " + message);
	}

}
