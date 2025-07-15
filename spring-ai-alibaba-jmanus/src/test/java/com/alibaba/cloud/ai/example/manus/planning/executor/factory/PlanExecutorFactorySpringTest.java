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
package com.alibaba.cloud.ai.example.manus.planning.executor.factory;

import com.alibaba.cloud.ai.example.manus.OpenManusSpringBootApplication;
import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.service.AgentService;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.service.DynamicAgentLoader;
import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import com.alibaba.cloud.ai.example.manus.planning.executor.MapReducePlanExecutor;
import com.alibaba.cloud.ai.example.manus.planning.executor.PlanExecutor;
import com.alibaba.cloud.ai.example.manus.planning.executor.PlanExecutorInterface;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.ExecutionPlan;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.mapreduce.MapReduceExecutionPlan;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;

import java.util.List;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spring integration test for PlanExecutorFactory Tests the factory's ability to create
 * appropriate executors based on plan type
 */
@SpringBootTest(classes = OpenManusSpringBootApplication.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
@Disabled("for local testing only, skip in CI environment")
class PlanExecutorFactorySpringTest {

	private static final Logger log = LoggerFactory.getLogger(PlanExecutorFactorySpringTest.class);

	@Autowired
	private DynamicAgentLoader dynamicAgentLoader;

	@Autowired
	private LlmService llmService;

	@Autowired
	private AgentService agentService;

	@Autowired
	private PlanExecutionRecorder recorder;

	@Autowired
	private ManusProperties manusProperties;

	private PlanExecutorFactory planExecutorFactory;

	@BeforeEach
	void setUp() {
		log.info("Setting up PlanExecutorFactory test environment");
		planExecutorFactory = new PlanExecutorFactory(dynamicAgentLoader, llmService, agentService, recorder,
				manusProperties);

		// Verify that required dependencies are properly injected
		Assertions.assertNotNull(dynamicAgentLoader, "DynamicAgentLoader should be autowired");
		Assertions.assertNotNull(llmService, "LlmService should be autowired");
		Assertions.assertNotNull(agentService, "AgentService should be autowired");
		Assertions.assertNotNull(recorder, "PlanExecutionRecorder should be autowired");
		Assertions.assertNotNull(manusProperties, "ManusProperties should be autowired");
		Assertions.assertNotNull(planExecutorFactory, "PlanExecutorFactory should be created");
	}

	@Test
	@Order(1)
	@DisplayName("Test creating simple plan executor")
	void testCreateSimpleExecutor() {
		try {
			log.info("Step 1: Create simple execution plan");
			ExecutionPlan simplePlan = new ExecutionPlan("test-simple-001", "test-simple-001", "Simple test plan");
			simplePlan.setPlanType("simple");

			log.info("Step 2: Create executor using factory");
			PlanExecutorInterface executor = planExecutorFactory.createExecutor(simplePlan);

			// Verify the executor type
			Assertions.assertNotNull(executor, "Executor should not be null");
			Assertions.assertTrue(executor instanceof PlanExecutor, "Should create PlanExecutor for simple plan type");
			Assertions.assertFalse(executor instanceof MapReducePlanExecutor,
					"Should not create MapReducePlanExecutor for simple plan type");

			log.info("Simple executor created successfully: {}", executor.getClass().getSimpleName());

		}
		catch (Exception e) {
			log.error("Error occurred during simple executor test", e);
			Assertions.fail("Simple executor test failed: " + e.getMessage());
		}
	}

	@Test
	@Order(2)
	@DisplayName("Test creating advanced MapReduce executor")
	void testCreateAdvancedExecutor() {
		try {
			log.info("Step 1: Create advanced execution plan");
			MapReduceExecutionPlan advancedPlan = new MapReduceExecutionPlan();
			advancedPlan.setCurrentPlanId("test-advanced-001");
			advancedPlan.setTitle("Advanced MapReduce test plan");
			advancedPlan.setPlanType("advanced");

			log.info("Step 2: Create executor using factory");
			PlanExecutorInterface executor = planExecutorFactory.createExecutor(advancedPlan);

			// Verify the executor type
			Assertions.assertNotNull(executor, "Executor should not be null");
			Assertions.assertTrue(executor instanceof MapReducePlanExecutor,
					"Should create MapReducePlanExecutor for advanced plan type");

			log.info("Advanced executor created successfully: {}", executor.getClass().getSimpleName());

		}
		catch (Exception e) {
			log.error("Error occurred during advanced executor test", e);
			Assertions.fail("Advanced executor test failed: " + e.getMessage());
		}
	}

	@Test
	@Order(3)
	@DisplayName("Test creating executor with null plan type defaults to simple")
	void testCreateExecutorWithNullPlanType() {
		try {
			log.info("Step 1: Create plan with null plan type");
			ExecutionPlan nullTypePlan = new ExecutionPlan("test-null-001", "test-simple-001", "Plan with null type");
			nullTypePlan.setPlanType(null);

			log.info("Step 2: Create executor using factory");
			PlanExecutorInterface executor = planExecutorFactory.createExecutor(nullTypePlan);

			// Should default to simple executor
			Assertions.assertNotNull(executor, "Executor should not be null");
			Assertions.assertTrue(executor instanceof PlanExecutor,
					"Should default to PlanExecutor for null plan type");

			log.info("Null type plan defaulted to simple executor: {}", executor.getClass().getSimpleName());

		}
		catch (Exception e) {
			log.error("Error occurred during null plan type test", e);
			Assertions.fail("Null plan type test failed: " + e.getMessage());
		}
	}

	@Test
	@Order(4)
	@DisplayName("Test creating executor with unknown plan type defaults to simple")
	void testCreateExecutorWithUnknownPlanType() {
		try {
			log.info("Step 1: Create plan with unknown plan type");
			ExecutionPlan unknownTypePlan = new ExecutionPlan("test-unknown-001", "test-simple-001",
					"Plan with unknown type");
			unknownTypePlan.setPlanType("unknown-type");

			log.info("Step 2: Create executor using factory");
			PlanExecutorInterface executor = planExecutorFactory.createExecutor(unknownTypePlan);

			// Should default to simple executor
			Assertions.assertNotNull(executor, "Executor should not be null");
			Assertions.assertTrue(executor instanceof PlanExecutor,
					"Should default to PlanExecutor for unknown plan type");

			log.info("Unknown type plan defaulted to simple executor: {}", executor.getClass().getSimpleName());

		}
		catch (Exception e) {
			log.error("Error occurred during unknown plan type test", e);
			Assertions.fail("Unknown plan type test failed: " + e.getMessage());
		}
	}

	@Test
	@Order(5)
	@DisplayName("Test creating executor by explicit type")
	void testCreateExecutorByExplicitType() {
		try {
			log.info("Step 1: Test explicit simple executor creation");
			PlanExecutorInterface simpleExecutor = planExecutorFactory.createExecutorByType("simple",
					"test-explicit-001");
			Assertions.assertTrue(simpleExecutor instanceof PlanExecutor,
					"Should create PlanExecutor for explicit simple type");

			log.info("Step 2: Test explicit advanced executor creation");
			PlanExecutorInterface advancedExecutor = planExecutorFactory.createExecutorByType("advanced",
					"test-explicit-002");
			Assertions.assertTrue(advancedExecutor instanceof MapReducePlanExecutor,
					"Should create MapReducePlanExecutor for explicit advanced type");

			log.info("Step 3: Test explicit unknown type defaults to simple");
			PlanExecutorInterface unknownExecutor = planExecutorFactory.createExecutorByType("unknown",
					"test-explicit-003");
			Assertions.assertTrue(unknownExecutor instanceof PlanExecutor,
					"Should default to PlanExecutor for explicit unknown type");

			log.info("Explicit executor creation tests completed successfully");

		}
		catch (Exception e) {
			log.error("Error occurred during explicit executor type test", e);
			Assertions.fail("Explicit executor type test failed: " + e.getMessage());
		}
	}

	@Test
	@Order(6)
	@DisplayName("Test supported plan types functionality")
	void testSupportedPlanTypes() {
		try {
			log.info("Step 1: Get supported plan types");
			String[] supportedTypes = planExecutorFactory.getSupportedPlanTypes();

			Assertions.assertNotNull(supportedTypes, "Supported types should not be null");
			Assertions.assertEquals(2, supportedTypes.length, "Should support exactly 2 plan types");
			Assertions.assertTrue(List.of(supportedTypes).contains("simple"), "Should support simple plan type");
			Assertions.assertTrue(List.of(supportedTypes).contains("advanced"), "Should support advanced plan type");

			log.info("Step 2: Test plan type support checking");
			Assertions.assertTrue(planExecutorFactory.isPlanTypeSupported("simple"), "Should support simple type");
			Assertions.assertTrue(planExecutorFactory.isPlanTypeSupported("advanced"), "Should support advanced type");
			Assertions.assertTrue(planExecutorFactory.isPlanTypeSupported("SIMPLE"),
					"Should support case-insensitive simple type");
			Assertions.assertTrue(planExecutorFactory.isPlanTypeSupported("ADVANCED"),
					"Should support case-insensitive advanced type");
			Assertions.assertFalse(planExecutorFactory.isPlanTypeSupported("unknown"),
					"Should not support unknown type");
			Assertions.assertFalse(planExecutorFactory.isPlanTypeSupported(null), "Should not support null type");

			log.info("Supported plan types functionality test completed successfully");

		}
		catch (Exception e) {
			log.error("Error occurred during supported plan types test", e);
			Assertions.fail("Supported plan types test failed: " + e.getMessage());
		}
	}

	@Test
	@Order(7)
	@DisplayName("Test error handling with null plan")
	void testErrorHandlingWithNullPlan() {
		try {
			log.info("Step 1: Test factory with null plan");

			Exception exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
				planExecutorFactory.createExecutor(null);
			});

			Assertions.assertEquals("Plan cannot be null", exception.getMessage(),
					"Should throw IllegalArgumentException with correct message");

			log.info("Null plan error handling test completed successfully");

		}
		catch (Exception e) {
			log.error("Error occurred during null plan error handling test", e);
			Assertions.fail("Null plan error handling test failed: " + e.getMessage());
		}
	}

	@AfterEach
	void tearDown() {
		log.info("Cleaning up after test execution");
	}

	@AfterAll
	static void tearDownAll() {
		log.info("All PlanExecutorFactory tests completed");
	}

}
