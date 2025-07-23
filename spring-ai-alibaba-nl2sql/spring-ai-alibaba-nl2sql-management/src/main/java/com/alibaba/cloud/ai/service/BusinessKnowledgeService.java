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

import com.alibaba.cloud.ai.entity.BusinessKnowledge;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 业务知识管理服务
 */
@Service
public class BusinessKnowledgeService {

	private final Map<Long, BusinessKnowledge> knowledgeStore = new ConcurrentHashMap<>();

	private final AtomicLong idGenerator = new AtomicLong(1);

	public BusinessKnowledgeService() {
		// 初始化示例数据
		initSampleData();
	}

	private void initSampleData() {
		save(new BusinessKnowledge("年龄分布", "分别计算劳动人口占比，少年儿童占比，老年人口占比三个字段指标的平均值", "年龄画像,年龄构成,年龄结构", true,
				"dataset_001"));
		save(new BusinessKnowledge("搜索业绩口径", "定义：订单/流量计入搜索", "搜索业绩,搜索口径", false, "dataset_001"));
		save(new BusinessKnowledge("GMV", "商品交易总额，包含付款和未付款的订单金额", "交易总额,成交总额", true, "dataset_002"));
	}

	public List<BusinessKnowledge> findAll() {
		return new ArrayList<>(knowledgeStore.values());
	}

	public List<BusinessKnowledge> findByDatasetId(String datasetId) {
		return knowledgeStore.values()
			.stream()
			.filter(k -> Objects.equals(k.getDatasetId(), datasetId))
			.collect(Collectors.toList());
	}

	public BusinessKnowledge findById(Long id) {
		return knowledgeStore.get(id);
	}

	public BusinessKnowledge save(BusinessKnowledge knowledge) {
		if (knowledge.getId() == null) {
			knowledge.setId(idGenerator.getAndIncrement());
			knowledge.setCreateTime(LocalDateTime.now());
		}
		knowledge.setUpdateTime(LocalDateTime.now());
		knowledgeStore.put(knowledge.getId(), knowledge);
		return knowledge;
	}

	public void deleteById(Long id) {
		knowledgeStore.remove(id);
	}

	public List<BusinessKnowledge> search(String keyword) {
		if (keyword == null || keyword.trim().isEmpty()) {
			return findAll();
		}

		String lowerKeyword = keyword.toLowerCase();
		return knowledgeStore.values()
			.stream()
			.filter(k -> k.getBusinessTerm().toLowerCase().contains(lowerKeyword)
					|| k.getDescription().toLowerCase().contains(lowerKeyword)
					|| (k.getSynonyms() != null && k.getSynonyms().toLowerCase().contains(lowerKeyword)))
			.collect(Collectors.toList());
	}

}
