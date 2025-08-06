package com.alibaba.cloud.ai.example.manus.inhouse.mcp;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.alibaba.cloud.ai.example.manus.inhouse.mcp.vo.McpPlanConfigVO;
import com.alibaba.cloud.ai.example.manus.inhouse.mcp.vo.McpPlanParameterVO;

/**
 * McpPlan组件简单测试类
 */
public class SimpleTest {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	public static void main(String[] args) {
		System.out.println("=== McpPlan 组件简单测试开始 ===");

		try {
			// 测试基本转换功能
			testBasicConversion();

			System.out.println("\n");

			// 测试参数解析功能
			testParameterParsing();

			System.out.println("\n");

			// 测试McpPlanToolProxy功能
			testMcpPlanToolProxy();

			System.out.println("\n");

		}
		catch (Exception e) {
			System.err.println("测试过程中发生异常: " + e.getMessage());
			e.printStackTrace();
		}

		System.out.println("\n=== McpPlan 组件简单测试完成 ===");
	}

	/**
	 * 测试基本转换功能
	 */
	private static void testBasicConversion() {
		System.out.println("--- 测试基本转换功能 ---");

		// 创建示例Plan JSON数据
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
		McpPlanConfigConverter converter = new McpPlanConfigConverter();
		McpPlanConfigVO result = converter.convert(planJson);

		// 输出结果
		System.out.println("转换结果:");
		System.out.println("ID: " + result.getId());
		System.out.println("名称: " + result.getName());
		System.out.println("描述: " + result.getDescription());
		System.out.println("参数数量: " + result.getParameters().size());

		for (McpPlanParameterVO param : result.getParameters()) {
			System.out
				.println("参数: " + param.getName() + " (类型: " + param.getType() + ", 必需: " + param.isRequired() + ")");
		}
	}

	/**
	 * 测试参数解析功能
	 */
	private static void testParameterParsing() {
		System.out.println("--- 测试参数解析功能 ---");

		// 创建测试数据
		List<String> steps = Arrays.asList("请输入您的{name}", "请输入您的{email}", "确认您的{name}和{email}");

		// 执行参数解析
		McpPlanConfigConverter converter = new McpPlanConfigConverter();
		List<McpPlanParameterVO> parameters = converter.parseParameters(steps);

		// 输出结果
		System.out.println("参数解析结果:");
		System.out.println("参数数量: " + parameters.size());

		for (McpPlanParameterVO param : parameters) {
			System.out.println("参数: " + param.getName() + " - " + param.getDescription());
		}
	}

	/**
	 * 测试McpPlanToolProxy功能
	 */
	private static void testMcpPlanToolProxy() {
		System.out.println("--- 测试McpPlanToolProxy功能 ---");

		// 创建测试数据
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
		McpPlanConfigConverter converter = new McpPlanConfigConverter();
		McpPlanConfigVO configVO = converter.convert(planJson);

		System.out.println("=== 原始McpPlanConfigVO对象 ===");
		System.out.println("configVO: " + configVO);
		System.out.println("configVO.getId(): " + configVO.getId());
		System.out.println("configVO.getName(): " + configVO.getName());
		System.out.println("configVO.getDescription(): " + configVO.getDescription());
		System.out.println("configVO.getParameters().size(): " + configVO.getParameters().size());

		for (int i = 0; i < configVO.getParameters().size(); i++) {
			McpPlanParameterVO param = configVO.getParameters().get(i);
			System.out.println("configVO.getParameters().get(" + i + "): " + param);
			System.out.println("  - name: " + param.getName());
			System.out.println("  - type: " + param.getType());
			System.out.println("  - description: " + param.getDescription());
			System.out.println("  - required: " + param.isRequired());
		}

		// 创建McpPlanToolProxy（注意：这里需要注入依赖，实际使用时应该通过Spring容器获取）
		// McpPlanToolProxy proxy = new McpPlanToolProxy(configVO, planTemplateService,
		// planPollingService);
		System.out.println("注意：McpPlanToolProxy需要Spring依赖注入，无法在静态方法中直接创建");

		System.out.println("\n=== McpPlanToolProxy对象 ===");
		System.out.println("注意：McpPlanToolProxy需要Spring依赖注入，无法在静态方法中直接创建");
		System.out.println("实际使用时应该通过Spring容器获取McpPlanToolProxy实例");

		System.out.println("\n=== MCP工具规范 ===");
		System.out.println("工具规范需要通过Spring容器中的McpPlanToolProxy实例创建");
	}

}
