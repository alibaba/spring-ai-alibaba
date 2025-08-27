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

package com.alibaba.cloud.ai.service.code;

import com.alibaba.cloud.ai.config.CodeExecutorProperties;
import com.alibaba.cloud.ai.service.code.impl.LocalCodePoolExecutorService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StringUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@SpringBootTest(classes = { CodeExecutorProperties.class })
@DisplayName("Run Python Code in Local Command Test")
public class LocalCodePoolExecutorServiceTest {

	private static final Logger logger = LoggerFactory.getLogger(LocalCodePoolExecutorServiceTest.class);

	@Autowired
	private CodeExecutorProperties properties;

	private CodePoolExecutorService codePoolExecutorService = null;

	@BeforeEach
	public void init() {
		this.properties.setCodeTimeout("5s");
		this.properties.setCodePoolExecutor(CodePoolExecutorEnum.LOCAL);
		this.codePoolExecutorService = new LocalCodePoolExecutorService(properties);
	}

	private void testNormalCode() {
		logger.info("Run Normal Code");
		CodePoolExecutorService.TaskResponse response = codePoolExecutorService
			.runTask(new CodePoolExecutorService.TaskRequest(CodeTestConstant.NORMAL_CODE, "", null));
		System.out.println(response);
		logger.info("Run Normal Code Finished");
		if (!response.isSuccess() || !response.stdOut().contains("3628800")) {
			throw new RuntimeException("Test Failed");
		}
	}

	private void testTimeoutCode() {
		logger.info("Run Code with Endless Loop");
		CodePoolExecutorService.TaskResponse response = codePoolExecutorService
			.runTask(new CodePoolExecutorService.TaskRequest(CodeTestConstant.TIMEOUT_CODE, "", null));
		System.out.println(response);
		logger.info("Run Code with Endless Loop Finished");
		if (response.isSuccess() || !response.toString().contains("Killed")
				|| !response.executionSuccessButResultFailed()) {
			throw new RuntimeException("Test Failed");
		}
	}

	private void testErrorCode() {
		logger.info("Run Code with Syntax Error");
		CodePoolExecutorService.TaskResponse response = codePoolExecutorService
			.runTask(new CodePoolExecutorService.TaskRequest(CodeTestConstant.ERROR_CODE, "", null));
		System.out.println(response);
		logger.info("Run Code with Syntax Error Finished");
		if (response.isSuccess() || !response.toString().contains("SyntaxError")
				|| !response.executionSuccessButResultFailed()) {
			throw new RuntimeException("Test Failed");
		}
	}

	private void testNeedInput() {
		logger.info("Check Need Input");
		CodePoolExecutorService.TaskResponse response = codePoolExecutorService
			.runTask(new CodePoolExecutorService.TaskRequest(CodeTestConstant.NEED_INPUT, "DataFrame Data", null));
		System.out.println(response);
		logger.info("Run Need Input Finished");
		if (!response.isSuccess() || !response.stdOut().contains("DataFrame Data")) {
			throw new RuntimeException("Test Failed");
		}
	}

	private void testStudentScoreAnalysis() {
		logger.info("Run Student Score Analysis");
		CodePoolExecutorService.TaskResponse response = codePoolExecutorService
			.runTask(new CodePoolExecutorService.TaskRequest(CodeTestConstant.STUDENT_SCORE_ANALYSIS,
					CodeTestConstant.STUDENT_SCORE_ANALYSIS_INPUT, null));
		System.out.println(response);
		logger.info("Run Student Score Analysis Finished");
		if (!response.isSuccess() || !StringUtils.hasText(response.stdOut())) {
			throw new RuntimeException("Test Failed");
		}
	}

	@Test
	public void testPandasCode() {
		logger.info("Run Pandas Code");
		CodePoolExecutorService.TaskResponse response = codePoolExecutorService
			.runTask(new CodePoolExecutorService.TaskRequest(CodeTestConstant.ECOMMERCE_SALES_PANDAS_CODE,
					CodeTestConstant.ECOMMERCE_SALES_PANDAS_INPUT, null));
		System.out.println(response);
		logger.info("Run Pandas Code Finished");
		assert response.isSuccess()
				|| (response.executionSuccessButResultFailed() && response.toString().contains("ModuleNotFoundError"));
	}

	@Test
	@DisplayName("Concurrency Testing")
	public void testConcurrency() throws InterruptedException {
		ExecutorService executorService = Executors.newFixedThreadPool(10);
		final int taskNum = 5;
		CountDownLatch countDownLatch = new CountDownLatch(taskNum);
		AtomicInteger successTask = new AtomicInteger(0);

		Consumer<Consumer<LocalCodePoolExecutorServiceTest>> submitTask = consumer -> {
			executorService.submit(() -> {
				try {
					consumer.accept(this);
					successTask.incrementAndGet();
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				finally {
					countDownLatch.countDown();
				}
			});
		};

		submitTask.accept(LocalCodePoolExecutorServiceTest::testNormalCode);
		submitTask.accept(LocalCodePoolExecutorServiceTest::testTimeoutCode);
		submitTask.accept(LocalCodePoolExecutorServiceTest::testErrorCode);
		submitTask.accept(LocalCodePoolExecutorServiceTest::testNeedInput);
		submitTask.accept(LocalCodePoolExecutorServiceTest::testStudentScoreAnalysis);

		assert countDownLatch.await(600L, TimeUnit.SECONDS);
		logger.info("Success Task Number: {}", successTask.get());
		Assertions.assertEquals(taskNum, successTask.get());
	}

}
