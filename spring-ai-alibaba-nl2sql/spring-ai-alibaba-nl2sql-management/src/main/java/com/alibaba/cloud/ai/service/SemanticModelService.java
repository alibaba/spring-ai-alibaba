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
package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.entity.SemanticModel;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 语义模型配置服务
 */
@Service
public class SemanticModelService {

	private final Map<Long, SemanticModel> modelStore = new ConcurrentHashMap<>();

	private final AtomicLong idGenerator = new AtomicLong(1);

	public SemanticModelService() {
		// 初始化示例数据
		initSampleData();
	}

	private void initSampleData() {
		save(new SemanticModel("dataset_001", "user_age", "用户年龄", "年龄,岁数", "用户的实际年龄", true, true, "INTEGER", "用户年龄字段"));
		save(new SemanticModel("dataset_001", "product_name", "商品名称", "产品名,商品", "商品的名称信息", false, true, "VARCHAR",
				"商品名称字段"));
		save(new SemanticModel("dataset_002", "order_amount", "订单金额", "金额,价格,费用", "订单的总金额", true, true, "DECIMAL",
				"订单金额字段"));
	}

	public List<SemanticModel> findAll() {
		return new ArrayList<>(modelStore.values());
	}

	public List<SemanticModel> findByDatasetId(String datasetId) {
		return modelStore.values()
			.stream()
			.filter(m -> Objects.equals(m.getDatasetId(), datasetId))
			.collect(Collectors.toList());
	}

	public SemanticModel findById(Long id) {
		return modelStore.get(id);
	}

	public SemanticModel save(SemanticModel model) {
		if (model.getId() == null) {
			model.setId(idGenerator.getAndIncrement());
			model.setCreateTime(LocalDateTime.now());
		}
		model.setUpdateTime(LocalDateTime.now());
		modelStore.put(model.getId(), model);
		return model;
	}

	public void deleteById(Long id) {
		modelStore.remove(id);
	}

	public List<SemanticModel> search(String keyword) {
		if (keyword == null || keyword.trim().isEmpty()) {
			return findAll();
		}

		String lowerKeyword = keyword.toLowerCase();
		return modelStore.values()
			.stream()
			.filter(m -> m.getOriginalFieldName().toLowerCase().contains(lowerKeyword)
					|| (m.getAgentFieldName() != null && m.getAgentFieldName().toLowerCase().contains(lowerKeyword))
					|| (m.getFieldSynonyms() != null && m.getFieldSynonyms().toLowerCase().contains(lowerKeyword)))
			.collect(Collectors.toList());
	}

	public void batchUpdateEnabled(String datasetId, boolean enabled) {
		modelStore.values().stream().filter(m -> Objects.equals(m.getDatasetId(), datasetId)).forEach(m -> {
			m.setEnabled(enabled);
			m.setUpdateTime(LocalDateTime.now());
		});
	}

}
