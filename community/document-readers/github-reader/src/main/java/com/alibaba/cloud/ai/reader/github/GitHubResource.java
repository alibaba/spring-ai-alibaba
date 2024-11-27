package com.alibaba.cloud.ai.reader.github;

import org.kohsuke.github.GHContent;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

/**
 * @author HeYQ
 * @since 1.0.0
 */
public class GitHubResource implements Resource {

	private final InputStream inputStream;

	private final GHContent content;

	public GitHubResource(String gitHubToken, String gitHubTokenOrganization, String owner, String repo, String branch,
			String path) {
		this(null, gitHubToken, gitHubTokenOrganization, owner, repo, branch, path);
	}

	public GitHubResource(String gitHubToken, String gitHubTokenOrganization, String owner, String repo, String path) {
		this(null, gitHubToken, gitHubTokenOrganization, owner, repo, "main", path);
	}

	public GitHubResource(String apiUrl, String gitHubToken, String gitHubTokenOrganization, String owner, String repo,
			String branch, String path) {
		GitHubBuilder gitHubBuilder = new GitHubBuilder();
		if (apiUrl != null) {
			gitHubBuilder.withEndpoint(apiUrl);
		}
		if (gitHubToken != null) {
			if (gitHubTokenOrganization == null) {
				gitHubBuilder.withOAuthToken(gitHubToken);
			}
			else {
				gitHubBuilder.withOAuthToken(gitHubToken, gitHubTokenOrganization);
			}
		}
		try {
			GitHub gitHub = gitHubBuilder.build();
			content = gitHub.getRepository(owner + "/" + repo).getFileContent(path, branch);
			Assert.isTrue(content.isFile(), "Path must be a file");
			inputStream = content.read();
		}
		catch (IOException ioException) {
			throw new RuntimeException(ioException);
		}
	}

	public GHContent getContent() {
		return content;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String apiUrl;

		private String gitHubToken;

		private String gitHubTokenOrganization;

		private String owner;

		private String repo;

		private String branch;

		private String path;

		public Builder apiUrl(String apiUrl) {
			this.apiUrl = apiUrl;
			return this;
		}

		public Builder gitHubToken(String gitHubToken) {
			this.gitHubToken = gitHubToken;
			return this;
		}

		public Builder gitHubTokenOrganization(String gitHubTokenOrganization) {
			this.gitHubTokenOrganization = gitHubTokenOrganization;
			return this;
		}

		public Builder owner(String owner) {
			this.owner = owner;
			return this;
		}

		public Builder repo(String repo) {
			this.repo = repo;
			return this;
		}

		public Builder branch(String branch) {
			this.branch = branch;
			return this;
		}

		public Builder path(String path) {
			this.path = path;
			return this;
		}

		public GitHubResource build() {
			Assert.notNull(owner, "Owner must not be null");
			Assert.notNull(repo, "Repo must not be null");
			Assert.notNull(path, "Path must not be null");
			return new GitHubResource(apiUrl, gitHubToken, gitHubTokenOrganization, owner, repo, branch, path);
		}

	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public URL getURL() throws IOException {
		return null;
	}

	@Override
	public URI getURI() throws IOException {
		return null;
	}

	@Override
	public File getFile() throws IOException {
		return null;
	}

	@Override
	public long contentLength() throws IOException {
		return 0;
	}

	@Override
	public long lastModified() throws IOException {
		return 0;
	}

	@Override
	public Resource createRelative(String relativePath) throws IOException {
		return null;
	}

	@Override
	public String getFilename() {
		return "";
	}

	@Override
	public String getDescription() {
		return "";
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return inputStream;
	}

}
