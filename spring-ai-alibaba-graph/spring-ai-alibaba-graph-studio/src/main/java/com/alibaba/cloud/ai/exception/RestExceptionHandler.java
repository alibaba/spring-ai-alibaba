/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.exception;

import com.alibaba.cloud.ai.common.R;
import com.alibaba.cloud.ai.common.ReturnCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@ResponseBody
public class RestExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

	@ExceptionHandler(NotFoundException.class)
	public R<String> notFoundException(NotFoundException e) {
		log.error("NotFoundException ", e);
		return R.error(e.getCode(), e.getMsg());
	}

	@ExceptionHandler(SerializationException.class)
	public R<String> serializeException(SerializationException e) {
		log.error("SerializeException ", e);
		return R.error(e.getCode(), e.getMsg());
	}

	@ExceptionHandler(NullPointerException.class)
	public R<String> nullPointerException(NullPointerException e) {
		log.error("NullPointerException ", e);
		return R.error(ReturnCode.RC400.getCode(), ReturnCode.RC400.getMsg());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public R<String> illegalArgumentException(IllegalArgumentException e) {
		log.error("IllegalArgumentException ", e);
		return R.error(ReturnCode.RC400.getCode(), e.getMessage());
	}

	@ExceptionHandler(RuntimeException.class)
	public R<String> runtimeException(RuntimeException e) {
		log.error("RuntimeException ", e);
		return R.error(ReturnCode.RC500.getCode(), e.getMessage());
	}

	@ExceptionHandler(Exception.class)
	public R<String> exception(Exception e) {
		log.error("Unknown exception = {}", e.getMessage(), e);
		return R.error(ReturnCode.RC500.getCode(), ReturnCode.RC500.getMsg());
	}

}
