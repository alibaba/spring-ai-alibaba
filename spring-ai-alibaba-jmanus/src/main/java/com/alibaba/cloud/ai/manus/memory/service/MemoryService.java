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

import java.util.List;

import com.alibaba.cloud.ai.manus.memory.entity.MemoryEntity;

/**
 * @author dahua
 * @time 2025/8/5
 * @desc memory service interface
 */
public interface MemoryService {

	List<MemoryEntity> getMemories();

	void deleteMemory(String id);

	MemoryEntity saveMemory(MemoryEntity memoryEntity);

	MemoryEntity updateMemory(MemoryEntity memoryEntity);

	MemoryEntity singleMemory(String memoryId);

}
