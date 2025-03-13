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
package com.alibaba.cloud.ai.reader.arxiv.client;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * arXiv查询结果中的一个条目
 *
 * @see <a href=
 * "https://arxiv.org/help/api/user-manual#_details_of_atom_results_returned">arXiv API
 * User's Manual: Details of Atom Results Returned</a>
 * @author brianxiadong
 */
public class ArxivResult {

	private String entryId; // URL in the format https://arxiv.org/abs/{id}

	private LocalDateTime updated; // Last update time

	private LocalDateTime published; // Initial publication time

	private String title; // Title

	private List<ArxivAuthor> authors; // List of authors

	private String summary; // Abstract

	private String comment; // Author comments (optional)

	private String journalRef; // Journal reference (optional)

	private String doi; // DOI link (optional)

	private String primaryCategory; // Primary category

	private List<String> categories; // All categories

	private List<ArxivLink> links; // Related links (up to 3)

	private String pdfUrl; // PDF link (if available)

	// Getters and Setters
	public String getEntryId() {
		return entryId;
	}

	public void setEntryId(String entryId) {
		this.entryId = entryId;
	}

	public LocalDateTime getUpdated() {
		return updated;
	}

	public void setUpdated(LocalDateTime updated) {
		this.updated = updated;
	}

	public LocalDateTime getPublished() {
		return published;
	}

	public void setPublished(LocalDateTime published) {
		this.published = published;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public List<ArxivAuthor> getAuthors() {
		return authors;
	}

	public void setAuthors(List<ArxivAuthor> authors) {
		this.authors = authors;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getJournalRef() {
		return journalRef;
	}

	public void setJournalRef(String journalRef) {
		this.journalRef = journalRef;
	}

	public String getDoi() {
		return doi;
	}

	public void setDoi(String doi) {
		this.doi = doi;
	}

	public String getPrimaryCategory() {
		return primaryCategory;
	}

	public void setPrimaryCategory(String primaryCategory) {
		this.primaryCategory = primaryCategory;
	}

	public List<String> getCategories() {
		return categories;
	}

	public void setCategories(List<String> categories) {
		this.categories = categories;
	}

	public List<ArxivLink> getLinks() {
		return links;
	}

	public void setLinks(List<ArxivLink> links) {
		this.links = links;
		// 设置PDF URL
		this.pdfUrl = links.stream()
			.filter(link -> "pdf".equals(link.getTitle()))
			.findFirst()
			.map(ArxivLink::getHref)
			.orElse(null);
	}

	public String getPdfUrl() {
		return pdfUrl;
	}

	/**
	 * Get article's short ID Examples: - For URL "https://arxiv.org/abs/2107.05580v1"
	 * returns "2107.05580v1" - For URL "https://arxiv.org/abs/quant-ph/0201082v1" returns
	 * "quant-ph/0201082v1"
	 */
	public String getShortId() {
		return entryId.split("arxiv.org/abs/")[1];
	}

	/**
	 * 生成默认的文件名
	 */
	public String getDefaultFilename(String extension) {
		String nonEmptyTitle = title != null && !title.isEmpty() ? title : "UNTITLED";
		return String.format("%s.%s.%s", getShortId().replace("/", "_"),
				Pattern.compile("[^\\w]").matcher(nonEmptyTitle).replaceAll("_"), extension);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ArxivResult result = (ArxivResult) o;
		return Objects.equals(entryId, result.entryId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(entryId);
	}

	/**
	 * 表示文章作者的内部类
	 */
	public static class ArxivAuthor {

		private String name;

		public ArxivAuthor(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			ArxivAuthor author = (ArxivAuthor) o;
			return Objects.equals(name, author.name);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name);
		}

		@Override
		public String toString() {
			return name;
		}

	}

	/**
	 * 表示相关链接的内部类
	 */
	public static class ArxivLink {

		private String href; // Link URL

		private String title; // Link title

		private String rel; // Relationship between link and Result

		private String contentType; // HTTP content type

		public ArxivLink(String href, String title, String rel, String contentType) {
			this.href = href;
			this.title = title;
			this.rel = rel;
			this.contentType = contentType;
		}

		public String getHref() {
			return href;
		}

		public void setHref(String href) {
			this.href = href;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getRel() {
			return rel;
		}

		public void setRel(String rel) {
			this.rel = rel;
		}

		public String getTextType() {
			return contentType;
		}

		public void setContentType(String contentType) {
			this.contentType = contentType;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			ArxivLink link = (ArxivLink) o;
			return Objects.equals(href, link.href);
		}

		@Override
		public int hashCode() {
			return Objects.hash(href);
		}

		@Override
		public String toString() {
			return href;
		}

	}

}
