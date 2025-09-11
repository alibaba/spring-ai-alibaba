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
package com.alibaba.cloud.ai.studio.admin.generator.service.generator.agent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author yHong
 * @version 1.0
 * @since 2025/8/28 17:54
 */
public class RenderContext {

	private final AtomicInteger seq = new AtomicInteger(0);

	public String nextVar(String base) {
		return base + seq.incrementAndGet();
	}

}
