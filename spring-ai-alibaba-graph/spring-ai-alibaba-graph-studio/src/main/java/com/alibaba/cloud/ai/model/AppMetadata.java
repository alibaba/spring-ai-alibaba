package com.alibaba.cloud.ai.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AppMetadata {

	public static final String CHATBOT_MODE = "chatbot";

	public static final String WORKFLOW_MODE = "workflow";

	public static final String[] SUPPORT_MODES = { CHATBOT_MODE, WORKFLOW_MODE };

	private String id;

	private String name;

	private String description;

	private String mode;

}
