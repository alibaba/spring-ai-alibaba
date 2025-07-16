/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus.dynamic.prompt.service;

import com.alibaba.cloud.ai.example.manus.dynamic.prompt.model.vo.PromptVO;
import org.springframework.ai.chat.messages.Message;

import java.util.List;
import java.util.Map;

public interface PromptService {

	List<PromptVO> getAll();

	List<PromptVO> getAllByNamespace(String namespace);

	PromptVO getById(Long id);

	PromptVO create(PromptVO promptVO);

	PromptVO update(PromptVO promptVO);

	void delete(Long id);

	Message createSystemMessage(String promptName, Map<String, Object> variables);

	Message createUserMessage(String promptName, Map<String, Object> variables);

	Message createMessage(String promptName, Map<String, Object> variables);

	String renderPrompt(String promptName, Map<String, Object> variables);

}
