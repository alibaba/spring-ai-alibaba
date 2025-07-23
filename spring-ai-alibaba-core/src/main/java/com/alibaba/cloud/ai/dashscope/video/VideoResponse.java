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

package com.alibaba.cloud.ai.dashscope.video;

import com.alibaba.cloud.ai.dashscope.api.DashScopeVideoApi;
import org.springframework.ai.model.ModelResponse;
import org.springframework.ai.model.ResponseMetadata;

import java.util.Collections;
import java.util.List;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

public class VideoResponse implements ModelResponse<DashScopeVideoApi.VideoGenerationResponse> {

	private final DashScopeVideoApi.VideoGenerationResponse result;

	public VideoResponse(DashScopeVideoApi.VideoGenerationResponse result) {

		this.result = result;
	}

	@Override
	public DashScopeVideoApi.VideoGenerationResponse getResult() {

		return this.result;
	}

	@Override
	public List<DashScopeVideoApi.VideoGenerationResponse> getResults() {

		return Collections.singletonList(this.result);
	}

	@Override
	public ResponseMetadata getMetadata() {
		// todo: add metadata.
		return null;
	}

}
