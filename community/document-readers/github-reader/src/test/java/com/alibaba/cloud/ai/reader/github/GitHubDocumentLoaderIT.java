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
