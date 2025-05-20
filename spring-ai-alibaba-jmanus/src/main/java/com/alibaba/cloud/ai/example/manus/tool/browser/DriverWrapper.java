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

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

public class DriverWrapper {

	private Playwright playwright;

	private Page currentPage;

	private Browser browser;

	private InteractiveElementRegistry interactiveElementRegistry;

	public DriverWrapper(Playwright playwright, Browser browser, Page currentPage) {
		this.playwright = playwright;
		this.currentPage = currentPage;
		this.browser = browser;
		this.interactiveElementRegistry = new InteractiveElementRegistry();
	}

	public InteractiveElementRegistry getInteractiveElementRegistry() {
		return interactiveElementRegistry;
	}

	public Playwright getPlaywright() {
		return playwright;
	}

	public void setPlaywright(Playwright playwright) {
		this.playwright = playwright;
	}

	public Page getCurrentPage() {
		return currentPage;
	}

	public void setCurrentPage(Page currentPage) {
		this.currentPage = currentPage;
	}

	public Browser getBrowser() {
		return browser;
	}

	public void setBrowser(Browser browser) {
		this.browser = browser;
	}

	public void close() {
		if (this.currentPage != null) {
			this.currentPage.close();
		}
		if (this.browser != null) {
			this.browser.close();
		}
	}

}
