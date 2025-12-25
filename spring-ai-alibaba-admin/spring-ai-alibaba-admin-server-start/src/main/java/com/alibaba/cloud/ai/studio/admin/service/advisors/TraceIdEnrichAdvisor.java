package com.alibaba.cloud.ai.studio.admin.service.advisors;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.handler.TracingObservationHandler;
import io.micrometer.tracing.handler.TracingObservationHandler.TracingContext;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.AdvisorUtils;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author zhuoguang
 */
public class TraceIdEnrichAdvisor implements BaseAdvisor {
    
    ObservationRegistry registry;
    
    public TraceIdEnrichAdvisor(ObservationRegistry registry) {
        this.registry = registry;
    }
    
    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        return chatClientRequest;
    }
    
    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse,
            AdvisorChain advisorChain) {
        return chatClientResponse;
    }
    
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
            StreamAdvisorChain streamAdvisorChain) {
        Assert.notNull(chatClientRequest, "chatClientRequest cannot be null");
        Assert.notNull(streamAdvisorChain, "streamAdvisorChain cannot be null");
        Assert.notNull(getScheduler(), "scheduler cannot be null");
        
        Flux<ChatClientResponse> chatClientResponseFlux = Mono.just(chatClientRequest)
                .publishOn(getScheduler())
                .map(request -> this.before(request, streamAdvisorChain))
                .flatMapMany(streamAdvisorChain::nextStream);
        
        return Flux.deferContextual((ctxView) -> {
            Observation observation = ctxView.getOrDefault(ObservationThreadLocalAccessor.KEY, null);
            String traceId;
            if (observation != null) {
                TracingObservationHandler.TracingContext traceContext = observation.getContext().getOrDefault(TracingContext.class, () -> null);
                traceId = traceContext.getSpan().context().traceId();
            } else {
                traceId = null;
            }
            return chatClientResponseFlux.map(response -> {
                if (AdvisorUtils.onFinishReason().test(response)) {
                    response = after(response, streamAdvisorChain);
                    response.context().put("traceId", traceId);
                }
                return response;
            }).onErrorResume(error -> Flux.error(new IllegalStateException("Stream processing failed", error)));
        });
    }
    
    @Override
    public int getOrder() {
        return 0;
    }
}
