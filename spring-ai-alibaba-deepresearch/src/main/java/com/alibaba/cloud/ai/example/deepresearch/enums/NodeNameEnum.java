package com.alibaba.cloud.ai.example.deepresearch.enums;

/**
 * StateGraph 节点枚举，统一管理所有流程节点名称。
 */
public enum NodeNameEnum {

	START("__START__", "开始"), END("__END__", "结束"), COORDINATOR("coordinator", ""),
	REWRITE_MULTI_QUERY("rewrite_multi_query", "查询问题相关信息"), BACKGROUND_INVESTIGATOR("background_investigator", "背景调查"),
	PLANNER("planner", "研究计划"), INFORMATION("information", ""), HUMAN_FEEDBACK("human_feedback", "人工反馈"),
	RESEARCH_TEAM("research_team", "等待并行节点执行完成"), PARALLEL_EXECUTOR("parallel_executor", ""),
	REPORTER("reporter", "报告生成"), RAG_NODE("rag_node", "");

	private final String nodeName;

	private final String displayTitle;

	NodeNameEnum(String nodeName, String displayTitle) {
		this.nodeName = nodeName;
		this.displayTitle = displayTitle;
	}

	public String nodeName() {
		return nodeName;
	}

	public String displayTitle() {
		return displayTitle;
	}

	public static String getDisplayTitleByNodeName(String nodeName) {
		for (NodeNameEnum n : values()) {
			if (n.nodeName.equals(nodeName)) {
				return n.displayTitle;
			}
		}
		return "";
	}

	public static NodeNameEnum fromNodeName(String nodeName) {
		for (NodeNameEnum n : values()) {
			if (n.nodeName.equals(nodeName)) {
				return n;
			}
		}
		return null;
	}

}
