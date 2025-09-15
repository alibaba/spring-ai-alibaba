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
 * Semantic Model Configuration Service
 */
@Service
public class SemanticModelService {

	private final Map<Long, SemanticModel> modelStore = new ConcurrentHashMap<>();

	private final AtomicLong idGenerator = new AtomicLong(1);

	public SemanticModelService() {
		// Initialize sample data
		initSampleData();
	}

	private void initSampleData() {
		// Create sample data for agent 1
		SemanticModel model1 = new SemanticModel();
		model1.setAgentId(1L);
		model1.setOriginalFieldName("user_age");
		model1.setAgentFieldName("用户年龄");
		model1.setFieldSynonyms("年龄,岁数");
		model1.setFieldDescription("用户的实际年龄");
		model1.setDefaultRecall(true);
		model1.setEnabled(true);
		model1.setFieldType("INTEGER");
		model1.setOriginalDescription("用户年龄字段");
		save(model1);

		SemanticModel model2 = new SemanticModel();
		model2.setAgentId(1L);
		model2.setOriginalFieldName("product_name");
		model2.setAgentFieldName("商品名称");
		model2.setFieldSynonyms("产品名,商品");
		model2.setFieldDescription("商品的名称信息");
		model2.setDefaultRecall(false);
		model2.setEnabled(true);
		model2.setFieldType("VARCHAR");
		model2.setOriginalDescription("商品名称字段");
		save(model2);

		// Create sample data for agent 2
		SemanticModel model3 = new SemanticModel();
		model3.setAgentId(2L);
		model3.setOriginalFieldName("order_amount");
		model3.setAgentFieldName("订单金额");
		model3.setFieldSynonyms("金额,价格,费用");
		model3.setFieldDescription("订单的总金额");
		model3.setDefaultRecall(true);
		model3.setEnabled(true);
		model3.setFieldType("DECIMAL");
		model3.setOriginalDescription("订单金额字段");
		save(model3);
	}

	public List<SemanticModel> findAll() {
		return new ArrayList<>(modelStore.values());
	}

	public List<SemanticModel> findByAgentId(Long agentId) {
		return modelStore.values()
			.stream()
			.filter(m -> Objects.equals(m.getAgentId(), agentId))
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

}
