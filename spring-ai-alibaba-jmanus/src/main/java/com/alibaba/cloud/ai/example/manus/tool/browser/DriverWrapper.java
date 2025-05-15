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
