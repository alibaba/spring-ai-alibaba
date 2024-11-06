package dev.ai.alibaba.samples.adaptiverag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;

import java.time.Duration;

public final class ChromaStore {
    public static ChromaStore of(String openApiKey) {
        return new ChromaStore(openApiKey);
    }

    private final ChromaEmbeddingStore chroma = new ChromaEmbeddingStore(
            "http://localhost:8000",
            "rag-chroma",
            Duration.ofMinutes(2),
            true,
            true );
    private final OpenAiEmbeddingModel embeddingModel;

    private ChromaStore( String openApiKey ) {
        this.embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(openApiKey)
                .build();
    }

    public EmbeddingSearchResult<TextSegment> search(String query) {

        Embedding queryEmbedding = embeddingModel.embed(query).content();

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding( queryEmbedding )
                .maxResults( 1 )
                .minScore( 0.0 )
                .build();
        return chroma.search( searchRequest );

    }
}
