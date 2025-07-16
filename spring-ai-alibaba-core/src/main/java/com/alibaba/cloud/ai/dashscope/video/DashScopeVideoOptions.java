/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.dashscope.video;

import org.springframework.ai.model.ModelOptions;

/**
 * DashScope Video Generation Options.
 *
 * @author dashscope
 */
public class DashScopeVideoOptions implements ModelOptions {

	/**
	 * Default video model.
	 */
	public static final String DEFAULT_MODEL = "text2video-synthesis";

	private String model;

	private Integer width;

	private Integer height;

	private Integer duration;

	private Integer fps;

	private Long seed;

	private Integer numFrames;

	public DashScopeVideoOptions() {
	}

	public DashScopeVideoOptions(String model, Integer width, Integer height, Integer duration, Integer fps, Long seed,
			Integer numFrames) {
		this.model = model;
		this.width = width;
		this.height = height;
		this.duration = duration;
		this.fps = fps;
		this.seed = seed;
		this.numFrames = numFrames;
	}

	public String getModel() {
		return this.model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public Integer getWidth() {
		return this.width;
	}

	public void setWidth(Integer width) {
		this.width = width;
	}

	public Integer getHeight() {
		return this.height;
	}

	public void setHeight(Integer height) {
		this.height = height;
	}

	public Integer getDuration() {
		return this.duration;
	}

	public void setDuration(Integer duration) {
		this.duration = duration;
	}

	public Integer getFps() {
		return this.fps;
	}

	public void setFps(Integer fps) {
		this.fps = fps;
	}

	public Long getSeed() {
		return this.seed;
	}

	public void setSeed(Long seed) {
		this.seed = seed;
	}

	public Integer getNumFrames() {
		return this.numFrames;
	}

	public void setNumFrames(Integer numFrames) {
		this.numFrames = numFrames;
	}

	/**
	 * Builder for DashScopeVideoOptions.
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for DashScopeVideoOptions.
	 */
	public static class Builder {

		private String model = DEFAULT_MODEL;

		private Integer width;

		private Integer height;

		private Integer duration;

		private Integer fps;

		private Long seed;

		private Integer numFrames;

		public Builder withModel(String model) {
			this.model = model;
			return this;
		}

		public Builder withWidth(Integer width) {
			this.width = width;
			return this;
		}

		public Builder withHeight(Integer height) {
			this.height = height;
			return this;
		}

		public Builder withDuration(Integer duration) {
			this.duration = duration;
			return this;
		}

		public Builder withFps(Integer fps) {
			this.fps = fps;
			return this;
		}

		public Builder withSeed(Long seed) {
			this.seed = seed;
			return this;
		}

		public Builder withNumFrames(Integer numFrames) {
			this.numFrames = numFrames;
			return this;
		}

		public DashScopeVideoOptions build() {
			return new DashScopeVideoOptions(this.model, this.width, this.height, this.duration, this.fps, this.seed,
					this.numFrames);
		}

	}

}
