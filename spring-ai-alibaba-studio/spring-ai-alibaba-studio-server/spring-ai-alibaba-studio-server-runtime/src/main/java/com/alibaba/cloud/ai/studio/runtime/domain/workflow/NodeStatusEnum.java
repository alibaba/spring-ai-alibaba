package com.alibaba.cloud.ai.studio.runtime.domain.workflow;

import lombok.Getter;

@Getter
public enum NodeStatusEnum {

	SUCCESS("success", "成功"), FAIL("fail", "失败"), SKIP("skip", "跳过"), EXECUTING("executing", "执行中"),
	PAUSE("pause", "暂停"), STOP("stop", "停止"),;

	NodeStatusEnum(String code, String desc) {
		this.code = code;
		this.desc = desc;
	}

	private final String code;

	private final String desc;

}
