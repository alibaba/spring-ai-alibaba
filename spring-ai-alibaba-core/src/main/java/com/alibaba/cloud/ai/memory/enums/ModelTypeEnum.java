package com.alibaba.cloud.ai.memory.enums;

import com.alibaba.cloud.ai.memory.constant.MemoryConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Title model type enumeration.<br>
 * Description configure different model type enumerations.<br>
 *
 * @author zhych1005
 * @since 1.0.0-M3
 */

@Getter
@AllArgsConstructor
public enum ModelTypeEnum {

	/**
	 * qwen model
	 */
	QWEN_MAX("qwen-max", MemoryConstant.QWEN, 6 * 1024),
	QWEN_MAX_LONGCONTEXT("qwen-max-longcontext", MemoryConstant.QWEN, 28 * 1024),
	QWEN_PLUS("qwen-plus", MemoryConstant.QWEN, 128 * 1024), QWEN_TURBO("qwen-turbo", MemoryConstant.QWEN, 6 * 1024),
	GPT35_TURBO("gpt-3.5-turbo", MemoryConstant.OPENAI, 6 * 1024), GPT_4("gpt-4", MemoryConstant.OPENAI, 6 * 1024),
	GPT_4O_MINI("gpt-4o-mini", MemoryConstant.OPENAI, 6 * 1024);

	private final String modelName;

	private final String manufacturer;

	private final int contextWindowSize;

	/**
	 * 通过modelName获取上下文窗口大小
	 * @param modelName 模型名称
	 * @return 上下文窗口大小
	 */
	public static int getContextWindowSize(String modelName) {
		for (ModelTypeEnum modelTypeEnum : ModelTypeEnum.values()) {
			if (modelTypeEnum.getModelName().equals(modelName)) {
				return modelTypeEnum.getContextWindowSize();
			}
		}
		throw new IllegalArgumentException("Invalid model name: " + modelName);
	}

	/**
	 * 通过modelName获取模型厂商
	 * @param modelName 模型名称
	 * @return 模型厂商
	 */
	public static String getManufacturer(String modelName) {
		for (ModelTypeEnum modelTypeEnum : ModelTypeEnum.values()) {
			if (modelTypeEnum.getModelName().equals(modelName)) {
				return modelTypeEnum.getManufacturer();
			}
		}
		throw new IllegalArgumentException("Invalid model name: " + modelName);
	}

}