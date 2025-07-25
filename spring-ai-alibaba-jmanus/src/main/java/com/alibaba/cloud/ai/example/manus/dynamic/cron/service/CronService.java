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
package com.alibaba.cloud.ai.example.manus.dynamic.cron.service;

import com.alibaba.cloud.ai.example.manus.dynamic.cron.vo.CronConfig;

import java.util.List;

public interface CronService {

	List<CronConfig> getAllCronTasks();

	CronConfig getCronTaskById(String id);

	CronConfig createCronTask(CronConfig cronConfig);

	CronConfig updateCronTask(CronConfig cronConfig);

	void updateTaskStatus(String id, Integer status);

	void deleteCronTask(String id);

}
