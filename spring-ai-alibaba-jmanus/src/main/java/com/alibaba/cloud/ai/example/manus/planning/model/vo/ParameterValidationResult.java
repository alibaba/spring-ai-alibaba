package com.alibaba.cloud.ai.example.manus.planning.model.vo;

import java.util.List;

/**
 * 参数验证结果类 用于存储参数验证的结果信息
 */
public class ParameterValidationResult {

	/**
	 * 验证是否通过
	 */
	private boolean valid;

	/**
	 * 缺失的参数列表
	 */
	private List<String> missingParameters;

	/**
	 * 找到的参数列表
	 */
	private List<String> foundParameters;

	/**
	 * 验证结果消息
	 */
	private String message;

	/**
	 * 默认构造函数
	 */
	public ParameterValidationResult() {
		this.valid = false;
		this.missingParameters = new java.util.ArrayList<>();
		this.foundParameters = new java.util.ArrayList<>();
		this.message = "";
	}

	/**
	 * 带参数的构造函数
	 */
	public ParameterValidationResult(boolean valid, List<String> missingParameters, List<String> foundParameters,
			String message) {
		this.valid = valid;
		this.missingParameters = missingParameters != null ? missingParameters : new java.util.ArrayList<>();
		this.foundParameters = foundParameters != null ? foundParameters : new java.util.ArrayList<>();
		this.message = message != null ? message : "";
	}

	// Getters and Setters
	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public List<String> getMissingParameters() {
		return missingParameters;
	}

	public void setMissingParameters(List<String> missingParameters) {
		this.missingParameters = missingParameters;
	}

	public List<String> getFoundParameters() {
		return foundParameters;
	}

	public void setFoundParameters(List<String> foundParameters) {
		this.foundParameters = foundParameters;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * 添加缺失的参数
	 */
	public void addMissingParameter(String parameter) {
		if (this.missingParameters == null) {
			this.missingParameters = new java.util.ArrayList<>();
		}
		this.missingParameters.add(parameter);
	}

	/**
	 * 添加找到的参数
	 */
	public void addFoundParameter(String parameter) {
		if (this.foundParameters == null) {
			this.foundParameters = new java.util.ArrayList<>();
		}
		this.foundParameters.add(parameter);
	}

	/**
	 * 检查是否有关键参数缺失
	 */
	public boolean hasCriticalMissingParameters() {
		return missingParameters != null && !missingParameters.isEmpty();
	}

	/**
	 * 获取缺失参数的数量
	 */
	public int getMissingParameterCount() {
		return missingParameters != null ? missingParameters.size() : 0;
	}

	/**
	 * 获取找到参数的数量
	 */
	public int getFoundParameterCount() {
		return foundParameters != null ? foundParameters.size() : 0;
	}

	@Override
	public String toString() {
		return "ParameterValidationResult{" + "valid=" + valid + ", missingParameters=" + missingParameters
				+ ", foundParameters=" + foundParameters + ", message='" + message + '\'' + '}';
	}

}
