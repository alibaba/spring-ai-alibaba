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

import org.springframework.ai.model.ModelRequest;

import java.util.Collections;
import java.util.List;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

public class VideoPrompt implements ModelRequest<List<VideoMessage>> {

	private final List<VideoMessage> messages;

	private final VideoOptions options;

	public VideoPrompt(String contents) {

		this(new VideoMessage(contents));
	}

	public VideoPrompt(VideoOptions options) {
		this("", options);
	}

	public VideoPrompt(VideoMessage message) {

		this(Collections.singletonList(message));
	}

	public VideoPrompt(List<VideoMessage> messages) {

		this(messages, null);
	}

	public VideoPrompt(String contents, VideoOptions options) {

		this(new VideoMessage(contents), options);
	}

	public VideoPrompt(VideoMessage message, VideoOptions options) {

		this(Collections.singletonList(message), options);
	}

	public VideoPrompt(List<VideoMessage> messages, VideoOptions options) {

		this.messages = messages;
		this.options = options;
	}

	@Override
	public List<VideoMessage> getInstructions() {

		return this.messages;
	}

	@Override
	public VideoOptions getOptions() {

		return this.options;
	}

}
