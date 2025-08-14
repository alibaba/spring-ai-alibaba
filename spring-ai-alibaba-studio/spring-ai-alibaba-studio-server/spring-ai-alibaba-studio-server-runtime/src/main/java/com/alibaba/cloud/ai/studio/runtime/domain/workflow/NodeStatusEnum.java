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
package com.alibaba.cloud.ai.studio.runtime.domain.workflow;

import lombok.Getter;

@Getter
public enum NodeStatusEnum {

	SUCCESS("success", "成功"), FAIL("fail", "失败"), SKIP("skip", "跳过"), EXECUTING("executing", "执行中"),
	PAUSE("pause", "暂停"), STOP("stop", "停止"),;

	NodeStatusEnum(String code, String desc) {
		this.code = code;
		this.desc = desc;
	}

	private final String code;

	private final String desc;

}
