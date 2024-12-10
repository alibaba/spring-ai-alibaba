package com.alibaba.cloud.ai.model;

import com.alibaba.cloud.ai.service.runner.RunnableModel;
import lombok.Data;

/**
 * Unified app model definition.
 */
@Data
public class App implements RunnableModel {

	private AppMetadata metadata;

	/**
	 * Spec has different implementations depending on the type of application. e.g.
	 * Workflow
	 */
	private Object spec;

	public App(AppMetadata metadata, Object spec) {
		this.metadata = metadata;
		this.spec = spec;
	}

	@Override
	public String getId() {
		return metadata.getId();
	}

}
