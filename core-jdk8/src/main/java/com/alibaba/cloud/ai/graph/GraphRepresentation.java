package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.diagram.PlantUMLGenerator;
import lombok.Value;
import com.alibaba.cloud.ai.graph.diagram.MermaidGenerator;

/**
 * The graph representation in diagram-as-a-code format.
 */
@Value
public class GraphRepresentation {

	/**
	 * The supported types.
	 */
	public enum Type {

		/**
		 * A drawable graph using PlantUML syntax.
		 */
		PLANTUML(new PlantUMLGenerator()),
		/**
		 * A drawable graph using Mermaid syntax.
		 */
		MERMAID(new MermaidGenerator());

		final DiagramGenerator generator;

		Type(DiagramGenerator generator) {
			this.generator = generator;
		}

	}

	Type type;

	String content;

}