package com.alibaba.cloud.ai.manus.planning.service;

import com.alibaba.cloud.ai.manus.planning.exception.ParameterValidationException;
import com.alibaba.cloud.ai.manus.planning.model.vo.ParameterValidationResult;

import java.util.List;
import java.util.Map;

/**
 * 计划参数映射服务接口 提供处理计划模板中参数占位符的功能
 */
public interface IPlanParameterMappingService {

	/**
	 * 验证计划模板中的参数占位符是否都能在原始参数中找到 如果验证失败，抛出详细的异常信息
	 * @param planJson 计划模板的JSON字符串
	 * @param rawParams 原始参数字典
	 * @return 验证结果，包含缺失的参数列表
	 * @throws ParameterValidationException 当参数验证失败时抛出
	 */
	ParameterValidationResult validateParameters(String planJson, Map<String, Object> rawParams)
			throws ParameterValidationException;

	/**
	 * 提取计划模板中所有的参数占位符
	 * @param planJson 计划模板的JSON字符串
	 * @return 参数占位符列表
	 */
	List<String> extractParameterPlaceholders(String planJson);

	/**
	 * Replace parameters in JSON and return the replaced plan JSON If validation fails,
	 * throws detailed exception information
	 * @param planJson Plan template JSON string
	 * @param rawParams Raw parameters dictionary
	 * @return Replaced plan JSON string
	 * @throws ParameterValidationException when parameter validation fails
	 */
	String replaceParametersInJson(String planJson, Map<String, Object> rawParams) throws ParameterValidationException;

	/**
	 * 在参数替换之前验证参数完整性 如果验证失败，抛出详细的异常信息
	 * @param planJson 计划模板JSON
	 * @param rawParams 原始参数
	 * @throws ParameterValidationException 当参数验证失败时抛出
	 */
	void validateParametersBeforeReplacement(String planJson, Map<String, Object> rawParams)
			throws ParameterValidationException;

	/**
	 * 安全地替换参数，如果验证失败则抛出异常
	 * @param planJson 计划模板JSON
	 * @param rawParams 原始参数
	 * @return 替换后的计划模板
	 * @throws ParameterValidationException 当参数验证失败时抛出
	 */
	String replaceParametersSafely(String planJson, Map<String, Object> rawParams) throws ParameterValidationException;

	/**
	 * 获取计划模板的参数要求信息 帮助用户了解需要提供哪些参数
	 * @param planJson 计划模板JSON
	 * @return 参数要求信息
	 */
	String getParameterRequirements(String planJson);

}
