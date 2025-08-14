package com.alibaba.cloud.ai.studio.core.model.llm.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Configuration information for a model
 */
@Data
public class ModelConfigInfo implements Serializable {

	/** Model icon */
	private String icon;

	/** Unique identifier for the model */
	private String modelId;

	/** Model provider */
	private String provider;

	/** Model name */
	private String name;

	/** Model tags */
	private List<String> tags;

	/** Whether the model is enabled */
	private Boolean enable;

	/** Source type: custom or preset */
	private String source;

	/** Model type */
	private String type = ModelTypeEnum.llm.name();

	/** Model mode: chat or completion */
	private String mode;

	/** Model modes */
	public enum ModeEnum {

		chat, completion

	}

	/** Model types */
	public enum ModelTypeEnum {

		llm, rerank, text_embedding, tts, stt

	}

}
