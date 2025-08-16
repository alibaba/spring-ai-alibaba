/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.studio.runtime.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import com.alibaba.cloud.ai.studio.runtime.domain.Error;

/**
 * Custom exception class for handling business logic errors in the application.
 *
 * @since 1.0.0.3
 */

@EqualsAndHashCode(callSuper = true)
@Data
public class BizException extends RuntimeException implements Serializable {

	/** Contains error details including message and additional error information */
	private Error error;

	/** Default constructor */
	public BizException() {
		super();
	}

	/** Constructor with error details */
	public BizException(Error error) {
		super(error.getMessage());
		this.error = error;
	}

	/** Constructor with error details and cause */
	public BizException(Error error, Throwable cause) {
		super(error.getMessage(), cause);
		this.error = error;
	}

	public BizException(Error error, String message) {
		super(message);
		this.error = error;
		this.error.setMessage(message);
	}

	/** Constructor with error details, cause, and stack trace options */
	public BizException(Error error, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(error.getMessage(), cause, enableSuppression, writableStackTrace);
		this.error = error;
	}

}
