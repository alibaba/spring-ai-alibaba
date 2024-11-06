package dev.ai.alibaba.samples.adaptiverag;

import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import dev.langchain4j.rag.content.Content;
import lombok.Value;

import java.util.List;
import java.util.function.Function;

@Value( staticConstructor = "of" )
public class WebSearchTool implements Function<String, List<Content>> {
    String tavilyApiKey;

    @Override
    public List<Content> apply(String query) {
        WebSearchEngine webSearchEngine = TavilyWebSearchEngine.builder()
                .apiKey(tavilyApiKey) // get a free key: https://app.tavily.com/sign-in
                .build();

        ContentRetriever webSearchContentRetriever = WebSearchContentRetriever.builder()
                .webSearchEngine(webSearchEngine)
                .maxResults(3)
                .build();

        return webSearchContentRetriever.retrieve( new Query( query ) );
    }
}
