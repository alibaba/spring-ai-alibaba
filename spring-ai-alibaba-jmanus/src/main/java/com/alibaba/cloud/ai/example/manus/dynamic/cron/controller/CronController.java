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
package com.alibaba.cloud.ai.example.manus.dynamic.cron.controller;

import com.alibaba.cloud.ai.example.manus.dynamic.cron.scheduler.DynamicCronTaskLoader;
import com.alibaba.cloud.ai.example.manus.dynamic.cron.service.CronService;
import com.alibaba.cloud.ai.example.manus.dynamic.cron.vo.CronConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cron-tasks")
@CrossOrigin(origins = "*") // Add cross-origin support
public class CronController {

	@Autowired
	private CronService cronService;

	@Autowired
	private DynamicCronTaskLoader taskManager;

	@GetMapping
	public ResponseEntity<List<CronConfig>> getAllCronTasks() {
		return ResponseEntity.ok(cronService.getAllCronTasks());
	}

	@GetMapping("/{id}")
	public ResponseEntity<CronConfig> getCronTaskById(@PathVariable("id") String id) {
		return ResponseEntity.ok(cronService.getCronTaskById(id));
	}

	@PostMapping
	public ResponseEntity<CronConfig> createCronTask(@RequestBody CronConfig cronConfig) {
		return ResponseEntity.ok(cronService.createCronTask(cronConfig));
	}

	@PutMapping("/{id}")
	public ResponseEntity<CronConfig> updateCronTask(@PathVariable("id") Long id, @RequestBody CronConfig cronConfig) {
		cronConfig.setId(id);
		return ResponseEntity.ok(cronService.updateCronTask(cronConfig));
	}

	@PutMapping("/{id}/status")
	public ResponseEntity<Void> updateTaskStatus(@PathVariable("id") String id,
			@RequestParam("status") Integer status) {
		cronService.updateTaskStatus(id, status);
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteCronTask(@PathVariable("id") String id) {
		try {
			cronService.deleteCronTask(id);
			return ResponseEntity.ok().build();
		}
		catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		}
	}

}
