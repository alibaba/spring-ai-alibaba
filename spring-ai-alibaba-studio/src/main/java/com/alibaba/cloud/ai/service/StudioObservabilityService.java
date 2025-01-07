package com.alibaba.cloud.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.opentelemetry.exporter.internal.otlp.traces.ResourceSpansMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.util.Collection;

/**
 * @Description:
 * @Author: 肖云涛
 * @Date: 2024/12/8
 */
public interface StudioObservabilityService {

	CompletableResultCode export(Collection<ResourceSpansMarshaler> allResourceSpans);

	ArrayNode getAITraceInfo();

	ArrayNode readObservabilityFile();

	JsonNode getTraceByTraceId(String traceId);

	String clearExportContent();

}
