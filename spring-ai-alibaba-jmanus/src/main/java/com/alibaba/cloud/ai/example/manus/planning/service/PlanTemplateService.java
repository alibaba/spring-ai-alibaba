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
package com.alibaba.cloud.ai.example.manus.planning.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.cloud.ai.example.manus.planning.model.po.PlanTemplate;
import com.alibaba.cloud.ai.example.manus.planning.model.po.PlanTemplateVersion;
import com.alibaba.cloud.ai.example.manus.planning.repository.PlanTemplateRepository;
import com.alibaba.cloud.ai.example.manus.planning.repository.PlanTemplateVersionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 计划模板服务类，提供计划模板相关的业务逻辑
 */
@Service
public class PlanTemplateService {

	private static final Logger logger = LoggerFactory.getLogger(PlanTemplateService.class);

	@Autowired
	private PlanTemplateRepository planTemplateRepository;

	@Autowired
	private PlanTemplateVersionRepository versionRepository;

	/**
	 * 保存计划模板及其第一个版本
	 * @param planTemplateId 计划模板ID
	 * @param title 计划标题
	 * @param userRequest 用户请求
	 * @param planJson 计划JSON数据
	 */
	@Transactional
	public void savePlanTemplate(String planTemplateId, String title, String userRequest, String planJson) {
		// 保存计划模板基本信息
		PlanTemplate template = new PlanTemplate(planTemplateId, title, userRequest);
		planTemplateRepository.save(template);

		// 保存第一个版本
		saveVersionToHistory(planTemplateId, planJson);

		logger.info("已保存计划模板 {} 及其第一个版本", planTemplateId);
	}

	/**
	 * 更新计划模板信息
	 * @param planTemplateId 计划模板ID
	 * @param title 计划标题
	 * @param planJson 计划JSON数据
	 * @return 是否更新成功
	 */
	@Transactional
	public boolean updatePlanTemplate(String planTemplateId, String title, String planJson) {
		Optional<PlanTemplate> templateOpt = planTemplateRepository.findByPlanTemplateId(planTemplateId);
		if (templateOpt.isPresent()) {
			PlanTemplate template = templateOpt.get();
			if (title != null && !title.isEmpty()) {
				template.setTitle(title);
			}
			template.setUpdateTime(LocalDateTime.now());
			planTemplateRepository.save(template);

			// 保存新版本
			saveVersionToHistory(planTemplateId, planJson);

			logger.info("已更新计划模板 {} 及保存新版本", planTemplateId);
			return true;
		}
		return false;
	}

	/**
	 * 保存计划版本到历史记录
	 * @param planTemplateId 计划模板ID
	 * @param planJson 计划JSON数据
	 */
	@Transactional
	public void saveVersionToHistory(String planTemplateId, String planJson) {
		// 获取最大版本号
		Integer maxVersionIndex = versionRepository.findMaxVersionIndexByPlanTemplateId(planTemplateId);
		int newVersionIndex = (maxVersionIndex == null) ? 0 : maxVersionIndex + 1;

		// 保存新版本
		PlanTemplateVersion version = new PlanTemplateVersion(planTemplateId, newVersionIndex, planJson);
		versionRepository.save(version);

		logger.info("已保存计划 {} 的版本 {}", planTemplateId, newVersionIndex);
	}

	/**
	 * 获取计划模板
	 * @param planTemplateId 计划模板ID
	 * @return 计划模板实体，如果不存在则返回null
	 */
	public PlanTemplate getPlanTemplate(String planTemplateId) {
		return planTemplateRepository.findByPlanTemplateId(planTemplateId).orElse(null);
	}

	/**
	 * 获取计划的所有版本JSON数据
	 * @param planTemplateId 计划模板ID
	 * @return 版本JSON数据列表
	 */
	public List<String> getPlanVersions(String planTemplateId) {
		List<PlanTemplateVersion> versions = versionRepository
			.findByPlanTemplateIdOrderByVersionIndexAsc(planTemplateId);
		List<String> jsonVersions = new ArrayList<>();
		for (PlanTemplateVersion version : versions) {
			jsonVersions.add(version.getPlanJson());
		}
		return jsonVersions;
	}

	/**
	 * 获取计划的指定版本
	 * @param planTemplateId 计划模板ID
	 * @param versionIndex 版本索引
	 * @return 版本JSON数据，如果版本不存在则返回null
	 */
	public String getPlanVersion(String planTemplateId, int versionIndex) {
		PlanTemplateVersion version = versionRepository.findByPlanTemplateIdAndVersionIndex(planTemplateId,
				versionIndex);
		return version != null ? version.getPlanJson() : null;
	}

	/**
	 * 获取计划的最新版本
	 * @param planTemplateId 计划模板ID
	 * @return 最新版本的JSON数据，如果没有版本则返回null
	 */
	public String getLatestPlanVersion(String planTemplateId) {
		Integer maxVersionIndex = versionRepository.findMaxVersionIndexByPlanTemplateId(planTemplateId);
		if (maxVersionIndex == null) {
			return null;
		}
		return getPlanVersion(planTemplateId, maxVersionIndex);
	}

	/**
	 * 从ExecutionPlan对象中提取标题
	 * @param planJson 计划JSON字符串
	 * @return 计划标题，如果无法提取则返回默认标题
	 */
	public String extractTitleFromPlan(String planJson) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode rootNode = mapper.readTree(planJson);
			if (rootNode.has("title")) {
				return rootNode.get("title").asText("未命名计划");
			}
		}
		catch (Exception e) {
			logger.warn("从计划JSON中提取标题失败", e);
		}
		return "未命名计划";
	}

	/**
	 * 获取所有计划模板
	 * @return 所有计划模板的列表
	 */
	public List<PlanTemplate> getAllPlanTemplates() {
		return planTemplateRepository.findAll();
	}

	/**
	 * 删除计划模板
	 * @param planTemplateId 计划模板ID
	 * @return 是否删除成功
	 */
	@Transactional
	public boolean deletePlanTemplate(String planTemplateId) {
		try {
			// 先删除所有相关的版本
			versionRepository.deleteByPlanTemplateId(planTemplateId);

			// 再删除模板本身
			planTemplateRepository.deleteByPlanTemplateId(planTemplateId);

			logger.info("已删除计划模板 {} 及其所有版本", planTemplateId);
			return true;
		}
		catch (Exception e) {
			logger.error("删除计划模板 {} 失败", planTemplateId, e);
			return false;
		}
	}

}
