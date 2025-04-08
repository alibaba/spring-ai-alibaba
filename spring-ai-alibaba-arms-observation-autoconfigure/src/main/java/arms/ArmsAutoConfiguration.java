package arms;

import com.alibaba.cloud.ai.tool.ObservableToolCallingManager;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(ChatModel.class)
@EnableConfigurationProperties(ArmsCommonProperties.class)
public class ArmsAutoConfiguration {

  @Bean
  @ConditionalOnProperty(
      prefix = ArmsCommonProperties.CONFIG_PREFIX,
      name = "enabled",
      havingValue = "true")
  ToolCallingManager toolCallingManager(ToolCallbackResolver toolCallbackResolver,
      ToolExecutionExceptionProcessor toolExecutionExceptionProcessor,
      ObjectProvider<ObservationRegistry> observationRegistry) {
    return ObservableToolCallingManager.builder()
        .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
        .toolCallbackResolver(toolCallbackResolver)
        .toolExecutionExceptionProcessor(toolExecutionExceptionProcessor)
        .build();
  }
}
