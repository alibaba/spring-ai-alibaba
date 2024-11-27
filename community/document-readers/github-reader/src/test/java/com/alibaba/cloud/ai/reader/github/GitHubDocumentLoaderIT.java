package com.alibaba.cloud.ai.reader.github;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.TextReader;

import java.util.List;

@EnabledIfEnvironmentVariable(named = "GITHUB_TOKEN", matches = ".+")
class GitHubDocumentLoaderIT {

	private static final String TEST_OWNER = "sincerity-being";

	private static final String TEST_REPO = "llm-course";

	GitHubDocumentReader reader;

	GitHubSource source = GitHubSource.builder()
		.gitHubToken(System.getenv("GITHUB_TOKEN"))
		.owner(TEST_OWNER)
		.repo(TEST_REPO)
		.branch("main")
		.path("Mergekit.ipynb") // Mergekit.ipynb //LICENSE
		.build();

	DocumentReader parser = new TextReader(source);

	@BeforeEach
	public void beforeEach() {
		reader = new GitHubDocumentReader(source.getContent(), parser);
	}

	@Test
	public void should_load_file() {
		List<Document> document = reader.get();
		String content = document.get(0).getContent();
		System.out.println(System.getenv("GITHUB_TOKEN"));
		System.out.println(content);
		// assertThat(content).contains("<groupId>com.alibaba.cloud</groupId>");
	}

}
