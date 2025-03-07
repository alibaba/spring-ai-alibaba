/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
