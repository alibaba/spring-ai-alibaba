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
package com.alibaba.cloud.ai.example.manus.tool.browser;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * 浏览器工具请求对象
 * 用于封装浏览器操作的请求参数
 */
public class BrowserRequestVO {
    
    /**
     * 浏览器操作类型
     * 支持: navigate, click, input_text, key_enter, screenshot, get_html, get_text,
     * execute_js, scroll, switch_tab, new_tab, close_tab, refresh
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
     * 滚动像素，用于scroll操作
     * 正数向下滚动，负数向上滚动
     */
    @JSONField(name = "scroll_amount")
    private Integer scrollAmount;
    
    /**
     * 标签页ID，用于switch_tab操作
     */
    @JSONField(name = "tab_id")
    private Integer tabId;
    
    // Getters and Setters
    
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
    
    @Override
    public String toString() {
        return "BrowserRequestVO{" +
                "action='" + action + '\'' +
                ", url='" + url + '\'' +
                ", index=" + index +
                ", text='" + text + '\'' +
                ", script='" + script + '\'' +
                ", scrollAmount=" + scrollAmount +
                ", tabId=" + tabId +
                '}';
    }
}
    
    // Getters and Setters
    
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
    
    @Override
    public String toString() {
        return "BrowserRequestVO{" +
                "action='" + action + '\'' +
                ", url='" + url + '\'' +
                ", index=" + index +
                ", text='" + text + '\'' +
                ", script='" + script + '\'' +
                ", scrollAmount=" + scrollAmount +
                ", tabId=" + tabId +
                '}';
    }
}
