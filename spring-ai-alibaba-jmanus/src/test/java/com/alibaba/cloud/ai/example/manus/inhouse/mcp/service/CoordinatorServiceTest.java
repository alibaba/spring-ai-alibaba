package com.alibaba.cloud.ai.example.manus.inhouse.mcp.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.alibaba.cloud.ai.example.manus.inhouse.mcp.vo.McpPlanConfigVO;
import com.alibaba.cloud.ai.example.manus.inhouse.mcp.vo.McpPlanParameterVO;
import com.alibaba.cloud.ai.example.manus.inhouse.mcp.tool.coordinator.CoordinatorTool;

/**
 * CoordinatorService测试类
 */
@SpringBootTest
class CoordinatorServiceTest {

	private CoordinatorService coordinatorService;

	@BeforeEach
	void setUp() {
		coordinatorService = new CoordinatorService();
	}

	@Test
	void testConvertToCoordinatorTool() {
		// 创建测试用的McpPlanConfigVO
		McpPlanConfigVO config = new McpPlanConfigVO();
		config.setId("test-plan-001");
		config.setName("测试计划");
		config.setDescription("这是一个测试计划描述");

		// 创建参数列表
		List<McpPlanParameterVO> parameters = new ArrayList<>();
		McpPlanParameterVO param1 = new McpPlanParameterVO("company", "string", "公司名称", true);
		McpPlanParameterVO param2 = new McpPlanParameterVO("period", "string", "时间周期", false);
		parameters.add(param1);
		parameters.add(param2);
		config.setParameters(parameters);

		// 执行转换
		CoordinatorTool tool = coordinatorService.convertToCoordinatorTool(config);

		// 验证转换结果
		assertNotNull(tool);
		assertEquals("example", tool.getEndpoint());
		assertEquals("测试计划", tool.getToolName());
		assertEquals("这是一个测试计划描述", tool.getToolDescription());
		assertNotNull(tool.getToolSchema());
		assertTrue(tool.getToolSchema().contains("company"));
		assertTrue(tool.getToolSchema().contains("period"));
	}

	@Test
	void testConvertToCoordinatorTools() {
		// 创建多个测试用的McpPlanConfigVO
		List<McpPlanConfigVO> configs = new ArrayList<>();

		McpPlanConfigVO config1 = new McpPlanConfigVO();
		config1.setId("test-plan-001");
		config1.setName("测试计划1");
		config1.setDescription("这是第一个测试计划");

		McpPlanConfigVO config2 = new McpPlanConfigVO();
		config2.setId("test-plan-002");
		config2.setName("测试计划2");
		config2.setDescription("这是第二个测试计划");

		configs.add(config1);
		configs.add(config2);

		// 执行批量转换
		List<CoordinatorTool> tools = coordinatorService.convertToCoordinatorTools(configs);

		// 验证转换结果
		assertNotNull(tools);
		assertEquals(2, tools.size());
		assertEquals("测试计划1", tools.get(0).getToolName());
		assertEquals("测试计划2", tools.get(1).getToolName());
		assertEquals("example", tools.get(0).getEndpoint());
		assertEquals("example", tools.get(1).getEndpoint());
	}

	@Test
	void testConvertToCoordinatorToolWithNullConfig() {
		// 测试空配置的情况
		CoordinatorTool tool = coordinatorService.convertToCoordinatorTool(null);
		assertNull(tool);
	}

	@Test
	void testConvertToCoordinatorToolsWithEmptyList() {
		// 测试空列表的情况
		List<CoordinatorTool> tools = coordinatorService.convertToCoordinatorTools(new ArrayList<>());
		assertNotNull(tools);
		assertTrue(tools.isEmpty());
	}

	@Test
	void testConvertToCoordinatorToolsWithNullList() {
		// 测试null列表的情况
		List<CoordinatorTool> tools = coordinatorService.convertToCoordinatorTools(null);
		assertNotNull(tools);
		assertTrue(tools.isEmpty());
	}

}