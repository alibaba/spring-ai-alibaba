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

import java.io.IOException;
import java.util.List;

/**
 * Interface for loading dot prompt templates from a source.
 */
public interface DotPromptLoader {

	/**
	 * Get a list of all available prompt names.
	 * @return List of prompt names
	 * @throws IOException if there's an error accessing the prompts
	 */
	List<String> getPromptNames() throws IOException;

	/**
	 * Load a prompt by its name.
	 * @param promptName the name of the prompt to load
	 * @return the loaded DotPrompt
	 * @throws IOException if there's an error loading the prompt
	 */
	DotPrompt load(String promptName) throws IOException;

}
