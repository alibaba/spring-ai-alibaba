package com.alibaba.cloud.ai.example.manus.workflow;

import com.alibaba.cloud.ai.example.manus.OpenManusSpringBootApplication;
import com.alibaba.cloud.ai.example.manus.planning.PlanningFactory;
import com.alibaba.cloud.ai.example.manus.planning.coordinator.PlanIdDispatcher;
import com.alibaba.cloud.ai.example.manus.planning.model.vo.mapreduce.MapReduceExecutionPlan;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Spring integration test class for SummaryWorkflow using real Spring context
 * to test MapReduce-based content summarization functionality
 */
@SpringBootTest(classes = OpenManusSpringBootApplication.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
@Disabled("For local testing only, skip in CI environment") // Add this line for CI compatibility
class SummaryWorkflowSpringTest {
	
	private static final Logger log = LoggerFactory.getLogger(SummaryWorkflowSpringTest.class);

	@Autowired
	private SummaryWorkflow summaryWorkflow;

	@Autowired
	private PlanningFactory planningFactory;

	@Autowired
	private PlanIdDispatcher planIdDispatcher;

	// Test data constants
	private static final String TEST_FILE_NAME = "test_document.md";
	
	private static final String TEST_CONTENT = """
			# Spring AI Alibaba Project Overview
			
			## Introduction
			Spring AI Alibaba is a comprehensive AI framework that provides intelligent agent capabilities.
			The framework includes several key components:
			
			## Core Components
			1. **Agent Management System**: Handles dynamic agent creation and lifecycle management
			2. **MapReduce Workflow**: Enables distributed processing of large content files
			3. **Tool Integration**: Supports various tools including browser automation and file processing
			4. **Planning Coordination**: Manages complex execution plans with multiple steps
			
			## Key Features
			- Dynamic agent configuration through YAML files
			- Automatic scanning and loading of agent definitions
			- Protection mechanisms for critical system agents
			- Flexible workflow templates with parameter optimization
			
			## Technical Details
			The system uses Spring Boot framework with Spring AI integration.
			It supports multiple agent types including data preparation, map task, reduce task, and finalization agents.
			The MapReduce implementation allows for scalable content processing and intelligent summarization.
			
			## Use Cases
			- Large document analysis and summarization
			- Content extraction based on specific query keywords
			- Distributed data processing workflows
			- Intelligent content structuring and formatting
			""";

	private static final String SHORT_CONTENT = """
			# Quick Test Document
			
			This is a short test document for quick summarization testing.
			It contains basic information about Spring AI Alibaba framework.
			The content is intentionally brief for fast processing validation.
			""";

	@BeforeEach
	void setUp() {
		log.info("Setting up SummaryWorkflow test environment");
		// Verify that required beans are properly injected
		Assertions.assertNotNull(summaryWorkflow, "SummaryWorkflow should be autowired");
		Assertions.assertNotNull(planningFactory, "PlanningFactory should be autowired");
		Assertions.assertNotNull(planIdDispatcher, "PlanIdDispatcher should be autowired");
	}

	@Test
	@Order(1)
	@DisplayName("Test basic MapReduce summary workflow execution")
	void testBasicSummaryWorkflow() {
		try {
			log.info("Step 1: Execute summary workflow with test content");
			
			String queryKey = "Spring AI features";
			String testPlanId = planIdDispatcher.generatePlanId();
			
			// Execute the summary workflow
			CompletableFuture<String> resultFuture = summaryWorkflow.executeSummaryWorkflow(
					testPlanId, TEST_FILE_NAME, TEST_CONTENT, queryKey);
			
			// Wait for completion with timeout
			String result = resultFuture.get(30, TimeUnit.SECONDS);
			
			// Verify the result
			Assertions.assertNotNull(result, "Summary result should not be null");
			Assertions.assertTrue(result.contains("MapReduce"), "Result should mention MapReduce");
			Assertions.assertTrue(result.contains("执行完成"), "Result should indicate completion");
			
			log.info("Summary workflow execution result: {}", result);
			log.info("Basic summary workflow test completed successfully");

		} catch (Exception e) {
			log.error("Error occurred during basic summary workflow test", e);
			Assertions.fail("Basic summary workflow test failed: " + e.getMessage());
		}
	}

	@Test
	@Order(2)
	@DisplayName("Test summary workflow with different query keys")
	void testSummaryWorkflowWithVariousQueries() {
		try {
			// Test with different query keywords
			String[] queryKeys = {
					"agent management",
					"workflow processing",
					"technical architecture",
					"system components"
			};
			
			for (String queryKey : queryKeys) {
				log.info("Step {}: Testing with query key: {}", 
						Arrays.asList(queryKeys).indexOf(queryKey) + 1, queryKey);
				
				String testPlanId = planIdDispatcher.generatePlanId();
				CompletableFuture<String> resultFuture = summaryWorkflow.executeSummaryWorkflow(
						testPlanId, "test_" + queryKey.replace(" ", "_") + ".md", TEST_CONTENT, queryKey);
				
				String result = resultFuture.get(25, TimeUnit.SECONDS);
				
				// Verify each result
				Assertions.assertNotNull(result, "Result should not be null for query: " + queryKey);
				Assertions.assertTrue(result.contains("计划ID"), "Result should contain plan ID");
				Assertions.assertTrue(result.contains("执行完成"), "Result should indicate completion");
				
				log.info("Query '{}' completed with result length: {}", queryKey, result.length());
			}
			
			log.info("Various query keys test completed successfully");

		} catch (Exception e) {
			log.error("Error occurred during various query keys test", e);
			Assertions.fail("Various query keys test failed: " + e.getMessage());
		}
	}

	@Test
	@Order(3)
	@DisplayName("Test quick summary workflow for small content")
	void testQuickSummaryWorkflow() {
		try {
			log.info("Step 1: Execute quick summary workflow");
			
			String queryKey = "framework overview";
			
			// Execute the quick summary workflow using the main workflow method
			String planId = "test_quick_summary_" + System.currentTimeMillis();
			CompletableFuture<String> resultFuture = summaryWorkflow.executeSummaryWorkflow(
					planId, "quick_test.md", SHORT_CONTENT, queryKey);
			
			// Wait for completion with timeout
			String result = resultFuture.get(20, TimeUnit.SECONDS);
			
			// Verify the result
			Assertions.assertNotNull(result, "Quick summary result should not be null");
			Assertions.assertTrue(result.contains("MapReduce") || result.contains("快速总结"), 
					"Result should mention MapReduce or quick summary");
			
			log.info("Quick summary workflow result: {}", result);
			log.info("Quick summary workflow test completed successfully");

		} catch (Exception e) {
			log.error("Error occurred during quick summary workflow test", e);
			Assertions.fail("Quick summary workflow test failed: " + e.getMessage());
		}
	}

	@Test
	@Order(4)
	@DisplayName("Test workflow with edge cases and error handling")
	void testWorkflowEdgeCases() {
		try {
			// Test with empty content
			log.info("Step 1: Testing with empty content");
			String testPlanId1 = planIdDispatcher.generatePlanId();
			CompletableFuture<String> emptyResultFuture = summaryWorkflow.executeSummaryWorkflow(
					testPlanId1, "empty.md", "", "test query");
			
			String emptyResult = emptyResultFuture.get(15, TimeUnit.SECONDS);
			Assertions.assertNotNull(emptyResult, "Empty content result should not be null");
			
			// Test with very long query key
			log.info("Step 2: Testing with long query key");
			String longQueryKey = "This is a very long query key that tests the system's ability to handle extended input parameters and validate proper processing of verbose search criteria";
			String testPlanId2 = planIdDispatcher.generatePlanId();
			CompletableFuture<String> longQueryFuture = summaryWorkflow.executeSummaryWorkflow(
					testPlanId2, "long_query_test.md", SHORT_CONTENT, longQueryKey);
			
			String longQueryResult = longQueryFuture.get(20, TimeUnit.SECONDS);
			Assertions.assertNotNull(longQueryResult, "Long query result should not be null");
			
			// Test with special characters in filename
			log.info("Step 3: Testing with special characters in filename");
			String testPlanId3 = planIdDispatcher.generatePlanId();
			CompletableFuture<String> specialCharFuture = summaryWorkflow.executeSummaryWorkflow(
					testPlanId3, "test-文件_名称@2024.md", SHORT_CONTENT, "special test");
			
			String specialCharResult = specialCharFuture.get(15, TimeUnit.SECONDS);
			Assertions.assertNotNull(specialCharResult, "Special character filename result should not be null");
			
			log.info("Edge cases test completed successfully");

		} catch (Exception e) {
			log.error("Error occurred during edge cases test", e);
			Assertions.fail("Edge cases test failed: " + e.getMessage());
		}
	}

	@Test
	@Order(5)
	@DisplayName("Test concurrent workflow executions")
	void testConcurrentWorkflowExecution() {
		try {
			log.info("Step 1: Execute multiple concurrent summary workflows");
			
			// Create multiple concurrent executions
			int concurrentCount = 3;
			CompletableFuture<String>[] futures = new CompletableFuture[concurrentCount];
			
			for (int i = 0; i < concurrentCount; i++) {
				final int index = i;
				String testPlanId = planIdDispatcher.generatePlanId();
				futures[i] = summaryWorkflow.executeSummaryWorkflow(
						testPlanId,
						"concurrent_test_" + index + ".md", 
						TEST_CONTENT, 
						"concurrent query " + index);
			}
			
			// Wait for all executions to complete
			CompletableFuture<Void> allOf = CompletableFuture.allOf(futures);
			allOf.get(60, TimeUnit.SECONDS);
			
			// Verify all results
			for (int i = 0; i < concurrentCount; i++) {
				String result = futures[i].get();
				Assertions.assertNotNull(result, "Concurrent result " + i + " should not be null");
				Assertions.assertTrue(result.contains("计划ID"), "Result should contain unique plan ID");
				log.info("Concurrent execution {} completed with plan ID in result", i);
			}
			
			log.info("Concurrent workflow execution test completed successfully");

		} catch (Exception e) {
			log.error("Error occurred during concurrent workflow test", e);
			Assertions.fail("Concurrent workflow test failed: " + e.getMessage());
		}
	}

	@Test
	@Order(6)
	@DisplayName("Test workflow plan generation and validation")
	void testWorkflowPlanGeneration() {
		try {
			log.info("Step 1: Test plan generation through reflection");
			
			// Use reflection to access the private buildSummaryExecutionPlan method for testing
			java.lang.reflect.Method buildPlanMethod = SummaryWorkflow.class
					.getDeclaredMethod("buildSummaryExecutionPlan", String.class, String.class, String.class);
			buildPlanMethod.setAccessible(true);
			
			MapReduceExecutionPlan plan = (MapReduceExecutionPlan) buildPlanMethod
					.invoke(summaryWorkflow, "test_plan.md", TEST_CONTENT, "plan validation");
			
			// Verify plan structure
			Assertions.assertNotNull(plan, "Execution plan should not be null");
			Assertions.assertNotNull(plan.getPlanId(), "Plan should have an ID");
			Assertions.assertEquals("advanced", plan.getPlanType(), "Plan type should be advanced");
			Assertions.assertTrue(plan.getTitle().contains("内容智能"), "Plan title should contain expected text");
			
			log.info("Generated plan ID: {}", plan.getPlanId());
			log.info("Plan type: {}", plan.getPlanType());
			log.info("Plan title: {}", plan.getTitle());
			
			// Verify plan steps structure
			Assertions.assertNotNull(plan.getSteps(), "Plan should have steps");
			Assertions.assertFalse(plan.getSteps().isEmpty(), "Plan should not have empty steps");
			
			log.info("Plan generation and validation test completed successfully");

		} catch (Exception e) {
			log.error("Error occurred during plan generation test", e);
			Assertions.fail("Plan generation test failed: " + e.getMessage());
		}
	}

	@Test
	@Order(7)
	@DisplayName("Test workflow performance and timeout handling")
	void testWorkflowPerformance() {
		try {
			log.info("Step 1: Measure workflow execution time");
			
			long startTime = System.currentTimeMillis();
			String testPlanId = planIdDispatcher.generatePlanId();
			
			CompletableFuture<String> resultFuture = summaryWorkflow.executeSummaryWorkflow(
					testPlanId, "performance_test.md", TEST_CONTENT, "performance measurement");
			
			String result = resultFuture.get(45, TimeUnit.SECONDS);
			
			long executionTime = System.currentTimeMillis() - startTime;
			
			// Verify result and performance
			Assertions.assertNotNull(result, "Performance test result should not be null");
			Assertions.assertTrue(executionTime < 45000, "Execution should complete within reasonable time");
			
			log.info("Workflow execution completed in {} ms", executionTime);
			log.info("Performance test result length: {} characters", result.length());
			
			// Verify result contains expected information
			Assertions.assertTrue(result.contains("执行完成"), "Result should indicate completion");
			Assertions.assertTrue(result.contains("📋"), "Result should contain formatted sections");
			
			log.info("Performance and timeout handling test completed successfully");

		} catch (Exception e) {
			log.error("Error occurred during performance test", e);
			Assertions.fail("Performance test failed: " + e.getMessage());
		}
	}

	@AfterEach
	void tearDown() {
		log.info("Cleaning up after test execution");
		// Add any necessary cleanup logic here
	}

	@AfterAll
	static void tearDownAll() {
		log.info("All SummaryWorkflow tests completed");
	}

}
