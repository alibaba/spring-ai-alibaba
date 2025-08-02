/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus.tool.pptGenerator;

import java.util.List;

public class PptInput {

	private String action;

	@com.fasterxml.jackson.annotation.JsonProperty("output_path")
	private String outputPath;

	private String title;

	private String subtitle;

	@com.fasterxml.jackson.annotation.JsonProperty("slide_contents")
	private List<SlideContent> slideContents;

	public static class SlideContent {

		private String title;

		private String content;

		@com.fasterxml.jackson.annotation.JsonProperty("image_path")
		private String imagePath;

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public String getImagePath() {
			return imagePath;
		}

		public void setImagePath(String imagePath) {
			this.imagePath = imagePath;
		}

	}

	public PptInput() {
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getOutputPath() {
		return outputPath;
	}

	public void setOutputPath(String outputPath) {
		this.outputPath = outputPath;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSubtitle() {
		return subtitle;
	}

	public void setSubtitle(String subtitle) {
		this.subtitle = subtitle;
	}

	public List<SlideContent> getSlideContents() {
		return slideContents;
	}

	public void setSlideContents(List<SlideContent> slideContents) {
		this.slideContents = slideContents;
	}

}
