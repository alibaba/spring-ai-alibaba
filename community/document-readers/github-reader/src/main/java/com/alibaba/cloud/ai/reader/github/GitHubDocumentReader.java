package com.alibaba.cloud.ai.reader.github;

import org.kohsuke.github.GHContent;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.ExtractedTextFormatter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.reader.DocumentParser;

/**
 * @author HeYQ
 * @since 1.0.0
 */
public class GitHubDocumentReader implements DocumentReader {

	private final DocumentReader parser;

	private final GitHubResource gitHubResource;

	public GitHubDocumentReader(GitHubResource gitHubResource, DocumentParser parserType) {
		this(gitHubResource, parserType.getParser(gitHubResource));
	}

	public GitHubDocumentReader(GitHubResource gitHubResource, DocumentParser parserType,
			ExtractedTextFormatter formatter) {
		this(gitHubResource, parserType.getParser(gitHubResource, formatter));
	}

	public GitHubDocumentReader(GitHubResource gitHubResource, DocumentReader parser) {
		this.gitHubResource = gitHubResource;
		this.parser = parser;
	}

	@Override
	public List<Document> get() {
		GHContent ghContent = gitHubResource.getContent();
		List<Document> documents = parser.get();
		for (Document document : documents) {
			Map<String, Object> metadata = document.getMetadata();
			metadata.put("github_git_url", ghContent.getGitUrl());
			try {
				metadata.put("github_download_url", ghContent.getDownloadUrl());
			}
			catch (IOException e) {
				// Ignore if download_url is not available
			}
			metadata.put("github_html_url", ghContent.getHtmlUrl());
			metadata.put("github_url", ghContent.getUrl());
			metadata.put("github_file_name", ghContent.getName());
			metadata.put("github_file_path", ghContent.getPath());
			metadata.put("github_file_sha", ghContent.getSha());
			metadata.put("github_file_size", Long.toString(ghContent.getSize()));
			metadata.put("github_file_encoding", ghContent.getEncoding());
		}
		return documents;
	}

}
