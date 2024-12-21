package com.alibaba.cloud.ai.service.generator;

import com.alibaba.cloud.ai.model.App;
import com.alibaba.cloud.ai.param.ProjectGenerateParam;
import com.alibaba.cloud.ai.model.workflow.Workflow;
import com.alibaba.cloud.ai.model.chatbot.ChatBot;
import com.alibaba.cloud.ai.model.AppMetadata;
import java.nio.file.Path;

/**
 * ProjectGenerator abstracts the project generation of a specific app type, e.g.
 * {@link Workflow}, {@link ChatBot}
 */
public interface ProjectGenerator {

	/**
	 * Whether the generator supports the given app mode
	 * @param appMode see `mode` in {@link AppMetadata}
	 * @return true if supported
	 */
	Boolean supportAppMode(String appMode);

	/**
	 * Generate the project, save into a local directory
	 * @param app {@link App}
	 * @param param see params in {@link ProjectGenerateParam}
	 * @return a local path
	 */
	Path generate(App app, ProjectGenerateParam param);

}
