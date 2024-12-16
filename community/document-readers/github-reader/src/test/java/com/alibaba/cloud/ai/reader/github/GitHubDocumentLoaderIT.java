package com.alibaba.cloud.ai.reader.github;

import com.alibaba.cloud.ai.document.TextDocumentParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.document.Document;
import com.alibaba.cloud.ai.document.DocumentParser;
import java.util.List;

@EnabledIfEnvironmentVariable(named = "GITHUB_TOKEN", matches = ".+")
class GitHubDocumentLoaderIT {

	private static final String TEST_OWNER = "sincerity-being";

	private static final String TEST_REPO = "llm-course";

	GitHubDocumentReader reader;

	GitHubResource source = GitHubResource.builder()
		.gitHubToken(System.getenv("GITHUB_TOKEN"))
		.owner(TEST_OWNER)
		.repo(TEST_REPO)
		.branch("main")
		.path("Mergekit.ipynb") // Mergekit.ipynb //LICENSE
		.build();

	DocumentParser parser = new TextDocumentParser();

	@BeforeEach
	public void beforeEach() {
		reader = new GitHubDocumentReader(source, parser);
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
