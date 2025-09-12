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
package com.alibaba.cloud.ai.manus.tool.browser;

import com.microsoft.playwright.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Utility class specialized in handling interactive text elements in web pages. Provides
 * functionality for finding, analyzing and processing text content in web pages. Uses
 * InteractiveElementRegistry to manage all interactive elements in the page, providing
 * global index access capability.
 */
public class InteractiveTextProcessor {

	private static final Logger log = LoggerFactory.getLogger(InteractiveTextProcessor.class);

	/**
	 * Store and manage interactive elements in the page
	 */
	private final InteractiveElementRegistry elementRegistry;

	/**
	 * Constructor
	 */
	public InteractiveTextProcessor() {
		this.elementRegistry = new InteractiveElementRegistry();
	}

	/**
	 * Refresh all interactive elements in the page, including those in iframes
	 * @param page Page to process
	 */
	public void refreshCache(Page page) {
		// Use registry to refresh page elements
		elementRegistry.refresh(page);
		log.info("Refreshed page elements, found {} interactive elements", elementRegistry.size());
	}

	/**
	 * Get interactive element at specified index
	 * @param index Global index
	 * @return Interactive element corresponding to this index, returns empty if not
	 * exists
	 */
	public Optional<InteractiveElement> getElementByIndex(int index) {
		return elementRegistry.getElementById(index);
	}

	/**
	 * Get list of all interactive elements
	 * @return List of interactive elements
	 */
	public List<InteractiveElement> getAllElements(Page page) {
		return elementRegistry.getAllElements(page);
	}

	/**
	 * Click element at specified index
	 * @param index Element global index
	 * @return Whether operation was successful
	 */
	public boolean clickElement(int index) {
		return elementRegistry.performAction(index, element -> {
			element.getLocator().click();
			log.info("Clicked element at index {}: {}", index, element.toString());
		});
	}

	/**
	 * Fill text in input element at specified index
	 * @param index Element global index
	 * @param text Text to fill
	 * @return Whether operation was successful
	 */
	public boolean fillText(int index, String text) {
		return elementRegistry.performAction(index, element -> {
			element.getLocator().fill(text);
			log.info("Filled text in element at index {}: {}", index, text);
		});
	}

	/**
	 * Get detailed information of all interactive elements in the web page
	 * @return Formatted element information string
	 */
	public String getInteractiveElementsInfo(Page page) {
		return elementRegistry.generateElementsInfoText(page);
	}

	/**
	 * Get total number of interactive elements
	 * @return Element count
	 */
	public int getElementCount() {
		return elementRegistry.size();
	}

	/**
	 * Execute custom operation based on element index
	 * @param index Element index
	 * @param action Operation to execute
	 * @return Whether operation was successful
	 */
	public boolean performAction(int index, InteractiveElementRegistry.ElementAction action) {
		return elementRegistry.performAction(index, action);
	}

}
