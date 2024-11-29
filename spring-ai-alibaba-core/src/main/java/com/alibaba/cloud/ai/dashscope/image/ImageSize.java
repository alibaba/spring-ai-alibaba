package com.alibaba.cloud.ai.dashscope.image;

public class ImageSize {

	private Integer width;

	private Integer height;

	public ImageSize() {
	}

	public ImageSize(Integer width, Integer height) {
		this.width = width;
		this.height = height;
	}

	public Integer getWidth() {
		return width;
	}

	public void setWidth(Integer width) {
		this.width = width;
	}

	public Integer getHeight() {
		return height;
	}

	public void setHeight(Integer height) {
		this.height = height;
	}

}
