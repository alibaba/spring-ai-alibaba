package com.alibaba.cloud.ai.example.manus.planning.service;

import com.alibaba.cloud.ai.example.manus.planning.model.vo.ParameterValidationResult;

import java.util.List;
import java.util.Map;

/**
 * 计划参数映射服务接口 提供处理计划模板中参数占位符的功能
 */
public interface IPlanParameterMappingService {

	/**
	 * 验证计划模板中的参数占位符是否都能在原始参数中找到
	 * @param planJson 计划模板的JSON字符串
	 * @param rawParams 原始参数字典
	 * @return 验证结果，包含缺失的参数列表
	 */
	ParameterValidationResult validateParameters(String planJson, Map<String, Object> rawParams);

	/**
	 * 提取计划模板中所有的参数占位符
	 * @param planJson 计划模板的JSON字符串
	 * @return 参数占位符列表
	 */
	List<String> extractParameterPlaceholders(String planJson);

}
