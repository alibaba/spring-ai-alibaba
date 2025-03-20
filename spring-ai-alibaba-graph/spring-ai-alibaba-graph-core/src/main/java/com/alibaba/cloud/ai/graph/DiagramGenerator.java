/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.graph;

import java.util.Objects;

import com.alibaba.cloud.ai.graph.internal.edge.EdgeCondition;
import com.alibaba.cloud.ai.graph.state.AgentState;

import static java.lang.String.format;
import static com.alibaba.cloud.ai.graph.StateGraph.START;

/**
 * Abstract class for diagram generation. This class provides a framework for generating
 * textual representations of graphs.
 */
public abstract class DiagramGenerator {

	public enum CallStyle {

		DEFAULT, START, END, CONDITIONAL, PARALLEL

	}

	public record Context(StringBuilder sb, String title, boolean printConditionalEdge, boolean isSubGraph) {

		static Builder builder() {
			return new Builder();
		}

		static public class Builder {

			String title;

			boolean printConditionalEdge;

			boolean IsSubGraph;

			private Builder() {
			}

			public Builder title(String title) {
				this.title = title;
				return this;
			}

			public Builder printConditionalEdge(boolean value) {
				this.printConditionalEdge = value;
				return this;
			}

			public Builder isSubGraph(boolean value) {
				this.IsSubGraph = value;
				return this;
			}

			public Context build() {
				return new Context(new StringBuilder(), title, printConditionalEdge, IsSubGraph);
			}

		}

		/**
		 * Converts a given title string to snake_case format by replacing all
		 * non-alphanumeric characters with underscores.
		 * @return the snake_case formatted string
		 */
		public String titleToSnakeCase() {
			return title.replaceAll("[^a-zA-Z0-9]", "_");
		}

		/**
		 * Returns a string representation of this object by returning the string built in
		 * {@link #sb}.
		 * @return a string representation of this object.
		 */
		@Override
		public String toString() {
			return sb.toString();
		}
	}

	/**
	 * Appends a header to the output based on the provided context.
	 * @param ctx The {@link Context} containing the information needed for appending the
	 * header.
	 */
	protected abstract void appendHeader(Context ctx);

	/**
	 * Appends a footer to the content.
	 * @param ctx Context object containing the necessary information.
	 */
	protected abstract void appendFooter(Context ctx);

	/**
	 * This method is an abstract method that must be implemented by subclasses. It is
	 * used to initiate a communication call between two parties identified by their phone
	 * numbers.
	 * @param ctx The current context in which the call is being made.
	 * @param from The phone number of the caller.
	 * @param to The phone number of the recipient.
	 */
	protected abstract void call(Context ctx, String from, String to, CallStyle style);

	/**
	 * Abstract method that must be implemented by subclasses to handle the logic of
	 * making a call.
	 * @param ctx The context in which the call is being made.
	 * @param from The phone number of the caller.
	 * @param to The phone number of the recipient.
	 * @param description A brief description of the call.
	 */
	protected abstract void call(Context ctx, String from, String to, String description, CallStyle style);

	/**
	 * Declares a conditional element in the configuration or template. This method is
	 * used to mark the start of a conditional section based on the provided {@code name}.
	 * It takes a {@code Context} object that may contain additional parameters necessary
	 * for the declaration, and a {@code name} which identifies the type or key associated
	 * with the conditional section.
	 * @param ctx The context containing contextual information needed for the
	 * declaration.
	 * @param name The name of the conditional section to be declared.
	 */
	protected abstract void declareConditionalStart(Context ctx, String name);

	/**
	 * Declares a node in the specified context with the given name.
	 * @param ctx the context in which to declare the node {@code @literal (not null)}
	 * @param name the name of the node to be declared
	 * {@code @literal (not null, not empty)}
	 */
	protected abstract void declareNode(Context ctx, String name);

	/**
	 * Declares a conditional edge in the context with a specified ordinal.
	 * @param ctx the context
	 * @param ordinal the ordinal value
	 */
	protected abstract void declareConditionalEdge(Context ctx, int ordinal);

	/**
	 * Comment a line in the given context.
	 * @param ctx The context in which the line is to be commented.
	 * @param yesOrNo Whether the line should be uncommented ({@literal true}) or
	 * commented ({@literal false}).
	 */
	protected abstract void commentLine(Context ctx, boolean yesOrNo);

	/**
	 * Generate a textual representation of the given graph.
	 * @param nodes the state graph nodes used to generate the context, which must not be
	 * null
	 * @param edges the state graph edges used to generate the context, which must not be
	 * null
	 * @param title The title of the graph.
	 * @param printConditionalEdge Whether to print the conditional edge condition.
	 * @return A string representation of the graph.
	 */
	public final String generate(StateGraph.Nodes nodes, StateGraph.Edges edges, String title,
			boolean printConditionalEdge) {

		return generate(nodes, edges,
				Context.builder().title(title).isSubGraph(false).printConditionalEdge(printConditionalEdge).build())
			.toString();

	}

	/**
	 * Generates a context based on the given state graph.
	 * @param nodes the state graph nodes used to generate the context, which must not be
	 * null
	 * @param edges the state graph edges used to generate the context, which must not be
	 * null
	 * @param ctx the initial context, which must not be null
	 * @return the generated context, which will not be null
	 */
	protected final Context generate(StateGraph.Nodes nodes, StateGraph.Edges edges, Context ctx) {

		appendHeader(ctx);

		for (var n : nodes.elements) {

			if (n instanceof SubGraphNode subGraphNode) {

				@SuppressWarnings("unchecked")
				var subGraph = (StateGraph) subGraphNode.subGraph();
				Context subgraphCtx = generate(subGraph.nodes, subGraph.edges,
						Context.builder()
							.title(n.id())
							.printConditionalEdge(ctx.printConditionalEdge)
							.isSubGraph(true)
							.build());
				ctx.sb().append(subgraphCtx);
			}
			else {
				declareNode(ctx, n.id());
			}
		}

		final int[] conditionalEdgeCount = { 0 };

		edges.elements.stream()
			.filter(e -> !Objects.equals(e.sourceId(), START))
			.filter(e -> !e.isParallel())
			.forEach(e -> {
				if (e.target().value() != null) {
					conditionalEdgeCount[0] += 1;
					commentLine(ctx, !ctx.printConditionalEdge());
					declareConditionalEdge(ctx, conditionalEdgeCount[0]);
				}
			});

		var edgeStart = edges.elements.stream()
			.filter(e -> Objects.equals(e.sourceId(), START))
			.findFirst()
			.orElseThrow();
		if (edgeStart.isParallel()) {
			edgeStart.targets().forEach(target -> {
				call(ctx, START, target.id(), CallStyle.START);
			});
		}
		else if (edgeStart.target().id() != null) {
			call(ctx, START, edgeStart.target().id(), CallStyle.START);
		}
		else if (edgeStart.target().value() != null) {
			String conditionName = "startcondition";
			commentLine(ctx, !ctx.printConditionalEdge());
			declareConditionalStart(ctx, conditionName);
			edgeCondition(ctx, edgeStart.target().value(), START, conditionName);
		}

		conditionalEdgeCount[0] = 0; // reset

		edges.elements.stream().filter(e -> !Objects.equals(e.sourceId(), START)).forEach(v -> {

			if (v.isParallel()) {
				v.targets().forEach(target -> {
					call(ctx, v.sourceId(), target.id(), CallStyle.PARALLEL);
				});
			}
			else if (v.target().id() != null) {
				call(ctx, v.sourceId(), v.target().id(), CallStyle.DEFAULT);
			}
			else if (v.target().value() != null) {
				conditionalEdgeCount[0] += 1;
				String conditionName = format("condition%d", conditionalEdgeCount[0]);

				edgeCondition(ctx, v.targets().get(0).value(), v.sourceId(), conditionName);
			}
		});

		appendFooter(ctx);

		return ctx;

	}

	/**
	 * Evaluates an edge condition based on the given context and condition.
	 * @param ctx the current context used for evaluation
	 * @param condition the condition to be evaluated
	 * @param k a string identifier for the condition
	 * @param conditionName the name of the condition being processed
	 */
	private void edgeCondition(Context ctx, EdgeCondition condition, String k, String conditionName) {
		commentLine(ctx, !ctx.printConditionalEdge());
		call(ctx, k, conditionName, CallStyle.CONDITIONAL);

		condition.mappings().forEach((cond, to) -> {
			commentLine(ctx, !ctx.printConditionalEdge());
			call(ctx, conditionName, to, cond, CallStyle.CONDITIONAL);
			commentLine(ctx, ctx.printConditionalEdge());
			call(ctx, k, to, cond, CallStyle.CONDITIONAL);
		});
	}

}