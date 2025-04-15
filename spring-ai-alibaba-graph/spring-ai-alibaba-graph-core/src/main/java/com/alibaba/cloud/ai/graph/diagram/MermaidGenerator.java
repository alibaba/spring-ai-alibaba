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
package com.alibaba.cloud.ai.graph.diagram;

import com.alibaba.cloud.ai.graph.DiagramGenerator;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static java.lang.String.format;

/**
 * This class represents a MermaidGenerator that extends DiagramGenerator. It generates a
 * flowchart using Mermaid syntax. The flowchart includes various nodes such as start,
 * stop, web_search, retrieve, grade_documents, generate, transform_query, and different
 * conditional states.
 */
public class MermaidGenerator extends DiagramGenerator {

	public static final char SUBGRAPH_PREFIX = '_';

	@Override
	protected void appendHeader(Context ctx) {
		if (ctx.isSubGraph()) {
			ctx.sb()
				.append(format("subgraph %s\n", ctx.title()))
				.append(format("\t%1$c%2$s((start)):::%1$c%2$s\n", SUBGRAPH_PREFIX, START))
				.append(format("\t%1$c%2$s((stop)):::%1$c%2$s\n", SUBGRAPH_PREFIX, END))
			// .append(format("\t#%s@{ shape: start, label: \"enter\" }\n", START))
			// .append(format("\t#%s@{ shape: stop, label: \"exit\" }\n", END))
			;
		}
		else {
			ctx.sb()
				.append(format("---\ntitle: %s\n---\n", ctx.title()))
				.append("flowchart TD\n")
				.append(format("\t%s((start))\n", START))
				.append(format("\t%s((stop))\n", END));
		}
	}

	@Override
	protected void appendFooter(Context ctx) {
		if (ctx.isSubGraph()) {
			ctx.sb().append("end\n");
		}
		else {
			ctx.sb()
				.append('\n')
				.append(format("\tclassDef %c%s fill:black,stroke-width:1px,font-size:xx-small;\n", SUBGRAPH_PREFIX,
						START))
				.append(format("\tclassDef %c%s fill:black,stroke-width:1px,font-size:xx-small;\n", SUBGRAPH_PREFIX,
						END));
		}
	}

	@Override
	protected void declareConditionalStart(Context ctx, String name) {
		ctx.sb().append('\t');
		if (ctx.isSubGraph())
			ctx.sb().append(SUBGRAPH_PREFIX);
		ctx.sb().append(format("%s{\"check state\"}\n", name));
	}

	@Override
	protected void declareNode(Context ctx, String name) {
		ctx.sb().append('\t');
		if (ctx.isSubGraph())
			ctx.sb().append(SUBGRAPH_PREFIX);
		ctx.sb().append(format("%s(\"%s\")\n", name, name));
	}

	@Override
	protected void declareConditionalEdge(Context ctx, int ordinal) {
		ctx.sb().append('\t');
		if (ctx.isSubGraph())
			ctx.sb().append(SUBGRAPH_PREFIX);
		ctx.sb().append(format("condition%d{\"check state\"}\n", ordinal));
	}

	@Override
	protected void commentLine(Context ctx, boolean yesOrNo) {
		if (yesOrNo)
			ctx.sb().append("\t%%");
	}

	@Override
	protected void call(Context ctx, String from, String to, CallStyle style) {
		ctx.sb().append('\t');

		if (ctx.isSubGraph()) {
			ctx.sb().append(switch (style) {
				case CONDITIONAL -> format("%1$c%2$s:::%1$c%2$s -.-> %1$c%3$s:::%1$c%3$s\n", SUBGRAPH_PREFIX, from, to);
				default -> format("%1$c%2$s:::%1$c%2$s --> %1$c%3$s:::%1$c%3$s\n", SUBGRAPH_PREFIX, from, to);
			});
		}
		else {
			ctx.sb().append(switch (style) {
				case CONDITIONAL -> format("%1$s:::%1$s -.-> %2$s:::%2$s\n", from, to);
				default -> format("%1$s:::%1$s --> %2$s:::%2$s\n", from, to);
			});
		}
	}

	@Override
	protected void call(Context ctx, String from, String to, String description, CallStyle style) {
		ctx.sb().append('\t');
		if (ctx.isSubGraph()) {
			ctx.sb().append(switch (style) {
				case CONDITIONAL -> format("%1$s%2$s:::%1$c%2$s -.->|%3$s| %1$s%4$s:::%1$c%4$s\n", SUBGRAPH_PREFIX,
						from, description, to);
				default -> format("%1$s%2$s:::%1$c%2$s -->|%3$s| %1$s%4$s:::%1$c%4$s\n", SUBGRAPH_PREFIX, from,
						description, to);
			});
		}
		else {
			ctx.sb().append(switch (style) {
				case CONDITIONAL -> format("%1$s:::%1$s -.->|%2$s| %3$s:::%3$s\n", from, description, to);
				default -> format("%1$s:::%1$s -->|%2$s| %3$s:::%3$s\n", from, description, to);
			});
		}

	}

}
