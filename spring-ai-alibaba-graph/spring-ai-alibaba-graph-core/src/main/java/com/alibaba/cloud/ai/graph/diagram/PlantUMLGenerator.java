package com.alibaba.cloud.ai.graph.diagram;

import com.alibaba.cloud.ai.graph.DiagramGenerator;

import static java.lang.String.format;
import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;

public class PlantUMLGenerator extends DiagramGenerator {

	@Override
	protected void appendHeader(Context ctx) {

		if (ctx.isSubGraph()) {
			ctx.sb()
				.append(format("rectangle %s [ {{\ntitle \"%s\"\n", ctx.title(), ctx.title()))
				.append(format("circle \" \" as %s\n", START))
				.append(format("circle exit as %s\n", END));
		}
		else {
			ctx.sb()
				.append(format("@startuml %s\n", ctx.titleToSnakeCase()))
				.append("skinparam usecaseFontSize 14\n")
				.append("skinparam usecaseStereotypeFontSize 12\n")
				.append("skinparam hexagonFontSize 14\n")
				.append("skinparam hexagonStereotypeFontSize 12\n")
				.append(format("title \"%s\"\n", ctx.title()))
				.append("footer\n\n")
				.append("powered by spring-ai-alibaba\n")
				.append("end footer\n")
				.append(format("circle start<<input>> as %s\n", START))
				.append(format("circle stop as %s\n", END));
		}
	}

	@Override
	protected void appendFooter(Context ctx) {
		if (ctx.isSubGraph()) {
			ctx.sb().append("\n}} ]\n");
		}
		else {
			ctx.sb().append("@enduml\n");
		}
	}

	@Override
	protected void call(Context ctx, String from, String to, CallStyle style) {
		ctx.sb().append(switch (style) {
			case CONDITIONAL -> format("\"%s\" .down.> \"%s\"\n", from, to);
			default -> format("\"%s\" -down-> \"%s\"\n", from, to);
		});
	}

	@Override
	protected void call(Context ctx, String from, String to, String description, CallStyle style) {

		ctx.sb().append(switch (style) {
			case CONDITIONAL -> format("\"%s\" .down.> \"%s\": \"%s\"\n", from, to, description);
			default -> format("\"%s\" -down-> \"%s\": \"%s\"\n", from, to, description);
		});
	}

	@Override
	protected void declareConditionalStart(Context ctx, String name) {
		ctx.sb().append(format("hexagon \"check state\" as %s<<Condition>>\n", name));
	}

	@Override
	protected void declareNode(Context ctx, String name) {
		ctx.sb().append(format("usecase \"%s\"<<Node>>\n", name));
	}

	@Override
	protected void declareConditionalEdge(Context ctx, int ordinal) {
		ctx.sb().append(format("hexagon \"check state\" as condition%d<<Condition>>\n", ordinal));
	}

	@Override
	protected void commentLine(Context ctx, boolean yesOrNo) {
		if (yesOrNo)
			ctx.sb().append("'");
	}

}
