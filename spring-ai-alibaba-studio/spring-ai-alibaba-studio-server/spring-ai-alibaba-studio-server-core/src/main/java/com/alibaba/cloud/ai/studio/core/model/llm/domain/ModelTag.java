package com.alibaba.cloud.ai.studio.core.model.llm.domain;

/**
 * Enum representing different capabilities of LLM models.
 */
public enum ModelTag {

	/** Vision capability for image processing */
	vision("视觉"),
	/** Web search capability for internet access */
	web_search("联网"),
	/** Embedding capability for vector representations */
	embedding("嵌入"),
	/** Reasoning capability for logical thinking */
	reasoning("推理"),
	/** Function call capability for tool usage */
	function_call("工具调用");

	/** Display name in Chinese */
	private final String displayName;

	ModelTag(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * Gets the Chinese display name of the model tag.
	 * @return the display name
	 */
	public String getDisplayName() {
		return this.displayName;
	}

}
