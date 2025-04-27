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
package com.alibaba.cloud.ai.example.manus.config.startUp;

import java.util.Scanner;

import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.planning.PlanningFactory;
import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanIdDispatcher;
import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanningCoordinator;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class QueryCommandRunner implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(QueryCommandRunner.class);

	@Autowired
	@Lazy
	private PlanningFactory planningFactory;

	@Autowired
	private PlanIdDispatcher planIdDispatcher;

	@Autowired
	private ManusProperties manusProperties;

	@Override
	public void run(String... args) throws Exception {
		// 只有当启用了控制台查询模式时才执行
		if (!manusProperties.getConsoleQuery()) {
			logger.info("控制台交互模式未启用，跳过命令行查询");
			return;
		}

		logger.info("启动控制台交互模式，请输入查询...");
		Scanner scanner = new Scanner(System.in);
		while (true) {
			System.out.println("Enter your query (or type 'exit' to quit): ");
			String query = scanner.nextLine();

			if ("exit".equalsIgnoreCase(query)) {
				System.out.println("Exiting...");
				break;
			}

			// 使用 PlanIdDispatcher 生成唯一的计划ID
			String planId = planIdDispatcher.generatePlanId();
			PlanningCoordinator planningCoordinator = planningFactory.createPlanningCoordinator(planId);
			ExecutionContext context = new ExecutionContext();
			context.setUserRequest(query);
			context.setPlanId(planId);
			try {
				var executionContext = planningCoordinator.executePlan(context);
				System.out.println("Plan " + planId + " executed successfully");
				System.out.println("Execution Context: " + executionContext.getResultSummary());
			}
			catch (Exception e) {
				logger.error("执行查询时发生错误", e);
				System.out.println("Error: " + e.getMessage());
			}
		}
		scanner.close();
	}

}
