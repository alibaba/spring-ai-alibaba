package com.alibaba.cloud.ai.studio.admin.observation;

import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationPredicate;
import org.springframework.stereotype.Component;

@Component
public class IgnoreStudioHttpSpanPredicate implements ObservationPredicate {

    /**
     * Ignore Http Span of Studio
     */
    @Override
    public boolean test(String name, Context context) {
        return !(context instanceof org.springframework.http.server.observation.ServerRequestObservationContext)
            && !(context instanceof org.springframework.http.server.reactive.observation.ServerRequestObservationContext);
    }
}
