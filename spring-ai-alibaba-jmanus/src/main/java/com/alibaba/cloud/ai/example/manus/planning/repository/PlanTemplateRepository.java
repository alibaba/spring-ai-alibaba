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
package com.alibaba.cloud.ai.example.manus.planning.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.alibaba.cloud.ai.example.manus.planning.model.po.PlanTemplate;

/**
 * 计划模板数据访问接口
 */
@Repository
public interface PlanTemplateRepository extends JpaRepository<PlanTemplate, String> {

	/**
	 * 根据计划模板ID查找计划模板
	 * @param planTemplateId 计划模板ID
	 * @return 计划模板实体
	 */
	Optional<PlanTemplate> findByPlanTemplateId(String planTemplateId);

	/**
	 * 根据计划模板ID删除计划模板
	 * @param planTemplateId 计划模板ID
	 */
	void deleteByPlanTemplateId(String planTemplateId);

}
