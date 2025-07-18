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

import com.alibaba.cloud.ai.example.manus.dynamic.prompt.model.po.PromptEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.prompt.model.vo.PromptVO;
import com.alibaba.cloud.ai.example.manus.dynamic.prompt.repository.PromptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.prompt.AssistantPromptTemplate;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PromptServiceImpl implements PromptService {

	private final PromptRepository promptRepository;

	@Value("${namespace.value}")
	private String namespace;

	private static final Logger log = LoggerFactory.getLogger(PromptDataInitializer.class);

	public PromptServiceImpl(PromptRepository promptRepository) {
		this.promptRepository = promptRepository;
	}

	@Override
	public List<PromptVO> getAll() {
		return promptRepository.findAll().stream().map(this::mapToPromptVO).collect(Collectors.toList());
	}

	@Override
	public List<PromptVO> getAllByNamespace(String namespace) {
		List<PromptEntity> entities;
		if ("default".equalsIgnoreCase(namespace)) {
			entities = promptRepository.findAll();
		}
		else {
			entities = promptRepository.getAllByNamespace(namespace);
		}
		return entities.stream().map(this::mapToPromptVO).collect(Collectors.toList());
	}

	@Override
	public PromptVO getById(Long id) {
		PromptEntity entity = promptRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("Prompt not found: " + id));
		return mapToPromptVO(entity);
	}

	@Override
	public PromptVO create(PromptVO promptVO) {
		if (promptVO.invalid()) {
			throw new IllegalArgumentException("PromptVO filed is invalid");
		}

		if (Boolean.TRUE.equals(promptVO.getBuiltIn())) {
			throw new IllegalArgumentException("Cannot create built-in prompt");
		}

		PromptEntity prompt = promptRepository.findByNamespaceAndPromptName(promptVO.getNamespace(),
				promptVO.getPromptName());
		if (prompt != null) {
			log.error("Found Prompt is existed: promptName :{} , namespace:{}, type :{}, String messageType:{}",
					promptVO.getPromptName(), promptVO.getNamespace(), promptVO.getType(), promptVO.getMessageType());
			throw new RuntimeException("Found Prompt is existed");
		}

		PromptEntity promptEntity = mapToPromptEntity(promptVO);
		PromptEntity save = promptRepository.save(promptEntity);
		log.info("Successfully created new Prompt promptName :{} , namespace:{}, type :{}, String messageType:{}",
				save.getPromptName(), save.getNamespace(), save.getType(), save.getMessageType());
		return mapToPromptVO(save);
	}

	@Override
	public PromptVO update(PromptVO promptVO) {
		if (promptVO.invalid()) {
			throw new IllegalArgumentException("PromptVO filed is invalid");
		}

		PromptEntity oldPrompt = promptRepository.findById(promptVO.getId())
			.orElseThrow(() -> new IllegalArgumentException("Prompt not found: " + promptVO.getId()));

		if (!oldPrompt.getPromptName().equals(promptVO.getPromptName())) {
			throw new IllegalArgumentException("Prompt name is not allowed to update");
		}

		PromptEntity promptEntity = mapToPromptEntity(promptVO);
		PromptEntity save = promptRepository.save(promptEntity);

		log.info("Successfully update new Prompt promptName :{} , namespace:{}, type :{}, String messageType:{}",
				save.getPromptName(), save.getNamespace(), save.getType(), save.getMessageType());
		return mapToPromptVO(save);
	}

	@Override
	public void delete(Long id) {
		PromptEntity entity = promptRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("Prompt not found: " + id));
		if (Boolean.TRUE.equals(entity.getBuiltIn())) {
			throw new IllegalArgumentException("Cannot delete built-in prompt");
		}

		promptRepository.deleteById(id);
	}

	/**
	 * Create system prompt template message
	 * @param promptName Prompt Name
	 * @param variables Variable mapping
	 * @return Prompt template message
	 */
	@Override
	public Message createSystemMessage(String promptName, Map<String, Object> variables) {
		PromptEntity promptEntity = promptRepository.findByNamespaceAndPromptName(namespace, promptName);
		if (promptEntity == null) {
			throw new IllegalArgumentException("Prompt not found: " + promptName);
		}

		SystemPromptTemplate template = new SystemPromptTemplate(promptEntity.getPromptContent());
		return template.createMessage(variables != null ? variables : Map.of());
	}

	/**
	 * Create user prompt template message
	 * @param promptName Prompt Name
	 * @param variables Variable mapping
	 * @return Prompt template message
	 */
	@Override
	public Message createUserMessage(String promptName, Map<String, Object> variables) {
		PromptEntity promptEntity = promptRepository.findByNamespaceAndPromptName(namespace, promptName);
		if (promptEntity == null) {
			throw new IllegalArgumentException("Prompt not found: " + promptName);
		}

		PromptTemplate template = new PromptTemplate(promptEntity.getPromptContent());
		return template.createMessage(variables != null ? variables : Map.of());
	}

	@Override
	public Message createMessage(String promptName, Map<String, Object> variables) {
		PromptEntity promptEntity = promptRepository.findByNamespaceAndPromptName(namespace, promptName);
		if (promptEntity == null) {
			throw new IllegalArgumentException("Prompt not found: " + promptName);
		}

		if (MessageType.USER.name().equals(promptEntity.getMessageType())) {
			PromptTemplate template = new PromptTemplate(promptEntity.getPromptContent());
			return template.createMessage(variables != null ? variables : Map.of());
		}
		else if (MessageType.SYSTEM.name().equals(promptEntity.getMessageType())) {
			SystemPromptTemplate template = new SystemPromptTemplate(promptEntity.getPromptContent());
			return template.createMessage(variables != null ? variables : Map.of());

		}
		else if (MessageType.ASSISTANT.name().equals(promptEntity.getMessageType())) {
			AssistantPromptTemplate template = new AssistantPromptTemplate(promptEntity.getPromptContent());
			return template.createMessage(variables != null ? variables : Map.of());
		}
		else {
			throw new IllegalArgumentException("Prompt message type not support : " + promptEntity.getMessageType());
		}

	}

	/**
	 * Render prompt template
	 * @param promptName Prompt Name
	 * @param variables Variable mapping
	 * @return Rendered prompt
	 */
	@Override
	public String renderPrompt(String promptName, Map<String, Object> variables) {
		PromptEntity promptEntity = promptRepository.findByNamespaceAndPromptName(namespace, promptName);
		if (promptEntity == null) {
			throw new IllegalArgumentException("Prompt not found: " + promptName);
		}

		log.info(promptName + " prompt content: {}", promptEntity.getPromptContent());
		PromptTemplate template = new PromptTemplate(promptEntity.getPromptContent());
		return template.render(variables != null ? variables : Map.of());
	}

	private PromptVO mapToPromptVO(PromptEntity entity) {
		PromptVO promptVO = new PromptVO();
		BeanUtils.copyProperties(entity, promptVO);
		return promptVO;
	}

	private PromptEntity mapToPromptEntity(PromptVO promptVO) {
		PromptEntity entity = new PromptEntity();
		BeanUtils.copyProperties(promptVO, entity);
		return entity;
	}

}
