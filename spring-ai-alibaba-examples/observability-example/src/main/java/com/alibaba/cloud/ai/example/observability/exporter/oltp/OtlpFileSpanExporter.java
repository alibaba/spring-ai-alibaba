/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.alibaba.cloud.ai.example.observability.exporter.oltp;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.SegmentedStringWriter;
import io.opentelemetry.exporter.internal.otlp.traces.ResourceSpansMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link SpanExporter} which writes {@linkplain SpanData spans} to a {@link Logger} in OTLP JSON
 * format. Each log line will include a single {@code ResourceSpans}.
 */
public final class OtlpFileSpanExporter implements SpanExporter {

  private static final Logger logger =
          Logger.getLogger(OtlpFileSpanExporter.class.getName());

  private final AtomicBoolean isShutdown = new AtomicBoolean();

  /** Returns a new {@link OtlpFileSpanExporter}. */
  public static SpanExporter create() {
    return new OtlpFileSpanExporter();
  }

  private OtlpFileSpanExporter() {}

  @Override
  public CompletableResultCode export(Collection<SpanData> spans) {
    if (isShutdown.get()) {
      return CompletableResultCode.ofFailure();
    }

    ResourceSpansMarshaler[] allResourceSpans = ResourceSpansMarshaler.create(spans);
    for (ResourceSpansMarshaler resourceSpans : allResourceSpans) {
      SegmentedStringWriter sw =
              new SegmentedStringWriter(JsonUtil.JSON_FACTORY._getBufferRecycler());
      try (JsonGenerator gen = JsonUtil.create(sw)) {
        resourceSpans.writeJsonTo(gen);
      } catch (IOException e) {
        // Shouldn't happen in practice, just skip it.
        continue;
      }
      try {
        logger.log(Level.INFO, sw.getAndClear());
      } catch (IOException e) {
        logger.log(Level.WARNING, "Unable to read OTLP JSON spans", e);
      }
    }
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    if (!isShutdown.compareAndSet(false, true)) {
      logger.log(Level.INFO, "Calling shutdown() multiple times.");
    }
    return CompletableResultCode.ofSuccess();
  }
}
