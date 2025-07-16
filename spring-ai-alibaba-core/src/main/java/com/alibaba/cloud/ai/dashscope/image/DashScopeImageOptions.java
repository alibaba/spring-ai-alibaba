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
package com.alibaba.cloud.ai.dashscope.image;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.ai.image.ImageOptions;

import java.util.Arrays;

/**
 * @author nuocheng.lxm
 * @author yuluo
 * @author Polaris
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
	@JsonProperty("width")
	private Integer width;

	/**
	 * The height of the generated images. Must be one of 720, 1024, 1280
	 */
	@JsonProperty("height")
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
	 * The format in which the generated images are returned. Must be one of url or
	 * b64_json.
	 */
	@JsonProperty("response_format")
	private String responseFormat;

	/**
	 * refer mode,Must be one of repaint,refonly
	 */
	@JsonProperty("ref_mode")
	private String refMode;

	@JsonProperty("negative_prompt")
	private String negativePrompt;

	@JsonProperty("prompt_extend")
	private Boolean promptExtend;

	@JsonProperty("watermark")
	private Boolean watermark;

	@JsonProperty("function")
	private String function;

	@JsonProperty("base_image_url")
	private String baseImageUrl;

	@JsonProperty("mask_image_url")
	private String maskImageUrl;

	@JsonProperty("sketch_image_url")
	private String sketchImageUrl;

	@JsonProperty("sketch_weight")
	private Integer sketchWeight;

	@JsonProperty("sketch_extraction")
	private Boolean sketchExtraction;

	@JsonProperty("sketch_color")
	private Integer[][] sketchColor;

	@JsonProperty("mask_color")
	private Integer[][] maskColor;

	public Boolean getPromptExtend() {
		return promptExtend;
	}

	public void setPromptExtend(Boolean promptExtend) {
		this.promptExtend = promptExtend;
	}

	public Boolean getWatermark() {
		return watermark;
	}

	public void setWatermark(Boolean watermark) {
		this.watermark = watermark;
	}

	public String getFunction() {
		return function;
	}

	public void setFunction(String function) {
		this.function = function;
	}

	public String getBaseImageUrl() {
		return baseImageUrl;
	}

	public void setBaseImageUrl(String baseImageUrl) {
		this.baseImageUrl = baseImageUrl;
	}

	public String getMaskImageUrl() {
		return maskImageUrl;
	}

	public void setMaskImageUrl(String maskImageUrl) {
		this.maskImageUrl = maskImageUrl;
	}

	public String getSketchImageUrl() {
		return sketchImageUrl;
	}

	public void setSketchImageUrl(String sketchImageUrl) {
		this.sketchImageUrl = sketchImageUrl;
	}

	public Integer getSketchWeight() {
		return sketchWeight;
	}

	public void setSketchWeight(Integer sketchWeight) {
		this.sketchWeight = sketchWeight;
	}

	public Boolean getSketchExtraction() {
		return sketchExtraction;
	}

	public void setSketchExtraction(Boolean sketchExtraction) {
		this.sketchExtraction = sketchExtraction;
	}

	public Integer[][] getSketchColor() {
		return sketchColor;
	}

	public void setSketchColor(Integer[][] sketchColor) {
		this.sketchColor = sketchColor;
	}

	public Integer[][] getMaskColor() {
		return maskColor;
	}

	public void setMaskColor(Integer[][] maskColor) {
		this.maskColor = maskColor;
	}

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
		return this.responseFormat;
	}

	@Override
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

	@Deprecated
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
				+ negativePrompt + '\'' + ", promptExtend=" + promptExtend + ", watermark=" + watermark + ", function='"
				+ function + '\'' + ", baseImageUrl='" + baseImageUrl + '\'' + ", maskImageUrl='" + maskImageUrl + '\''
				+ ", sketchImageUrl='" + sketchImageUrl + '\'' + ", sketchWeight=" + sketchWeight
				+ ", sketchExtraction=" + sketchExtraction + ", sketchColor=" + Arrays.toString(sketchColor)
				+ ", maskColor=" + Arrays.toString(maskColor) + '}';
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

		@Deprecated
		public Builder withSize(String size) {
			options.setSize(size);
			return this;
		}

		public Builder withNegativePrompt(String negativePrompt) {
			options.setNegativePrompt(negativePrompt);
			return this;
		}

		public Builder withPromptExtend(Boolean promptExtend) {
			this.options.promptExtend = promptExtend;
			return this;
		}

		public Builder withWatermark(Boolean watermark) {
			this.options.watermark = watermark;
			return this;
		}

		public Builder withFunction(String function) {
			this.options.function = function;
			return this;
		}

		public Builder withBaseImageUrl(String baseImageUrl) {
			this.options.baseImageUrl = baseImageUrl;
			return this;
		}

		public Builder withMaskImageUrl(String maskImageUrl) {
			this.options.maskImageUrl = maskImageUrl;
			return this;
		}

		public Builder withSketchImageUrl(String sketchImageUrl) {
			this.options.sketchImageUrl = sketchImageUrl;
			return this;
		}

		public Builder withSketchWeight(Integer sketchWeight) {
			this.options.sketchWeight = sketchWeight;
			return this;
		}

		public Builder withSketchExtraction(Boolean sketchExtraction) {
			this.options.sketchExtraction = sketchExtraction;
			return this;
		}

		public Builder withSketchColor(Integer[][] sketchColor) {
			this.options.sketchColor = sketchColor;
			return this;
		}

		public Builder withMaskColor(Integer[][] maskColor) {
			this.options.maskColor = maskColor;
			return this;
		}

		public Builder withResponseFormat(String responseFormat) {
			this.options.responseFormat = responseFormat;
			return this;
		}

		public DashScopeImageOptions build() {
			return options;
		}

	}

}
