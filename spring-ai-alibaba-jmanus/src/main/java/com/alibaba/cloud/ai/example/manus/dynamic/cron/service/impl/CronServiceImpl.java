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
package com.alibaba.cloud.ai.example.manus.dynamic.cron.service.impl;

import com.alibaba.cloud.ai.example.manus.dynamic.cron.entity.CronEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.cron.repository.CronRepository;
import com.alibaba.cloud.ai.example.manus.dynamic.cron.scheduler.DynamicCronTaskScheduler;
import com.alibaba.cloud.ai.example.manus.dynamic.cron.service.CronService;
import com.alibaba.cloud.ai.example.manus.dynamic.cron.vo.CronConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CronServiceImpl implements CronService {

	private static final Logger log = LoggerFactory.getLogger(CronServiceImpl.class);

	private final CronRepository repository;

	private final DynamicCronTaskScheduler taskScheduler;

	@Autowired
	public CronServiceImpl(CronRepository repository, DynamicCronTaskScheduler taskScheduler) {
		this.repository = repository;
		this.taskScheduler = taskScheduler;
	}

	@Override
	public List<CronConfig> getAllCronTasks() {
		return repository.findAll().stream().map(CronEntity::mapToCronConfig).collect(Collectors.toList());
	}

	@Override
	public CronConfig getCronTaskById(String id) {
		CronEntity entity = repository.findById(Long.parseLong(id))
			.orElseThrow(() -> new IllegalArgumentException("Cron task not found: " + id));
		return entity.mapToCronConfig();
	}

	@Override
	public CronConfig createCronTask(CronConfig config) {
		try {
			// 校验cron表达式
			validateCronExpression(config.getCronTime());

			CronEntity entity = new CronEntity();
			convertEntityFromConfig(entity, config);
			entity.setCreateTime(LocalDateTime.now());
			entity = repository.save(entity);
			log.info("创建定时任务: {}", config.getPlanDesc());
			return entity.mapToCronConfig();
		}
		catch (Exception e) {
			log.error("创建定时任务失败: {} - {}", config.getPlanDesc(), e.getMessage());
			throw e;
		}
	}

	@Override
	public CronConfig updateCronTask(CronConfig config) {
		// 校验cron表达式
		validateCronExpression(config.getCronTime());

		CronEntity entity = repository.findById(config.getId())
			.orElseThrow(() -> new IllegalArgumentException("Cron task not found: " + config.getId()));
		convertEntityFromConfig(entity, config);
		entity = repository.save(entity);

		// 移除旧任务，等待调度器重新加载
		taskScheduler.removeTask(entity.getId());
		return entity.mapToCronConfig();
	}

	@Override
	public void updateTaskStatus(String id, Integer status) {
		CronEntity entity = repository.findById(Long.parseLong(id))
			.orElseThrow(() -> new IllegalArgumentException("Cron task not found: " + id));
		entity.setStatus(status);
		repository.save(entity);
	}

	@Override
	public void executeCronTask(String id) {
		taskScheduler.executeTaskById(Long.parseLong(id));
	}

	@Override
	public void deleteCronTask(String id) {
		repository.deleteById(Long.parseLong(id));
	}

	private void convertEntityFromConfig(CronEntity entity, CronConfig config) {
		if (config.getCronName() != null) {
			entity.setCronName(config.getCronName());
		}
		if (config.getCronTime() != null) {
			entity.setCronTime(config.getCronTime());
		}
		if (config.getPlanDesc() != null) {
			entity.setPlanDesc(config.getPlanDesc());
		}
		if (config.getStatus() != null) {
			entity.setStatus(config.getStatus());
		}
		// 无论planTemplateId是否为空都进行更新
		entity.setPlanTemplateId(config.getPlanTemplateId());
	}

	private void validateCronExpression(String cronExpression) {
		if (cronExpression == null || cronExpression.trim().isEmpty()) {
			throw new IllegalArgumentException("Cron expression cannot be null or empty");
		}

		try {
			CronExpression.parse(cronExpression);
		}
		catch (IllegalArgumentException e) {
			log.error("无效的cron表达式: {}", cronExpression);
			throw new IllegalArgumentException("Invalid cron expression: " + cronExpression + ". " + e.getMessage());
		}
	}

}
