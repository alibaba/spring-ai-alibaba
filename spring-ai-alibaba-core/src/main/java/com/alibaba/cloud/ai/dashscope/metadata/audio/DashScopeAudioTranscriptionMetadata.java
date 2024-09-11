/*
 * Copyright 2023-2024 the original author or authors.
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

package com.alibaba.cloud.ai.dashscope.metadata.audio;

import org.springframework.ai.model.ResultMetadata;

/**
 * @author xYLiu
 * @author yuluo
 * @since 2023.0.1.0
 */

public interface DashScopeAudioTranscriptionMetadata extends ResultMetadata {

	/**
	 * A constant instance of {@link DashScopeAudioTranscriptionMetadata} that represents
	 * a null or empty metadata.
	 */
	DashScopeAudioTranscriptionMetadata NULL = DashScopeAudioTranscriptionMetadata.create();

	/**
	 * Factory method for creating a new instance of
	 * {@link DashScopeAudioTranscriptionMetadata}.
	 * @return a new instance of {@link DashScopeAudioTranscriptionMetadata}
	 */
	static DashScopeAudioTranscriptionMetadata create() {
		return new DashScopeAudioTranscriptionMetadata() {
		};
	}

}
