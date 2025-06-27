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

package com.alibaba.cloud.ai.dashscope.metadata;

import org.springframework.ai.image.ImageGenerationMetadata;

import java.util.Map;
import java.util.Objects;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

public class DashScopeImageGenMetadata implements ImageGenerationMetadata {

	private final String revisedPrompt;

	private final Map<String, Object> taskMetadata;

	public DashScopeImageGenMetadata(String revisedPrompt, Map<String, Object> metadata) {
		this.revisedPrompt = revisedPrompt;
		this.taskMetadata = metadata;
	}

	public String getRevisedPrompt() {
		return this.revisedPrompt;
	}

	public Map<String, Object> getTaskMetadata() {
		return this.taskMetadata;
	}

	public String toString() {
		return "DashScopeImageGenMetadata {" + "revisedPrompt='" + this.taskMetadata + this.revisedPrompt + '\'' + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof DashScopeImageGenMetadata that)) {
			return false;
		}
		return Objects.equals(this.revisedPrompt, that.revisedPrompt)
				&& Objects.equals(this.taskMetadata, that.taskMetadata);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.revisedPrompt, this.taskMetadata);
	}

}
