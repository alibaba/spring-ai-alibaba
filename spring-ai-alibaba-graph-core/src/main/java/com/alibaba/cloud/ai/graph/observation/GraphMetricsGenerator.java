/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.observation;

import com.alibaba.cloud.ai.graph.observation.edge.GraphEdgeObservationContext;
import com.alibaba.cloud.ai.graph.observation.graph.GraphObservationContext;
import com.alibaba.cloud.ai.graph.observation.metric.SpringAiAlibabaObservationMetricAttributes;
import com.alibaba.cloud.ai.graph.observation.metric.SpringAiAlibabaObservationMetricNames;
import com.alibaba.cloud.ai.graph.observation.node.GraphNodeObservationContext;
import io.micrometer.common.KeyValue;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.observation.Observation;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for generating metrics from graph observation contexts. Creates counters
 * for graph nodes, edges, and overall graph operations. Supports success/failure tracking
 * and custom tagging for metric aggregation.
 *
 * @author XiaoYunTao
 * @since 2025/6/28
 */
public class GraphMetricsGenerator {

	private static final String DESCRIPTION = "Counts the number of times a graph is used";

	private GraphMetricsGenerator() {
	}

	/**
	 * Generates metrics for a graph node observation context. Creates a counter with
	 * node-specific tags and success/failure tracking.
	 * @param context the node observation context
	 * @param meterRegistry the meter registry for metric registration
	 * @param isSuccess whether the node operation was successful
	 */
	public static void generate(GraphNodeObservationContext context, MeterRegistry meterRegistry, boolean isSuccess) {
		Counter.builder(SpringAiAlibabaObservationMetricNames.GRAPH_NODE.value())
			.tag(SpringAiAlibabaObservationMetricAttributes.GRAPH_NODE_NAME.value(), context.getNodeName())
			.tag(SpringAiAlibabaObservationMetricAttributes.GRAPH_NODE_SUCCESS.value(), String.valueOf(isSuccess))
			.description(DESCRIPTION)
			.tags(createTags(context))
			.register(meterRegistry)
			.increment();
	}

	/**
	 * Generates metrics for a graph observation context. Creates a counter with
	 * graph-specific tags and success/failure tracking.
	 * @param context the graph observation context
	 * @param meterRegistry the meter registry for metric registration
	 * @param isSuccess whether the graph operation was successful
	 */
	public static void generate(GraphObservationContext context, MeterRegistry meterRegistry, boolean isSuccess) {
		Counter.builder(SpringAiAlibabaObservationMetricNames.GRAPH.value())
			.tag(SpringAiAlibabaObservationMetricAttributes.GRAPH_NAME.value(), context.getGraphName())
			.tag(SpringAiAlibabaObservationMetricAttributes.GRAPH_SUCCESS.value(), String.valueOf(isSuccess))
			.description(DESCRIPTION)
			.tags(createTags(context))
			.register(meterRegistry)
			.increment();
	}

	/**
	 * Generates metrics for a graph edge observation context. Creates a counter with
	 * edge-specific tags and success/failure tracking.
	 * @param context the edge observation context
	 * @param meterRegistry the meter registry for metric registration
	 * @param isSuccess whether the edge operation was successful
	 */
	public static void generate(GraphEdgeObservationContext context, MeterRegistry meterRegistry, boolean isSuccess) {
		Counter.builder(SpringAiAlibabaObservationMetricNames.GRAPH_EDGE.value())
			.tag(SpringAiAlibabaObservationMetricAttributes.GRAPH_EDGE_NAME.value(), context.getGraphEdgeName())
			.tag(SpringAiAlibabaObservationMetricAttributes.GRAPH_EDGE_SUCCESS.value(), String.valueOf(isSuccess))
			.description(DESCRIPTION)
			.tags(createTags(context))
			.register(meterRegistry)
			.increment();
	}

	/**
	 * Creates tags from the low cardinality key-values of an observation context.
	 * Converts KeyValue objects to Tag objects for metric registration.
	 * @param context the observation context containing key-values
	 * @return a list of tags created from the context's key-values
	 */
	private static List<Tag> createTags(Observation.Context context) {
		List<Tag> tags = new ArrayList<>();
		for (KeyValue keyValue : context.getLowCardinalityKeyValues()) {
			tags.add(Tag.of(keyValue.getKey(), keyValue.getValue()));
		}
		return tags;
	}

}
