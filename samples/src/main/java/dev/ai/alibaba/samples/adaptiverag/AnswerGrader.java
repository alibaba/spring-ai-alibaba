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
public class AnswerGrader implements Function<AnswerGrader.Arguments,AnswerGrader.Score> {
    /**
     * Binary score to assess answer addresses question.
     */
    public static class Score {

        @Description("Answer addresses the question, 'yes' or 'no'")
        public String binaryScore;
    }

    @StructuredPrompt("User question: \n\n {{question}} \n\n LLM generation: {{generation}}")
    @Value(staticConstructor="of")
    public static class Arguments {
        String question;
        String generation;
    }

    interface Service {

        
        @SystemMessage( "You are a grader assessing whether an answer addresses and/or resolves a question. \n\n" + 
                        "Give a binary score 'yes' or 'no'. Yes, means that the answer resolves the question otherwise return 'no'")
        Score invoke(String userMessage);
    }

    String openApiKey;

    @Override
    public Score apply(Arguments args) {
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
