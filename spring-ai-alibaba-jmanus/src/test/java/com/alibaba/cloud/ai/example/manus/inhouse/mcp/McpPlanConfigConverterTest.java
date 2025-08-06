package com.alibaba.cloud.ai.example.manus.inhouse.mcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import com.alibaba.cloud.ai.example.manus.inhouse.mcp.vo.McpPlanConfigVO;

/**
 * McpPlanConfigConverter单元测试
 */
class McpPlanConfigConverterTest {

	private McpPlanConfigConverter converter;

	@BeforeEach
	void setUp() {
		converter = new McpPlanConfigConverter();
	}

	@Test
	void testBasicConversion() {
		// 准备测试数据
		String planJson = """
				{
				    "planId": "planTemplate-1754276365157",
				    "title": "Plan for retrieving and saving Alibaba's stock information",
				    "userRequest": "打开百度查询阿里巴巴最近一周的股票。生成markdown文件到本地",
				    "steps": [
				        "[BROWSER_AGENT] Search for {company} stock information for the last {period}",
				        "[DEFAULT_AGENT] Save the searched information into a {fileType} file"
				    ]
				}
				""";

		// 执行转换
		McpPlanConfigVO result = converter.convert(planJson);

		// 验证结果
		assertEquals("planTemplate-1754276365157", result.getId());
		assertEquals("Plan for retrieving and saving Alibaba's stock information", result.getName());
		assertEquals("打开百度查询阿里巴巴最近一周的股票。生成markdown文件到本地", result.getDescription());
		assertEquals(3, result.getParameters().size()); // company, period, fileType

		// 验证参数
		assertTrue(result.getParameters().stream().anyMatch(p -> "company".equals(p.getName())));
		assertTrue(result.getParameters().stream().anyMatch(p -> "period".equals(p.getName())));
		assertTrue(result.getParameters().stream().anyMatch(p -> "fileType".equals(p.getName())));
	}

	@Test
	void testParameterParsing() {
		// 准备测试数据
		String planJson = """
				{
				    "planId": "test-plan",
				    "title": "Test Plan",
				    "userRequest": "Test request",
				    "steps": [
				        "请输入您的{name}",
				        "请输入您的{email}",
				        "确认您的{name}和{email}"
				    ]
				}
				""";

		// 执行转换
		McpPlanConfigVO result = converter.convert(planJson);

		// 验证结果
		assertEquals(2, result.getParameters().size()); // name和email，去重
		assertTrue(result.getParameters().stream().anyMatch(p -> "name".equals(p.getName())));
		assertTrue(result.getParameters().stream().anyMatch(p -> "email".equals(p.getName())));
	}

	@Test
	void testNoParameters() {
		// 准备测试数据
		String planJson = """
				{
				    "planId": "test-plan",
				    "title": "Test Plan",
				    "userRequest": "Test request",
				    "steps": [
				        "[BROWSER_AGENT] Search for Alibaba's stock information",
				        "[DEFAULT_AGENT] Save the information"
				    ]
				}
				""";

		// 执行转换
		McpPlanConfigVO result = converter.convert(planJson);

		// 验证结果
		assertEquals(0, result.getParameters().size());
	}

	@Test
	void testNullPlanJson() {
		// 测试null输入
		assertThrows(McpPlanConversionException.class, () -> {
			converter.convert(null);
		});
	}

	@Test
	void testEmptyPlanId() {
		// 准备测试数据
		String planJson = """
				{
				    "planId": "",
				    "title": "Test Plan",
				    "userRequest": "Test request",
				    "steps": [
				        "test"
				    ]
				}
				""";

		// 测试空的planId
		assertThrows(McpPlanConversionException.class, () -> {
			converter.convert(planJson);
		});
	}

	@Test
	void testEmptyTitle() {
		// 准备测试数据
		String planJson = """
				{
				    "planId": "test-plan",
				    "title": "",
				    "userRequest": "Test request",
				    "steps": [
				        "test"
				    ]
				}
				""";

		// 测试空的title
		assertThrows(McpPlanConversionException.class, () -> {
			converter.convert(planJson);
		});
	}

	@Test
	void testEmptyUserRequest() {
		// 准备测试数据
		String planJson = """
				{
				    "planId": "test-plan",
				    "title": "Test Plan",
				    "userRequest": "",
				    "steps": [
				        "test"
				    ]
				}
				""";

		// 测试空的userRequest
		assertThrows(McpPlanConversionException.class, () -> {
			converter.convert(planJson);
		});
	}

}
