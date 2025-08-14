package com.alibaba.cloud.ai.studio.runtime.domain.workflow;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * Edge of The Graph 画布的边声明
 *
 * @since 1.0.0-beta
 */
@Data
public class Edge implements Cloneable, Serializable {

	/**
	 * id of the edge 边的id
	 */
	private String id;

	/**
	 * source node id of the edge 边的源节点id
	 */
	private String source;

	/**
	 * If the source node has multiple outgoing connection points, sourceHandle is
	 * formatted as {source}_{connectionPointId};Otherwise (if there’s only one connection
	 * point), sourceHandle simply uses {source}.
	 * 边的源节点存在多个出向连接点，sourceHandle则格式化为{source}_{连接点的id}；否则sourceHandle为{source}
	 */
	@JsonProperty("source_handle")
	private String sourceHandle;

	/**
	 * target node id of the edge 边的目标节点id
	 */
	private String target;

	/**
	 * If the target node has multiple incoming connection points, targetHandle is
	 * formatted as {target}_{connectionPointId};Otherwise (if there’s only one connection
	 * point), targetHandle simply uses {target}.
	 * 边的目的节点存在多个入向连接点，targetHandle则格式化为{target}_{连接点的id}；否则targetHandle则格式化为为{target}
	 */
	@JsonProperty("target_handle")
	private String targetHandle;

	@Override
	public Object clone() {
		try {
			return super.clone();
		}
		catch (CloneNotSupportedException e) {
			// shouldn't happen as we are Cloneable
			throw new InternalError();
		}
	}

}
