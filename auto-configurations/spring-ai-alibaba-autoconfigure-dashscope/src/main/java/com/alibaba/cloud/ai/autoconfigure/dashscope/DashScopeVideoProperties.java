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
package com.alibaba.cloud.ai.autoconfigure.dashscope;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DashScope Video Generation Properties.
 *
 * @author dashscope
 */
@ConfigurationProperties(prefix = "spring.ai.alibaba.dashscope.video")
public class DashScopeVideoProperties {

	/**
	 * Enable DashScope video generation.
	 */
	private boolean enabled = true;

	/**
	 * Video model name.
	 */
	private String model = "text2video-synthesis";

	/**
	 * Video width.
	 */
	private Integer width;

	/**
	 * Video height.
	 */
	private Integer height;

	/**
	 * Video duration in seconds.
	 */
	private Integer duration;

	/**
	 * Video frames per second.
	 */
	private Integer fps;

	/**
	 * Random seed for video generation.
	 */
	private Long seed;

	/**
	 * Number of frames.
	 */
	private Integer numFrames;

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
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

}
