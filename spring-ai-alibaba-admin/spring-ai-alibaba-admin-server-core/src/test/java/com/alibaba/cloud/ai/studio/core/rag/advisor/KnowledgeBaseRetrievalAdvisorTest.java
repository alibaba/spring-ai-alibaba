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

package com.alibaba.cloud.ai.studio.core.rag.advisor;

import com.alibaba.cloud.ai.studio.core.agent.AgentContext;
import com.alibaba.cloud.ai.studio.core.config.CommonConfig;
import com.alibaba.cloud.ai.studio.runtime.domain.app.AgentConfig;
import com.alibaba.cloud.ai.studio.runtime.domain.app.FileSearchOptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import reactor.core.scheduler.Schedulers;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static com.alibaba.cloud.ai.studio.core.rag.RagConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KnowledgeBaseRetrievalAdvisorTest {

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void shouldAugmentAnIndependentPromptWithoutMutatingTheCaller(boolean mutable) {
		List<Document> retrieved = List.of(Document.builder().text("Current lesson experiment").build());
		DocumentRetriever retriever = mock(DocumentRetriever.class);
		when(retriever.retrieve(any())).thenReturn(retrieved);
		AgentConfig config = new AgentConfig();
		config.setFileSearch(new FileSearchOptions());
		AgentContext context = new AgentContext();
		context.setConfig(config);
		KnowledgeBaseRetrievalAdvisor advisor = KnowledgeBaseRetrievalAdvisor.builder()
			.documentRetriever(retriever)
			.commonConfig(new CommonConfig())
			.agentContext(context)
			.scheduler(Schedulers.immediate())
			.build();
		List<Message> original = List.of(new SystemMessage("Use these references: {documents}"),
				new UserMessage("Explain the experiment"));
		Prompt prompt = new Prompt(mutable ? new ArrayList<>(original) : original);
		ChatClientRequest request = ChatClientRequest.builder()
			.prompt(prompt)
			.context(Map.of("trace", "lesson-request"))
			.build();
		ChatClientRequest advised = advisor.before(request, null);
		assertThat(advised.prompt().getSystemMessage().getText()).contains("Current lesson experiment");
		assertThat(advised.prompt().getUserMessage().getText()).isEqualTo("Explain the experiment");
		assertThat(advised.prompt().getInstructions()).hasSize(2);
		assertThat(advised.context()).containsEntry(FILE_SEARCH_RESULT, retrieved)
			.containsEntry("trace", "lesson-request");
		assertThat(request.prompt().getInstructions()).containsExactlyElementsOf(original);
		assertThat(request.prompt().getSystemMessage().getText()).isEqualTo("Use these references: {documents}");
	}

}
