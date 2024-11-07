package com.alibaba.cloud.ai.dashscope.chat;

import com.alibaba.cloud.ai.dashscope.DashscopeAiTestConfiguration;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.observation.conventions.AiProvider;
import io.micrometer.common.KeyValue;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.observation.ChatModelObservationDocumentation;
import org.springframework.ai.chat.observation.DefaultChatModelObservationConvention;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.observation.conventions.AiOperationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for observation instrumentation in {@link DashScopeChatModel}
 *
 * @author Lumian
 * */
@SpringBootTest(classes = DashscopeAiTestConfiguration.class)
@EnabledIfEnvironmentVariable(named = "DASHSCOPE_API_KEY", matches = ".+")
public class DashScopeChatModelObservationIT {

    @Autowired
    TestObservationRegistry observationRegistry;

    @Autowired
    ChatModel dashscopeChatModel;

    @BeforeEach
    void beforeEach() {
        this.observationRegistry.clear();
    }

    @Test
    void observationForChatOperation() {

        var options = DashScopeChatOptions.builder()
                .withModel(DashScopeApi.ChatModel.QWEN_MAX.getModel())
                .withRepetitionPenalty(1.2)
                .withStop(List.of("this-is-the-end"))
                .withTemperature(0.75)
                .withTopP(0.95)
                .build();

        Prompt prompt = new Prompt("Why does a raven look like a desk?", options);

        ChatResponse chatResponse = this.dashscopeChatModel.call(prompt);
        assertThat(chatResponse.getResult().getOutput().getContent()).isNotEmpty();

        ChatResponseMetadata responseMetadata = chatResponse.getMetadata();
        assertThat(responseMetadata).isNotNull();

        validate(responseMetadata);
    }

    @Test
    void observationForStreamingChatOperation() {
        var options = DashScopeChatOptions.builder()
                .withModel(DashScopeApi.ChatModel.QWEN_MAX.getModel())
                .withRepetitionPenalty(1.2)
                .withStop(List.of("this-is-the-end"))
                .withTemperature(0.75)
                .withTopP(0.95)
                .withStream(true)
                .build();

        Prompt prompt = new Prompt("Why does a raven look like a desk?", options);

        Flux<ChatResponse> chatResponseFlux = this.dashscopeChatModel.stream(prompt);

        List<ChatResponse> responses = chatResponseFlux.collectList().block();
        assertThat(responses).isNotEmpty();
        assertThat(responses).hasSizeGreaterThan(10);

        String aggregatedResponse = responses.subList(0, responses.size() - 1)
                .stream()
                .map(r -> r.getResult().getOutput().getContent())
                .collect(Collectors.joining());
        assertThat(aggregatedResponse).isNotEmpty();

        ChatResponse lastChatResponse = responses.get(responses.size() - 1);

        ChatResponseMetadata responseMetadata = lastChatResponse.getMetadata();
        assertThat(responseMetadata).isNotNull();

        validate(responseMetadata);
    }

    private void validate(ChatResponseMetadata responseMetadata) {
        TestObservationRegistryAssert.assertThat(this.observationRegistry)
                .doesNotHaveAnyRemainingCurrentObservation()
                .hasObservationWithNameEqualTo(DefaultChatModelObservationConvention.DEFAULT_NAME)
                .that()
                .hasContextualNameEqualTo("chat " + DashScopeApi.ChatModel.QWEN_MAX.getModel())
                .hasLowCardinalityKeyValue(ChatModelObservationDocumentation.LowCardinalityKeyNames.AI_OPERATION_TYPE.asString(),
                        AiOperationType.CHAT.value())
                .hasLowCardinalityKeyValue(ChatModelObservationDocumentation.LowCardinalityKeyNames.AI_PROVIDER.asString(), AiProvider.DASHSCOPE.value())
                .hasLowCardinalityKeyValue(ChatModelObservationDocumentation.LowCardinalityKeyNames.REQUEST_MODEL.asString(),
                        DashScopeApi.ChatModel.QWEN_MAX.getModel())
                // TODO cannot parse response model from chat response metadata
                .hasLowCardinalityKeyValue(ChatModelObservationDocumentation.LowCardinalityKeyNames.RESPONSE_MODEL.asString(), KeyValue.NONE_VALUE)
                // TODO not support in dashscope yet
                .doesNotHaveHighCardinalityKeyValueWithKey(ChatModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_FREQUENCY_PENALTY.asString())
                .doesNotHaveHighCardinalityKeyValueWithKey(ChatModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_MAX_TOKENS.asString())
                .doesNotHaveHighCardinalityKeyValueWithKey(ChatModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_PRESENCE_PENALTY.asString())
                // FIXME stop sequences return null in options
                // .hasHighCardinalityKeyValue(ChatModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_STOP_SEQUENCES.asString(),
                //        "[\"this-is-the-end\"]")
                .doesNotHaveHighCardinalityKeyValueWithKey(ChatModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_STOP_SEQUENCES.asString())
                .hasHighCardinalityKeyValue(ChatModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_TEMPERATURE.asString(), "0.75")
                .doesNotHaveHighCardinalityKeyValueWithKey(ChatModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_TOP_K.asString())
                .hasHighCardinalityKeyValue(ChatModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_TOP_P.asString(), "0.95")
                .hasHighCardinalityKeyValue(ChatModelObservationDocumentation.HighCardinalityKeyNames.RESPONSE_ID.asString(), responseMetadata.getId())
                .hasHighCardinalityKeyValue(ChatModelObservationDocumentation.HighCardinalityKeyNames.RESPONSE_FINISH_REASONS.asString(), "[\"STOP\"]")
                .hasHighCardinalityKeyValue(ChatModelObservationDocumentation.HighCardinalityKeyNames.USAGE_INPUT_TOKENS.asString(),
                        String.valueOf(responseMetadata.getUsage().getPromptTokens()))
                .hasHighCardinalityKeyValue(ChatModelObservationDocumentation.HighCardinalityKeyNames.USAGE_OUTPUT_TOKENS.asString(),
                        String.valueOf(responseMetadata.getUsage().getGenerationTokens()))
                .hasHighCardinalityKeyValue(ChatModelObservationDocumentation.HighCardinalityKeyNames.USAGE_TOTAL_TOKENS.asString(),
                        String.valueOf(responseMetadata.getUsage().getTotalTokens()))
                .hasBeenStarted()
                .hasBeenStopped();
    }

}
