package com.alibaba.cloud.ai.model;

import lombok.Data;

@Data
public class App {

	private AppMetadata metadata;

	private Object spec;

	public App(AppMetadata metadata, Object spec) {
		this.metadata = metadata;
		this.spec = spec;
	}

}
