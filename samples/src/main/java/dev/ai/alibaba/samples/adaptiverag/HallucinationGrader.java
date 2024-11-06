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
import java.util.List;
import java.util.function.Function;

@Value(staticConstructor="of")
public class HallucinationGrader implements Function<HallucinationGrader.Arguments,HallucinationGrader.Score> {

    /**
     * Binary score for hallucination present in generation answer.
     */
    public static class Score {

        @Description("Answer is grounded in the facts, 'yes' or 'no'")
        public String binaryScore;
    }

    @StructuredPrompt("Set of facts: \\n\\n {{documents}} \\n\\n LLM generation: {{generation}}")
    @Value(staticConstructor = "of")
    public static class Arguments {
        List<String> documents;
        String generation;
    }

    interface Service {

        @SystemMessage(
                "You are a grader assessing whether an LLM generation is grounded in / supported by a set of retrieved facts. \n" +
                "Give a binary score 'yes' or 'no'. 'Yes' means that the answer is grounded in / supported by the set of facts.")
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

        Service grader = AiServices.create(Service.class, chatLanguageModel);

        Prompt prompt = StructuredPromptProcessor.toPrompt(args);

        return grader.invoke(prompt.text());

    }

}
