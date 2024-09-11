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

package com.alibaba.cloud.ai.dashscope.audio.transcription;

import java.util.List;

import com.alibaba.cloud.ai.dashscope.metadata.audio.DashScopeAudioTranscriptionResponseMetadata;

import org.springframework.ai.model.ModelResponse;
import org.springframework.ai.model.ResponseMetadata;

/**
 * @author xYLiu
 * @author yuluo
 * @since 2023.0.1.0
 */

public class AudioTranscriptionResponse implements ModelResponse<AudioTranscriptionResult> {

	private List<AudioTranscriptionResult> resultList;

	private DashScopeAudioTranscriptionResponseMetadata transcriptionResponseMetadata;

	public AudioTranscriptionResponse(List<AudioTranscriptionResult> result) {

		this(result, DashScopeAudioTranscriptionResponseMetadata.NULL);
	}

	public AudioTranscriptionResponse(List<AudioTranscriptionResult> result,
			DashScopeAudioTranscriptionResponseMetadata transcriptionResponseMetadata) {

		this.resultList = List.copyOf(result);
		this.transcriptionResponseMetadata = transcriptionResponseMetadata;
	}

	@Override
	public AudioTranscriptionResult getResult() {

		return this.resultList.get(0);
	}

	@Override
	public List<AudioTranscriptionResult> getResults() {

		return this.resultList;
	}

	@Override
	public ResponseMetadata getMetadata() {

		return this.transcriptionResponseMetadata;
	}

}
