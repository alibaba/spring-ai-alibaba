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

package com.alibaba.cloud.ai.service.code.memory;

import com.alibaba.cloud.ai.dto.schema.SchemaDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 存储SQL结果，供代码运行时读取
 *
 * @author vlsmb
 * @since 2025/7/28
 */
public class InMemorySqlResultMemoryService implements SqlResultMemoryService {

	private static final Logger log = LoggerFactory.getLogger(InMemorySqlResultMemoryService.class);

	private final ConcurrentHashMap<Key, Value> memory;

	private final AtomicInteger count;

	public InMemorySqlResultMemoryService() {
		this.memory = new ConcurrentHashMap<>();
		this.count = new AtomicInteger(0);
	}

	private String generateKey() {
		int count = this.count.getAndIncrement();
		return "sql_var_" + count;
	}

	@Override
	public Key saveSqlResult(Value value) {
		Key key = Key.createKey(generateKey());
		value = value.setNewKey(key);
		this.memory.put(key, value);
		return key;
	}

	@Override
	public SchemaDTO getSchemaByKey(Key key) {
		Value value = this.memory.get(key);
		return value != null ? value.schemaDTO() : null;
	}

	@Override
	public List<Map<String, Object>> getAllSqlResult(Key key) {
		Value value = this.memory.get(key);
		return value != null ? value.results() : null;
	}

}
