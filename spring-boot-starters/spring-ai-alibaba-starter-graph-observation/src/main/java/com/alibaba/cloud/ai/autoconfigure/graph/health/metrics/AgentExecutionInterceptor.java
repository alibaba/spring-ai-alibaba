package com.alibaba.cloud.ai.autoconfigure.graph.health.metrics;

import com.alibaba.cloud.ai.autoconfigure.graph.health.model.ExecutionContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.Usage;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;

/**
 * AOP interceptor for agent execution metrics.
 */
@Aspect
public class AgentExecutionInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AgentExecutionInterceptor.class);

    private final AgentMetrics agentMetrics;
    private final LlmMetrics llmMetrics;

    public AgentExecutionInterceptor(AgentMetrics agentMetrics, LlmMetrics llmMetrics) {
        this.agentMetrics = agentMetrics;
        this.llmMetrics = llmMetrics;
    }

    /**
     * Intercept ReactAgent.call() method.
     */
    @Around("execution(* com.alibaba.cloud.ai.graph.agent.ReactAgent.call(..))")
    public Object interceptReactAgentCall(ProceedingJoinPoint joinPoint) throws Throwable {
        Instant start = Instant.now();
        String agentName = extractAgentName(joinPoint);
        
        ExecutionContext.ExecutionStatus status = ExecutionContext.ExecutionStatus.SUCCESS;
        String errorMessage = null;

        try {
            Object result = joinPoint.proceed();
            
            // Try to extract LLM metrics from result
            if (result instanceof AssistantMessage) {
                recordLlmMetricsFromMessage((AssistantMessage) result, agentName);
            }
            
            return result;
            
        } catch (Exception e) {
            status = ExecutionContext.ExecutionStatus.FAILED;
            errorMessage = e.getMessage();
            log.warn("Agent execution failed: {}", agentName, e);
            throw e;
            
        } finally {
            Duration duration = Duration.between(start, Instant.now());
            
            ExecutionContext context = ExecutionContext.builder()
                .agentName(agentName)
                .agentType("ReactAgent")
                .duration(duration)
                .status(status)
                .errorMessage(errorMessage)
                .build();
                
            agentMetrics.recordExecution(context);
        }
    }

    /**
     * Intercept workflow agents (SequentialAgent, ParallelAgent, LoopAgent).
     */
    @Around("execution(* com.alibaba.cloud.ai.graph.agent.SequentialAgent.invoke(..)) || " +
            "execution(* com.alibaba.cloud.ai.graph.agent.ParallelAgent.invoke(..)) || " +
            "execution(* com.alibaba.cloud.ai.graph.agent.LoopAgent.invoke(..))")
    public Object interceptWorkflowAgent(ProceedingJoinPoint joinPoint) throws Throwable {
        Instant start = Instant.now();
        String agentName = extractAgentName(joinPoint);
        String agentType = joinPoint.getTarget().getClass().getSimpleName();
        
        ExecutionContext.ExecutionStatus status = ExecutionContext.ExecutionStatus.SUCCESS;

        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            status = ExecutionContext.ExecutionStatus.FAILED;
            throw e;
        } finally {
            Duration duration = Duration.between(start, Instant.now());
            
            ExecutionContext context = ExecutionContext.builder()
                .agentName(agentName)
                .agentType(agentType)
                .duration(duration)
                .status(status)
                .build();
                
            agentMetrics.recordExecution(context);
        }
    }

    /**
     * Extract agent name from the intercepted object.
     */
    private String extractAgentName(ProceedingJoinPoint joinPoint) {
        Object target = joinPoint.getTarget();
        
        try {
            Method getNameMethod = target.getClass().getMethod("getName");
            return (String) getNameMethod.invoke(target);
        } catch (Exception e) {
            log.debug("Could not extract agent name, using class name", e);
            return target.getClass().getSimpleName();
        }
    }

    /**
     * Extract and record LLM metrics from AssistantMessage.
     */
    private void recordLlmMetricsFromMessage(AssistantMessage message, String agentName) {
        try {
            // FIX: getMetadata() restituisce Map<String, Object>, non ChatResponseMetadata
            java.util.Map<String, Object> metadata = message.getMetadata();
            
            if (metadata == null || metadata.isEmpty()) {
                return;
            }

            // Recupera il modello dalla mappa (se presente)
            String modelName = (String) metadata.getOrDefault("model", "unknown-model");
            
            // In Spring AI, 'Usage' solitamente non è nei metadata del messaggio di default.
            // Tuttavia, se il framework Alibaba o il tuo codice lo hanno iniettato lì:
            Object usageObj = metadata.get("usage"); // "usage" è la chiave convenzionale
            
            // Verifica se l'oggetto recuperato è effettivamente di tipo Usage
            if (usageObj instanceof Usage) {
                Usage usage = (Usage) usageObj;
                
                long inputTokens = usage.getPromptTokens() != null ? usage.getPromptTokens() : 0L;
                long outputTokens = usage.getGenerationTokens() != null ? usage.getGenerationTokens() : 0L;
                
                llmMetrics.recordLlmCall(
                    modelName,
                    Duration.ZERO, // Duration not available from message alone
                    inputTokens,
                    outputTokens,
                    true
                );
            } else {
                log.debug("Usage object not found in message metadata for agent: {}", agentName);
            }
            
        } catch (Exception e) {
            log.debug("Could not extract LLM metrics from message", e);
        }
    }
}

