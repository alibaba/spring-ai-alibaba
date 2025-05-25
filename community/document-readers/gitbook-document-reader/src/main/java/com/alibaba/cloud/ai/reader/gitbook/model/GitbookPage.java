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
package com.alibaba.cloud.ai.reader.gitbook.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents a page in Gitbook. Contains all the metadata and content information for a
 * single page.
 *
 * @author brianxiadong
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitbookPage {

	/**
	 * The unique identifier of the page
	 */
	private String id;

	/**
	 * The title/name of the page
	 */
	private String title;

	/**
	 * The URL path to access the page
	 */
	private String path;

	/**
	 * The type of the page (e.g. "document", "group")
	 */
	private String type;

	/**
	 * A brief description of the page content
	 */
	private String description;

	/**
	 * The ID of the parent page if this is a sub-page
	 */
	private String parent;

	/**
	 * List of child pages under this page
	 */
	@JsonProperty("pages")
	private List<GitbookPage> subPages;

	// Getters and Setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public List<GitbookPage> getSubPages() {
		return subPages;
	}

	public void setSubPages(List<GitbookPage> subPages) {
		this.subPages = subPages;
	}

	/**
	 * Checks if this page is a document type.
	 * @return true if the page is a document, false otherwise
	 */
	public boolean isDocument() {
		return "document".equals(type);
	}

	/**
	 * Checks if this page has any sub-pages.
	 * @return true if the page has sub-pages, false otherwise
	 */
	public boolean hasSubPages() {
		return subPages != null && !subPages.isEmpty();
	}

}
