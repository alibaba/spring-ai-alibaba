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

import com.alibaba.cloud.ai.config.CodeExecutorConfiguration;
import com.alibaba.cloud.ai.tool.PythonExecutorTool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StringUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@SpringBootTest(classes = { CodeExecutorConfiguration.class })
@DisplayName("Run Python Code in Docker Test Without Network")
@ActiveProfiles("docker")
public class DockerCodePoolExecutorServiceTest {

	private static final Logger log = LoggerFactory.getLogger(DockerCodePoolExecutorServiceTest.class);

	@Autowired
	private PythonExecutorTool pythonExecutorTool;

	private void testNormalCode() {
		log.info("Run Normal Code");
		String response = pythonExecutorTool.executePythonCode(CodeConstant.NORMAL_CODE, null, "DataFrame Data");
		System.out.println(response);
		log.info("Run Normal Code Finished");
		if (!response.contains("3628800")) {
			throw new RuntimeException("Test Failed");
		}
	}

	private void testPipInstall() {
		log.info("Run Code with Third-parties Installed");
		String response = pythonExecutorTool.executePythonCode(CodeConstant.CODE_WITH_DEPENDENCY, "numpy==2.2.6",
				"DataFrame Data");
		System.out.println(response);
		log.info("Run Code with Third-parties Installed Finished");
		if (response.contains("ModuleNotFoundError")) {
			throw new RuntimeException("Test Failed");
		}
	}

	private void testTimeoutCode() {
		log.info("Run Code with Endless Loop");
		String response = pythonExecutorTool.executePythonCode(CodeConstant.TIMEOUT_CODE, null, "DataFrame Data");
		System.out.println(response);
		log.info("Run Code with Endless Loop Finished");
		if (!response.contains("Killed")) {
			throw new RuntimeException("Test Failed");
		}
	}

	private void testErrorCode() {
		log.info("Run Code with Syntax Error");
		String response = pythonExecutorTool.executePythonCode(CodeConstant.ERROR_CODE, null, "DataFrame Data");
		System.out.println(response);
		log.info("Run Code with Syntax Error Finished");
		if (!response.contains("SyntaxError")) {
			throw new RuntimeException("Test Failed");
		}
	}

	private void testNeedInput() {
		log.info("Check Need Input");
		String response = pythonExecutorTool.executePythonCode(CodeConstant.NEED_INPUT, null, "DataFrame Data");
		System.out.println(response);
		log.info("Run Need Input Finished");
		if (!response.contains("DataFrame Data")) {
			throw new RuntimeException("Test Failed");
		}
	}

	private void testStudentScoreAnalysis() {
		log.info("Run Student Score Analysis");
		String response = pythonExecutorTool.executePythonCode(CodeConstant.STUDENT_SCORE_ANALYSIS, null,
				CodeConstant.STUDENT_SCORE_ANALYSIS_INPUT);
		System.out.println(response);
		log.info("Run Student Score Analysis Finished");
		if (!StringUtils.hasText(response)) {
			throw new RuntimeException("Test Failed");
		}
	}

	private void testPandasCode() {
		log.info("Run Pandas Code");
		String response = pythonExecutorTool.executePythonCode(CodeConstant.ECOMMERCE_SALES_PANDAS_CODE, null,
				CodeConstant.ECOMMERCE_SALES_PANDAS_INPUT);
		System.out.println(response);
		log.info("Run Pandas Code Finished");
		if (response.contains("ModuleNotFoundError")) {
			throw new RuntimeException("Test Failed");
		}
	}

	@Test
	@DisplayName("Concurrency Testing")
	public void testConcurrency() throws InterruptedException {
		ExecutorService executorService = Executors.newFixedThreadPool(10);
		final int taskNum = 7;
		CountDownLatch countDownLatch = new CountDownLatch(taskNum);
		AtomicInteger successTask = new AtomicInteger(0);

		Consumer<Consumer<DockerCodePoolExecutorServiceTest>> submitTask = consumer -> {
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

		submitTask.accept(DockerCodePoolExecutorServiceTest::testNormalCode);
		submitTask.accept(DockerCodePoolExecutorServiceTest::testPipInstall);
		submitTask.accept(DockerCodePoolExecutorServiceTest::testTimeoutCode);
		submitTask.accept(DockerCodePoolExecutorServiceTest::testErrorCode);
		submitTask.accept(DockerCodePoolExecutorServiceTest::testNeedInput);
		submitTask.accept(DockerCodePoolExecutorServiceTest::testStudentScoreAnalysis);
		submitTask.accept(DockerCodePoolExecutorServiceTest::testPandasCode);

		assert countDownLatch.await(600L, TimeUnit.SECONDS);
		log.info("Success Task Number: {}", successTask.get());
		Assertions.assertEquals(taskNum, successTask.get());
	}

}
