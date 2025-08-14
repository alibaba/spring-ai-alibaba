package com.alibaba.cloud.ai.studio.runtime.domain.workflow.inner;

import com.alibaba.cloud.ai.studio.runtime.domain.workflow.CommonParam;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 异常处理配置
 *
 * @since 1.0.0-beta
 */
@Data
public class TryCatchConfig implements Serializable {

	/**
	 * @see StrategyEnum
	 */
	@JsonProperty("strategy")
	private String strategy;

	@JsonProperty("default_values")
	private List<CommonParam> defaultValues;

	public enum StrategyEnum {

		noop, defaultValue, failBranch

	}

}
