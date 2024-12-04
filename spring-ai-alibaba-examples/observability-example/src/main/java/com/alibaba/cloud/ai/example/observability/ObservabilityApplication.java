package com.alibaba.cloud.ai.example.observability;

import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeChatProperties;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import io.micrometer.observation.ObservationRegistry;
import io.opentelemetry.api.trace.Span;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@SpringBootApplication
public class ObservabilityApplication {

    public static void main(String[] args) {
        SpringApplication.run(ObservabilityApplication.class, args);
    }

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = DashScopeChatProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
            matchIfMissing = true)
    public DashScopeChatModel dashscopeChatModel(DashScopeChatProperties chatProperties, List<FunctionCallback> toolFunctionCallbacks,
                                                 FunctionCallbackContext functionCallbackContext, RetryTemplate retryTemplate,
                                                 ObjectProvider<ObservationRegistry> observationRegistry, DashScopeApi dashScopeApi) {

        if (!CollectionUtils.isEmpty(toolFunctionCallbacks)) {
            chatProperties.getOptions().getFunctionCallbacks().addAll(toolFunctionCallbacks);
        }

        return new DashScopeChatModel(dashScopeApi, chatProperties.getOptions(), functionCallbackContext, retryTemplate,
                observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP));
    }
}

@Controller
@ResponseBody
class JokeController {

    private final ChatClient chatClient;

    JokeController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/joke")
    Map<String, String> joke() {
        var reply = chatClient
                .prompt()
                .user("""
                        tell me a joke. be concise. don't send anything except the joke.
                        """)
                .call()
                .content();
        Span currentSpan = Span.current();
        return Map.of("joke", reply, "traceId", currentSpan.getSpanContext().getTraceId());
    }
}
