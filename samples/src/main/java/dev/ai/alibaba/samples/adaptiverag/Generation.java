package dev.ai.alibaba.samples.adaptiverag;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import lombok.Value;

import java.time.Duration;
import java.util.List;
import java.util.function.BiFunction;


@Value(staticConstructor="of")
public class Generation implements BiFunction<String, List<String>, String> {

    public interface Service {

        @UserMessage("You are an assistant for question-answering tasks. Use the following pieces of retrieved context to answer the question. If you don't know the answer, just say that you don't know. Use three sentences maximum and keep the answer concise.\n" +
                "Question: {{question}} \n" +
                "Context: {{context}} \n" +
                "Answer:")
        String invoke(@V("question") String question, @V("context") List<String> context );
    }

    String openApiKey;

    public String apply( String question, List<String> context) {

        ChatLanguageModel chatLanguageModel = OpenAiChatModel.builder()
                .apiKey( openApiKey )
                .modelName( "gpt-3.5-turbo" )
                .timeout(Duration.ofMinutes(2))
                .logRequests(true)
                .logResponses(true)
                .maxRetries(2)
                .temperature(0.0)
                .maxTokens(2000)
                .build();

        Service service = AiServices.create(Service.class, chatLanguageModel);

        return service.invoke( question, context ); // service
    }

}
