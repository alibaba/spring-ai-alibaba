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
 * Service interface for managing DotPrompt templates.
 */
public interface DotPromptService {

	/**
	 * Get a list of all available DotPrompt names.
	 * @return List of DotPrompt names
	 * @throws IOException if there's an error accessing the prompt files
	 */
	List<String> getPromptNames() throws IOException;

	/**
	 * Get the DotPrompt details for a specific prompt name.
	 * @param promptName the name of the prompt to retrieve
	 * @return the DotPrompt object containing the prompt details
	 * @throws IOException if there's an error loading the prompt
	 */
	DotPrompt getPrompt(String promptName) throws IOException;

}
