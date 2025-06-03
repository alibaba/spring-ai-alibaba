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
package com.alibaba.cloud.ai.example.manus.tool.browser.actions;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 浏览器工具请求对象 用于封装浏览器操作的请求参数
 */
public class BrowserRequestVO {

	/**
	 * 浏览器操作类型 支持: navigate, click, input_text, key_enter, screenshot, get_html, get_text,
	 * execute_js, scroll, switch_tab, new_tab, close_tab, refresh, get_element_position,
	 * move_to_and_click
	 */
	private String action;

	/**
	 * URL地址，用于navigate和new_tab操作
	 */
	private String url;

	/**
	 * 元素索引，用于click、input_text和key_enter操作
	 */
	private Integer index;

	/**
	 * 要输入的文本，用于input_text操作
	 */
	private String text;

	/**
	 * JavaScript代码，用于execute_js操作
	 */
	private String script;

	/**
	 * 滚动像素，用于scroll操作 正数向下滚动，负数向上滚动
	 */
	@JsonProperty("scroll_amount")
	private Integer scrollAmount;

	/**
	 * 标签页ID，用于switch_tab操作
	 */
	@JsonProperty("tab_id")
	private Integer tabId;

	/**
	 * 元素名称，用于get_element_position操作
	 */
	@JsonProperty("element_name")
	private String elementName;

	/**
	 * X坐标，用于move_to_and_click操作
	 */
	@JsonProperty("position_x")
	private Double positionX;

	/**
	 * Y坐标，用于move_to_and_click操作
	 */
	@JsonProperty("position_y")
	private Double positionY;

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Integer getIndex() {
		return index;
	}

	public void setIndex(Integer index) {
		this.index = index;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

	public Integer getScrollAmount() {
		return scrollAmount;
	}

	public void setScrollAmount(Integer scrollAmount) {
		this.scrollAmount = scrollAmount;
	}

	public Integer getTabId() {
		return tabId;
	}

	public void setTabId(Integer tabId) {
		this.tabId = tabId;
	}

	public String getElementName() {
		return elementName;
	}

	public void setElementName(String elementName) {
		this.elementName = elementName;
	}

	public Double getPositionX() {
		return positionX;
	}

	public void setPositionX(Double positionX) {
		this.positionX = positionX;
	}

	public Double getPositionY() {
		return positionY;
	}

	public void setPositionY(Double positionY) {
		this.positionY = positionY;
	}

}
