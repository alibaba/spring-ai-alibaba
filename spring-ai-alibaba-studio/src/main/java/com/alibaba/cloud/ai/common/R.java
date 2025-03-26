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
package com.alibaba.cloud.ai.common;

import lombok.Data;

@Data
public class R<T> {

	private Integer code;

	private String msg;

	private T data;

	private long timestamp;

	private String requestId;

	public R() {
		this.timestamp = System.currentTimeMillis();
	}

	public static <T> R<T> success(T data) {
		R<T> r = new R<>();
		r.setCode(ReturnCode.RC200.getCode());
		r.setMsg(ReturnCode.RC200.getMsg());
		r.setData(data);
		return r;
	}

	public static <T> R<T> error(int code, String msg) {
		R<T> r = new R<>();
		r.setCode(code);
		r.setMsg(msg);
		r.setData(null);
		return r;
	}

}
