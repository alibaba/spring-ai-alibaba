package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.state.NodeState;

import static java.lang.String.format;
import static com.alibaba.cloud.ai.graph.StateGraph.START;

public abstract class DiagramGenerator {

	protected abstract void appendHeader(StringBuilder sb, String title);

	protected abstract void appendFooter(StringBuilder sb);

	protected abstract void start(StringBuilder sb, String entryPoint);

	protected abstract void finish(StringBuilder sb, String finishPoint);

	protected abstract void finish(StringBuilder sb, String finishPoint, String description);

	protected abstract void call(StringBuilder sb, String from, String to);

	protected abstract void call(StringBuilder sb, String from, String to, String description);

	protected abstract void declareConditionalStart(StringBuilder sb, String name);

	protected abstract void declareNode(StringBuilder sb, String name);

	protected abstract void declareConditionalEdge(StringBuilder sb, int ordinal);

	protected abstract StringBuilder commentLine(StringBuilder sb, boolean yesOrNo);

	public final <State extends NodeState> String generate(StateGraph<State> compiledGraph, String title,
			boolean printConditionalEdge) {
		StringBuilder sb = new StringBuilder();

		appendHeader(sb, title);

		compiledGraph.nodes.forEach(s -> declareNode(sb, s.id()));

		final int[] conditionalEdgeCount = { 0 };

		compiledGraph.edges.forEach(e -> {
			if (e.target().value() != null) {
				conditionalEdgeCount[0] += 1;
				declareConditionalEdge(commentLine(sb, !printConditionalEdge), conditionalEdgeCount[0]);
			}
		});

		EdgeValue<State> entryPoint = compiledGraph.getEntryPoint();
		if (entryPoint.id() != null) {
			start(sb, entryPoint.id());
		}
		else if (entryPoint.value() != null) {
			String conditionName = "startcondition";
			declareConditionalStart(commentLine(sb, !printConditionalEdge), conditionName);
			edgeCondition(sb, entryPoint.value(), START, conditionName, printConditionalEdge);
		}

		conditionalEdgeCount[0] = 0; // reset

		compiledGraph.edges.forEach(v -> {
			if (v.target().id() != null) {
				call(sb, v.sourceId(), v.target().id());
			}
			else if (v.target().value() != null) {
				conditionalEdgeCount[0] += 1;
				String conditionName = format("condition%d", conditionalEdgeCount[0]);

				edgeCondition(sb, v.target().value(), v.sourceId(), conditionName, printConditionalEdge);

			}
		});
		if (compiledGraph.getFinishPoint() != null) {
			finish(sb, compiledGraph.getFinishPoint());
		}
		appendFooter(sb);

		return sb.toString();

	}

	private <State extends NodeState> void edgeCondition(StringBuilder sb, EdgeCondition<State> condition, String k,
			String conditionName, boolean printConditionalEdge) {
		call(commentLine(sb, !printConditionalEdge), k, conditionName);

		condition.mappings().forEach((cond, to) -> {
			if (to.equals(StateGraph.END)) {

				finish(commentLine(sb, !printConditionalEdge), conditionName, cond);
				finish(commentLine(sb, printConditionalEdge), k, cond);

			}
			else {
				call(commentLine(sb, !printConditionalEdge), conditionName, to, cond);
				call(commentLine(sb, printConditionalEdge), k, to, cond);
			}
		});
	}

}
