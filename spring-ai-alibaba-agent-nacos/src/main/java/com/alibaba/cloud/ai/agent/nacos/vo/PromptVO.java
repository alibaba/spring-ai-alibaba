package com.alibaba.cloud.ai.agent.nacos.vo;

import java.util.List;

import lombok.Data;

@Data
public class PromptVO {

	String promptKey;

	String version;

	String template;

	List<String> variables;

}
