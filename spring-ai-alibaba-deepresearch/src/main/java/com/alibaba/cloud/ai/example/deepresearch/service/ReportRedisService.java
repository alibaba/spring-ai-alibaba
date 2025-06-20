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

package com.alibaba.cloud.ai.example.deepresearch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 报告 Redis 服务类
 *
 * @author huangzhen
 * @since 2025/6/18
 */
@Service
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = false)
public class ReportRedisService implements ReportService {

	private static final Logger logger = LoggerFactory.getLogger(ReportRedisService.class);

	private final RedisTemplate<String, Object> redisTemplate;

	/**
	 * Redis key 前缀
	 */
	private static final String REPORT_KEY_PREFIX = "deepresearch:report:";

	/**
	 * 默认过期时间（24小时）
	 */
	private static final long DEFAULT_EXPIRE_HOURS = 24;

	public ReportRedisService(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	/**
	 * 存储报告到 Redis
	 * @param threadId 线程ID
	 * @param report 报告内容
	 */
	@Override
	public void saveReport(String threadId, String report) {
		try {
			String key = buildKey(threadId);
			redisTemplate.opsForValue().set(key, report, DEFAULT_EXPIRE_HOURS, TimeUnit.HOURS);
			logger.info("报告已保存到 Redis，线程ID: {}, key: {}", threadId, key);
		}
		catch (Exception e) {
			logger.error("保存报告到 Redis 失败，线程ID: {}", threadId, e);
			throw new RuntimeException("保存报告失败", e);
		}
	}

	/**
	 * 从 Redis 获取报告
	 * @param threadId 线程ID
	 * @return 报告内容，如果不存在返回 null
	 */
	@Override
	public String getReport(String threadId) {
		try {
			String key = buildKey(threadId);
			Object result = redisTemplate.opsForValue().get(key);
			if (result != null) {
				logger.info("从 Redis 获取报告成功，线程ID: {}, key: {}", threadId, key);
				return result.toString();
			}
			else {
				logger.warn("Redis 中未找到报告，线程ID: {}, key: {}", threadId, key);
				return null;
			}
		}
		catch (Exception e) {
			logger.error("从 Redis 获取报告失败，线程ID: {}", threadId, e);
			throw new RuntimeException("获取报告失败", e);
		}
	}

	/**
	 * 检查报告是否存在
	 * @param threadId 线程ID
	 * @return 是否存在
	 */
	@Override
	public boolean existsReport(String threadId) {
		try {
			String key = buildKey(threadId);
			Boolean exists = redisTemplate.hasKey(key);
			return exists != null && exists;
		}
		catch (Exception e) {
			logger.error("检查报告是否存在失败，线程ID: {}", threadId, e);
			return false;
		}
	}

	/**
	 * 删除报告
	 * @param threadId 线程ID
	 */
	@Override
	public void deleteReport(String threadId) {
		try {
			String key = buildKey(threadId);
			redisTemplate.delete(key);
			logger.info("已删除 Redis 中的报告，线程ID: {}, key: {}", threadId, key);
		}
		catch (Exception e) {
			logger.error("删除报告失败，线程ID: {}", threadId, e);
			throw new RuntimeException("删除报告失败", e);
		}
	}

	/**
	 * 构建 Redis key
	 * @param threadId 线程ID
	 * @return Redis key
	 */
	private String buildKey(String threadId) {
		return REPORT_KEY_PREFIX + threadId;
	}

}
