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

package com.alibaba.cloud.ai.dotprompt;

import org.springframework.util.Assert;

import java.io.IOException;
import java.util.List;

/**
 * Default implementation of DotPromptService.
 */
public class DefaultDotPromptService implements DotPromptService {

	private final DotPromptLoader promptLoader;

	public DefaultDotPromptService(DotPromptLoader promptLoader) {
		Assert.notNull(promptLoader, "promptLoader must not be null");
		this.promptLoader = promptLoader;
	}

	@Override
	public List<String> getPromptNames() throws IOException {
		return promptLoader.getPromptNames();
	}

	@Override
	public DotPrompt getPrompt(String promptName) throws IOException {
		Assert.hasText(promptName, "promptName must not be empty");
		return promptLoader.load(promptName);
	}

}
