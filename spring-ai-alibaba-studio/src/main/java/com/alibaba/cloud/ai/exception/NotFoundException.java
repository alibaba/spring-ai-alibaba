package com.alibaba.cloud.ai.exception;

import com.feiniaojin.gracefulresponse.api.ExceptionMapper;

@ExceptionMapper(code = "404", msg = "not found")
public class NotFoundException extends RuntimeException {

}
