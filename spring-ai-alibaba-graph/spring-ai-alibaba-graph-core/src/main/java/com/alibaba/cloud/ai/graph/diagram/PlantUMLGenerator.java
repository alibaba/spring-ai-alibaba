package com.alibaba.cloud.ai.graph.diagram;

import com.alibaba.cloud.ai.graph.DiagramGenerator;

import static java.lang.String.format;
import static com.alibaba.cloud.ai.graph.StateGraph.END;

public class PlantUMLGenerator extends DiagramGenerator {

	@Override
	protected void appendHeader(StringBuilder sb, String title) {
		sb.append("@startuml unnamed.puml\n")
			.append("skinparam usecaseFontSize 14\n")
			.append("skinparam usecaseStereotypeFontSize 12\n")
			.append("skinparam hexagonFontSize 14\n")
			.append("skinparam hexagonStereotypeFontSize 12\n")
			.append(format("title \"%s\"\n", title))
			.append("footer\n\n")
			.append("powered by langgraph4j\n")
			.append("end footer\n")
			.append("circle start<<input>>\n")
			.append(format("circle stop as %s\n", END));
	}

	@Override
	protected void appendFooter(StringBuilder sb) {
		sb.append("@enduml\n");
	}

	@Override
	protected void start(StringBuilder sb, String entryPoint) {
		sb.append(format("start -down-> \"%s\"\n", entryPoint));
	}

	@Override
	protected void finish(StringBuilder sb, String finishPoint) {
		sb.append(format("\"%s\" -down-> stop\n", finishPoint));
	}

	@Override
	protected void finish(StringBuilder sb, String finishPoint, String description) {
		sb.append(format("\"%s\" -down-> stop: \"%s\"\n", finishPoint, description));
	}

	@Override
	protected void call(StringBuilder sb, String from, String to) {
		sb.append(format("\"%s\" -down-> \"%s\"\n", from, to));
	}

	@Override
	protected void call(StringBuilder sb, String from, String to, String description) {
		sb.append(format("\"%s\" --> \"%s\": \"%s\"\n", from, to, description));
	}

	@Override
	protected void declareConditionalStart(StringBuilder sb, String name) {
		sb.append(format("hexagon \"check state\" as %s<<Condition>>\n", name));
	}

	@Override
	protected void declareNode(StringBuilder sb, String name) {
		sb.append(format("usecase \"%s\"<<AbstractNode>>\n", name));
	}

	@Override
	protected void declareConditionalEdge(StringBuilder sb, int ordinal) {
		sb.append(format("hexagon \"check state\" as condition%d<<Condition>>\n", ordinal));
	}

	@Override
	protected StringBuilder commentLine(StringBuilder sb, boolean yesOrNo) {
		return (yesOrNo) ? sb.append("'") : sb;
	}

}
