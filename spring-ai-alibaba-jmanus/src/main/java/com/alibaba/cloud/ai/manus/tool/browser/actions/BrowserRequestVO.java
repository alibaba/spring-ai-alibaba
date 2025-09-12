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
package com.alibaba.cloud.ai.manus.tool.browser.actions;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Browser tool request object for encapsulating browser operation request parameters
 */
public class BrowserRequestVO {

	/**
	 * Browser operation type Supports: navigate, click, input_text, key_enter,
	 * screenshot, get_html, get_text, execute_js, scroll, switch_tab, new_tab, close_tab,
	 * refresh, get_element_position, move_to_and_click
	 */
	private String action;

	/**
	 * URL address, used for navigate and new_tab operations
	 */
	private String url;

	/**
	 * Element index, used for click, input_text and key_enter operations
	 */
	private Integer index;

	/**
	 * Text to be input, used for input_text operations
	 */
	private String text;

	/**
	 * JavaScript code, used for execute_js operations
	 */
	private String script;

	/**
	 * Scroll pixels, used for scroll operations Positive scrolls down, negative scrolls
	 * up
	 */
	@JsonProperty("scroll_amount")
	private Integer scrollAmount;

	/**
	 * Tab ID, used for switch_tab operations
	 */
	@JsonProperty("tab_id")
	private Integer tabId;

	/**
	 * Element name, used for get_element_position operations
	 */
	@JsonProperty("element_name")
	private String elementName;

	/**
	 * X coordinate, used for move_to_and_click operations
	 */
	@JsonProperty("position_x")
	private Double positionX;

	/**
	 * Y coordinate, used for move_to_and_click operations
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
