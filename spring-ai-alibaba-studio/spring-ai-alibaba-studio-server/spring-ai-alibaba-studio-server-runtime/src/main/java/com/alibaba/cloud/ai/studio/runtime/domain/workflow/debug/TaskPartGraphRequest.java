package com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug;

import com.alibaba.cloud.ai.studio.runtime.domain.workflow.CommonParam;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Edge;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Node;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 片段画布调试请求
 */
@Data
public class TaskPartGraphRequest implements Serializable {

	@JsonProperty("app_id")
	private String appId;

	private List<Node> nodes;

	private List<Edge> edges;

	/**
	 * 输入调试参数
	 */
	@JsonProperty("input_params")
	private List<CommonParam> inputParams;

}
