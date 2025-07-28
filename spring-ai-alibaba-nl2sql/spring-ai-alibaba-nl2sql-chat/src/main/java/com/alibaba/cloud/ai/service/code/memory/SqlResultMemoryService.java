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

import java.util.List;
import java.util.Map;

/**
 * 存储SQL结果，供代码运行时读取
 *
 * @author vlsmb
 * @since 2025/7/28
 */
public interface SqlResultMemoryService {

	record Key(String key) {
		public static Key createKey(String key) {
			return new Key(key);
		}
	}

	record Value(Key key, SchemaDTO schemaDTO, List<Map<String, Object>> results) {
		public Value setNewKey(Key key) {
			return new Value(key, this.schemaDTO(), this.results());
		}

		public static Value createValue(SchemaDTO schemaDTO, List<Map<String, Object>> results) {
			return new Value(null, schemaDTO, results);
		}
	}

	/**
	 * 存储SQL运行结果
	 * @param value SQL运行结果，key字段应为null
	 * @return key，如果失败则返回null
	 */
	Key saveSqlResult(Value value);

	/**
	 * 根据key获取SchemaDTO对象
	 * @param key key
	 * @return SchemaDTO，失败返回null
	 */
	SchemaDTO getSchemaByKey(Key key);

	/**
	 * 获取运行结果的所有值（传递给代码）
	 * @param key key
	 * @return 存储的SQL结果，失败返回null
	 */
	List<Map<String, Object>> getAllSqlResult(Key key);

	/**
	 * 获取前几个运行结果（传递给AI模型，作为输入示例）
	 * @param key key
	 * @param limit 数量
	 * @return 存储的SQL结果，失败返回null
	 */
	default List<Map<String, Object>> getSqlResult(Key key, Long limit) {
		List<Map<String, Object>> result = this.getAllSqlResult(key);
		if (result == null) {
			return null;
		}
		return result.stream().limit(limit).toList();
	}

	/**
	 * 获取前5个运行结果（传递给AI模型，作为输入示例）
	 * @param key key
	 * @return 存储的SQL结果，失败返回null
	 */
	default List<Map<String, Object>> getSqlResult(Key key) {
		return this.getSqlResult(key, 5L);
	}

}
