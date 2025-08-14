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

package com.alibaba.cloud.ai.studio.runtime.utils;

import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.Error;

/**
 * Utility class for handling exceptions and converting them to standardized error
 * responses.
 *
 * @since 1.0.0.3
 */
public class ExceptionUtils {

	/**
	 * Converts a Throwable to a standardized Error object. If the throwable is a
	 * BizException, returns its error; otherwise returns a system error.
	 * @param err The throwable to convert
	 * @return Standardized Error object
	 */
	public static Error convertError(Throwable err) {
		Error error;
		if (err instanceof BizException) {
			error = ((BizException) err).getError();
		}
		else {
			error = ErrorCode.SYSTEM_ERROR.toError();
		}

		return error;
	}

}
