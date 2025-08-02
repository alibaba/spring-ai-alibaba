/*
 * Copyright 2024 - 2024 the original author or authors.
 */

package com.alibaba.cloud.ai.example.manus.inhouse.registry;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.alibaba.cloud.ai.example.manus.inhouse.annotation.McpTool;
import com.alibaba.cloud.ai.example.manus.inhouse.annotation.McpToolSchema;
import com.alibaba.cloud.ai.example.manus.tool.AbstractBaseTool;
import com.alibaba.cloud.ai.example.manus.tool.ToolCallBiFunctionDef;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;

/**
 * MCP 工具注册器
 *
 * 自动发现和注册带有 @McpTool 注解的工具类
 */
@Component
public class McpToolRegistry {

	private static final Logger log = LoggerFactory.getLogger(McpToolRegistry.class);

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private ObjectMapper objectMapper;

	/**
	 * 发现并注册所有 MCP 工具
	 */
	public List<McpServerFeatures.SyncToolSpecification> discoverAndRegisterTools() {
		log.info("开始发现 MCP 工具...");

		List<McpServerFeatures.SyncToolSpecification> tools = new ArrayList<>();

		try {
			// 使用更安全的方式扫描 Bean
			String[] beanNames = applicationContext.getBeanDefinitionNames();
			
			for (String beanName : beanNames) {
				try {
					Object bean = applicationContext.getBean(beanName);
					
					// 检查是否有 @McpTool 注解
					if (bean.getClass().isAnnotationPresent(McpTool.class)) {
						McpTool annotation = bean.getClass().getAnnotation(McpTool.class);
						if (annotation.enabled()) {
							McpServerFeatures.SyncToolSpecification toolSpec = createToolSpecification(bean);
							tools.add(toolSpec);
							log.debug("发现工具: {}", beanName);
						}
					}
				} catch (Exception e) {
					// 忽略无法获取的 Bean（可能还在初始化中）
					log.debug("跳过 Bean {}: {}", beanName, e.getMessage());
				}
			}
		} catch (Exception e) {
			log.warn("扫描工具时出现异常: {}", e.getMessage());
		}

		log.info("发现并注册了 {} 个 MCP 工具", tools.size());
		tools.forEach(tool -> {
			String toolName = tool.tool().name();
			log.info("  - {}: {}", toolName, tool.tool().description());
		});

		return tools;
	}

	/**
	 * 为工具 Bean 创建 MCP 工具规范
	 */
	private McpServerFeatures.SyncToolSpecification createToolSpecification(Object toolBean) {
		McpTool annotation = toolBean.getClass().getAnnotation(McpTool.class);
		
		// 获取工具名称：优先使用注解值，为空时从工具对象获取
		String toolName = getToolName(toolBean, annotation);
		
		// 获取工具描述：优先使用注解值，为空时从工具对象获取
		String description = getToolDescription(toolBean, annotation);

		return McpServerFeatures.SyncToolSpecification.builder()
			.tool(new Tool(toolName, description, getToolSchema(toolBean)))
			.callHandler((exchange, request) -> invokeTool(toolBean, request))
			.build();
	}

	/**
	 * 获取工具名称
	 */
	private String getToolName(Object toolBean, McpTool annotation) {
		String name = annotation.name();
		if (name != null && !name.trim().isEmpty()) {
			return name;
		}
		
		// 从工具对象获取名称
		try {
			if (toolBean instanceof ToolCallBiFunctionDef) {
				return ((ToolCallBiFunctionDef<?>) toolBean).getName();
			}
			
			// 尝试调用 getName() 方法
			Method getNameMethod = toolBean.getClass().getMethod("getName");
			Object result = getNameMethod.invoke(toolBean);
			if (result instanceof String) {
				return (String) result;
			}
		} catch (Exception e) {
			log.debug("无法从工具 {} 获取名称: {}", toolBean.getClass().getSimpleName(), e.getMessage());
		}
		
		// 使用类名作为默认名称
		return toolBean.getClass().getSimpleName().toLowerCase();
	}

	/**
	 * 获取工具描述
	 */
	private String getToolDescription(Object toolBean, McpTool annotation) {
		String description = annotation.description();
		if (description != null && !description.trim().isEmpty()) {
			return description;
		}
		
		// 从工具对象获取描述
		try {
			if (toolBean instanceof ToolCallBiFunctionDef) {
				return ((ToolCallBiFunctionDef<?>) toolBean).getDescription();
			}
			
			// 尝试调用 getDescription() 方法
			Method getDescriptionMethod = toolBean.getClass().getMethod("getDescription");
			Object result = getDescriptionMethod.invoke(toolBean);
			if (result instanceof String) {
				return (String) result;
			}
		} catch (Exception e) {
			log.debug("无法从工具 {} 获取描述: {}", toolBean.getClass().getSimpleName(), e.getMessage());
		}
		
		// 使用默认描述
		return "工具描述";
	}

	/**
	 * 获取工具的 JSON Schema
	 */
	private String getToolSchema(Object toolBean) {
		// 1. 优先从 @McpToolSchema 注解获取
		if (toolBean.getClass().isAnnotationPresent(McpToolSchema.class)) {
			return toolBean.getClass().getAnnotation(McpToolSchema.class).value();
		}

		// 2. 从 getParameters() 方法获取（如果存在）
		try {
			Method getParameters = toolBean.getClass().getMethod("getParameters");
			Object result = getParameters.invoke(toolBean);
			if (result instanceof String) {
				return (String) result;
			}
		}
		catch (Exception e) {
			log.debug("工具 {} 没有 getParameters() 方法", toolBean.getClass().getSimpleName());
		}

		// 3. 返回默认 Schema
		return createDefaultSchema();
	}

	/**
	 * 调用工具
	 */
	private CallToolResult invokeTool(Object toolBean, CallToolRequest request) {
		try {
			log.debug("调用工具: {}, 参数: {}", toolBean.getClass().getSimpleName(), request.arguments());

			if (toolBean instanceof AbstractBaseTool) {
				return invokeBaseTool((AbstractBaseTool<?>) toolBean, request);
			}
			else {
				return invokeGenericTool(toolBean, request);
			}
		}
		catch (Exception e) {
			log.error("工具调用失败: {}", e.getMessage(), e);
			return new CallToolResult(List.of(new McpSchema.TextContent("工具调用失败: " + e.getMessage())), null);
		}
	}

	/**
	 * 调用 AbstractBaseTool 类型的工具
	 */
	private CallToolResult invokeBaseTool(AbstractBaseTool<?> tool, CallToolRequest request) {
		try {
			// 直接传递 Map 参数给 run 方法
			Method runMethod = tool.getClass().getMethod("run", Object.class);
			ToolExecuteResult result = (ToolExecuteResult) runMethod.invoke(tool, request.arguments());

			if (result != null && result.getOutput() != null) {
				return new CallToolResult(List.of(new McpSchema.TextContent(result.getOutput())), null);
			}
			else {
				return new CallToolResult(List.of(new McpSchema.TextContent("工具执行完成，无返回结果")), null);
			}
		}
		catch (Exception e) {
			log.error("调用 AbstractBaseTool 失败: {}", e.getMessage(), e);
			return new CallToolResult(List.of(new McpSchema.TextContent("工具调用失败: " + e.getMessage())), null);
		}
	}

	/**
	 * 调用通用工具
	 */
	private CallToolResult invokeGenericTool(Object toolBean, CallToolRequest request) {
		try {
			// 尝试调用 execute 方法
			Method executeMethod = toolBean.getClass().getMethod("execute", Map.class);
			Object result = executeMethod.invoke(toolBean, request.arguments());

			if (result != null) {
				String resultStr = result instanceof String ? (String) result : objectMapper.writeValueAsString(result);
				return new CallToolResult(List.of(new McpSchema.TextContent(resultStr)), null);
			}
			else {
				return new CallToolResult(List.of(new McpSchema.TextContent("工具执行完成")), null);
			}
		}
		catch (Exception e) {
			// 如果 execute 方法不存在，尝试其他常见的方法名
			try {
				Method runMethod = toolBean.getClass().getMethod("run", Map.class);
				Object result = runMethod.invoke(toolBean, request.arguments());

				if (result != null) {
					String resultStr = result instanceof String ? (String) result
							: objectMapper.writeValueAsString(result);
					return new CallToolResult(List.of(new McpSchema.TextContent(resultStr)), null);
				}
				else {
					return new CallToolResult(List.of(new McpSchema.TextContent("工具执行完成")), null);
				}
			}
			catch (Exception e2) {
				log.error("调用通用工具失败: {}", e2.getMessage(), e2);
				return new CallToolResult(List.of(new McpSchema.TextContent("工具调用失败: " + e2.getMessage())), null);
			}
		}
	}

	/**
	 * 创建默认的 JSON Schema
	 */
	private String createDefaultSchema() {
		return """
				{
				"$schema": "http://json-schema.org/draft-07/schema#",
				"type": "object",
				"properties": {},
				"additionalProperties": true
				}
				""";
	}

}