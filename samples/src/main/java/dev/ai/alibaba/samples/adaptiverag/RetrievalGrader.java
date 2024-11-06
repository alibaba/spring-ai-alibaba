package dev.ai.alibaba.samples.adaptiverag;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.structured.StructuredPrompt;
import dev.langchain4j.model.input.structured.StructuredPromptProcessor;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import lombok.Value;

import java.time.Duration;
import java.util.function.Function;

@Value(staticConstructor="of")
public class RetrievalGrader implements Function<RetrievalGrader.Arguments, RetrievalGrader.Score> {

    public static class Score {

        @Description("Documents are relevant to the question, 'yes' or 'no'")
        public String binaryScore;
    }

    @StructuredPrompt("Retrieved document: \n\n {{document}} \n\n User question: {{question}}")
    @Value(staticConstructor = "of")
    public static class Arguments {
        String question;
        String document;
    }

    interface Service {

        @SystemMessage("You are a grader assessing relevance of a retrieved document to a user question. \n" +
                "    If the document contains keyword(s) or semantic meaning related to the user question, grade it as relevant. \n" +
                "    It does not need to be a stringent test. The goal is to filter out erroneous retrievals. \n" +
                "    Give a binary score 'yes' or 'no' score to indicate whether the document is relevant to the question.")
        Score invoke(String question);
    }

    String openApiKey;


    @Override
    public Score apply(Arguments args ) {

        ChatLanguageModel chatLanguageModel = OpenAiChatModel.builder()
                .apiKey( openApiKey )
                .modelName( "gpt-3.5-turbo-0125" )
                .timeout(Duration.ofMinutes(2))
                .logRequests(true)
                .logResponses(true)
                .maxRetries(2)
                .temperature(0.0)
                .maxTokens(2000)
                .build();


        Service service = AiServices.create(Service.class, chatLanguageModel);

        Prompt prompt = StructuredPromptProcessor.toPrompt(args);

        return service.invoke(prompt.text());

    }

}
