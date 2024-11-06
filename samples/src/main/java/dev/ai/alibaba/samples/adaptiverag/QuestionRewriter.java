package dev.ai.alibaba.samples.adaptiverag;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import lombok.Value;

import java.time.Duration;
import java.util.function.Function;

import static com.alibaba.cloud.ai.graph.utils.CollectionsUtils.mapOf;


@Value(staticConstructor = "of")
public class QuestionRewriter implements Function<String, String> {

    private final String openApiKey;

    interface LLMService {

        @SystemMessage(
                "You a question re-writer that converts an input question to a better version that is optimized \n" +
                        "for vectorstore retrieval. Look at the input and try to reason about the underlying semantic intent / meaning.")
        String invoke(String question);
    }


//    private QuestionRewriter( String openApiKey ) {
//        this.openApiKey = openApiKey;
//    }

    @Override
    public String apply(String question) {

        ChatLanguageModel chatLanguageModel = OpenAiChatModel.builder()
                .apiKey(openApiKey)
                .modelName("gpt-3.5-turbo-0125")
                .timeout(Duration.ofMinutes(2))
                .logRequests(true)
                .logResponses(true)
                .maxRetries(2)
                .temperature(0.0)
                .maxTokens(2000)
                .build();


        LLMService service = AiServices.create(LLMService.class, chatLanguageModel);

        PromptTemplate template = PromptTemplate.from("Here is the initial question: \n\n {{question}} \n Formulate an improved question.");

        Prompt prompt = template.apply(mapOf("question", question));

        return service.invoke(prompt.text());
    }


}
