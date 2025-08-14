package com.alibaba.cloud.ai.studio.runtime.domain.workflow.inner;

import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Node;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 短期记忆参数
 *
 * @since 1.0.0-beta
 */
@Data
public class ShortTermMemory implements Serializable {

	@JsonProperty("enabled")
	private Boolean enabled;

	/**
	 * @see TypeEnum
	 */
	@JsonProperty("type")
	private String type;

	// 轮次
	@JsonProperty("round")
	private Integer round;

	@JsonProperty("param")
	private Node.InputParam param;

	public enum TypeEnum {

		custom, self

	}

}
