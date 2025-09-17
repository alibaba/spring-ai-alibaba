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
package com.alibaba.cloud.ai.manus.memory.service;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.cloud.ai.manus.memory.entity.MemoryEntity;
import com.alibaba.cloud.ai.manus.memory.repository.MemoryRepository;

import java.util.List;

/**
 * @author dahua
 * @time 2025/8/5
 * @desc memory service impl
 */
@Service
@Transactional
public class MemoryServiceImpl implements MemoryService {

	@Autowired
	private MemoryRepository memoryRepository;

	@Autowired
	private ChatMemory chatMemory;

	@Override
	public List<MemoryEntity> getMemories() {
		List<MemoryEntity> memoryEntities = memoryRepository.findAll();
		memoryEntities.forEach(memoryEntity -> {
			List<Message> messages = chatMemory.get(memoryEntity.getMemoryId());
			memoryEntity.setMessages(messages);
		});
		memoryEntities.stream()
			.sorted((m1, m2) -> Math.toIntExact(m1.getCreateTime().getTime() - m2.getCreateTime().getTime()))
			.toList();
		return memoryEntities;
	}

	@Override
	public void deleteMemory(String memoryId) {
		chatMemory.clear(memoryId);
		memoryRepository.deleteByMemoryId(memoryId);
	}

	@Override
	public MemoryEntity saveMemory(MemoryEntity memoryEntity) {
		MemoryEntity findEntity = memoryRepository.findByMemoryId(memoryEntity.getMemoryId());
		if (findEntity != null) {
			findEntity.setMessages(null);
		}
		else {
			findEntity = memoryEntity;
		}
		MemoryEntity saveEntity = memoryRepository.save(findEntity);
		saveEntity.setMessages(chatMemory.get(saveEntity.getMemoryId()));
		return saveEntity;
	}

	@Override
	public MemoryEntity updateMemory(MemoryEntity memoryEntity) {
		MemoryEntity findEntity = memoryRepository.findByMemoryId(memoryEntity.getMemoryId());
		if (findEntity == null) {
			throw new IllegalArgumentException();
		}
		findEntity.setMemoryName(memoryEntity.getMemoryName());
		MemoryEntity saveEntity = memoryRepository.save(findEntity);
		saveEntity.setMessages(chatMemory.get(saveEntity.getMemoryId()));
		return saveEntity;
	}

	@Override
	public MemoryEntity singleMemory(String memoryId) {
		MemoryEntity findEntity = memoryRepository.findByMemoryId(memoryId);
		if (findEntity == null) {
			throw new IllegalArgumentException();
		}
		findEntity.setMessages(chatMemory.get(findEntity.getMemoryId()));
		return findEntity;
	}

}
