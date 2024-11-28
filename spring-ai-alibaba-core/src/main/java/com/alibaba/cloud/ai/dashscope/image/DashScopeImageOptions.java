/*
 * Copyright 2023-2024 the original author or authors.
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

package com.alibaba.cloud.ai.dashscope.image;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.ai.image.ImageOptions;

/**
 * @author nuocheng.lxm
 * @author yuluo
 * @since 2024/8/16 11:29
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashScopeImageOptions implements ImageOptions {

	/**
	 * The model to use for image generation.
	 */
	@JsonProperty("model")
	private String model;

	/**
	 * The number of images to generate. Must be between 1 and 4.
	 */
	@JsonProperty("n")
	private Integer n;

	/**
	 * The width of the generated images. Must be one of 720, 1024, 1280
	 */
	@JsonProperty("size_width")
	private Integer width;

	/**
	 * The height of the generated images. Must be one of 720, 1024, 1280
	 */
	@JsonProperty("size_height")
	private Integer height;

	/**
	 * The size of the generated images. Must be one of 1024*1024, 720*1280, 1280*720
	 */
	@JsonProperty("size")
	private String size;

	/**
	 * The style of the generated images.Must be one of <photography>,<portrait>,<3d
	 * cartoon>,<anime>, <oil painting>,<watercolor>,<sketch>,<chinese painting> <flat
	 * illustration>,<auto>
	 */
	@JsonProperty("style")
	private String style;

	/**
	 * Sets the random number seed to use for generation. Must be between 0 and
	 * 4294967290.
	 *
	 */
	@JsonProperty("seed")
	private Integer seed;

	/**
	 * refer image,Support jpg, png, tiff, webp
	 */
	@JsonProperty("ref_img")
	private String refImg;

	/**
	 * refer strength,Must be between 0.0 and 1.0
	 */
	@JsonProperty("ref_strength")
	private Float refStrength;

	/**
	 * refer mode,Must be one of repaint,refonly
	 */
	@JsonProperty("ref_mode")
	private String refMode;

	@JsonProperty("negative_prompt")
	private String negativePrompt;

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public Integer getN() {
		return this.n;
	}

	public void setN(Integer n) {
		this.n = n;
	}

	@Override
	public String getModel() {
		return this.model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	@Override
	public Integer getWidth() {
		return this.width;
	}

	public void setWidth(Integer width) {
		this.width = width;
		this.size = this.width + "*" + this.height;
	}

	@Override
	public Integer getHeight() {
		return this.height;
	}

	public void setHeight(Integer height) {
		this.height = height;
		this.size = this.width + "*" + this.height;
	}

	@Override
	public String getResponseFormat() {
		return null;
	}

	public String getStyle() {
		return this.style;
	}

	public void setStyle(String style) {
		this.style = style;
	}

	public String getSize() {

		if (this.size != null) {
			return this.size;
		}
		return (this.width != null && this.height != null) ? this.width + "*" + this.height : null;
	}

	public void setSize(String size) {
		this.size = size;
	}

	public Integer getSeed() {
		return seed;
	}

	public void setSeed(Integer seed) {
		this.seed = seed;
	}

	public String getRefImg() {
		return refImg;
	}

	public void setRefImg(String refImg) {
		this.refImg = refImg;
	}

	public Float getRefStrength() {
		return refStrength;
	}

	public void setRefStrength(Float refStrength) {
		this.refStrength = refStrength;
	}

	public String getRefMode() {
		return refMode;
	}

	public void setRefMode(String refMode) {
		this.refMode = refMode;
	}

	public String getNegativePrompt() {
		return negativePrompt;
	}

	public void setNegativePrompt(String negativePrompt) {
		this.negativePrompt = negativePrompt;
	}

	@Override
	public String toString() {

		return "DashScopeImageOptions{" + "model='" + model + '\'' + ", n=" + n + ", width=" + width + ", height="
				+ height + ", size='" + size + '\'' + ", style='" + style + '\'' + ", seed=" + seed + ", refImg='"
				+ refImg + '\'' + ", refStrength=" + refStrength + ", refMode='" + refMode + '\'' + ", negativePrompt='"
				+ negativePrompt + '\'' + '}';
	}

	public static class Builder {

		private final DashScopeImageOptions options;

		private Builder() {
			this.options = new DashScopeImageOptions();
		}

		public Builder withN(Integer n) {
			options.setN(n);
			return this;
		}

		public Builder withModel(String model) {
			options.setModel(model);
			return this;
		}

		public Builder withWidth(Integer width) {
			options.setWidth(width);
			return this;
		}

		public Builder withHeight(Integer height) {
			options.setHeight(height);
			return this;
		}

		public Builder withStyle(String style) {
			options.setStyle(style);
			return this;
		}

		public Builder withSeed(Integer seed) {
			options.setSeed(seed);
			return this;
		}

		public Builder withRefImg(String refImg) {
			options.setRefImg(refImg);
			return this;
		}

		public Builder withRefStrength(Float refStrength) {
			options.setRefStrength(refStrength);
			return this;
		}

		public Builder withRefMode(String refMode) {
			options.setRefMode(refMode);
			return this;
		}

		public Builder withNegativePrompt(String negativePrompt) {
			options.setNegativePrompt(negativePrompt);
			return this;
		}

		public DashScopeImageOptions build() {
			return options;
		}

	}

}
