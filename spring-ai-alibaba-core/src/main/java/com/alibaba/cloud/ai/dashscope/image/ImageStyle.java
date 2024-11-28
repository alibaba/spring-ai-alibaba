package com.alibaba.cloud.ai.dashscope.image;

public class ImageStyle {

	private String style;

	private String refImg;

	private Float refStrength;

	private String refMode;

	public String getStyle() {
		return style;
	}

	public void setStyle(String style) {
		this.style = style;
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

}
