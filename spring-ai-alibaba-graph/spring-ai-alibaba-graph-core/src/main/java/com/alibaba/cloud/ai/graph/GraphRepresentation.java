package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.diagram.MermaidGenerator;
import com.alibaba.cloud.ai.graph.diagram.PlantUMLGenerator;

/**
 * The graph representation in diagram-as-code format.
 * <p>
 * The {@code GraphRepresentation} record encapsulates the visual representation data
 * which could be in various formats like PlantUML or Mermaid syntax. It also includes an
 * enumeration of supported types for graph representations, each associated with a
 * specific generator capable of producing the diagram.
 * </p>
 *
 * @param type the diagram-as-code representation type.
 * @param content the current representation code.
 */
public record GraphRepresentation(Type type, String content) {
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

		/**
		 * Constructs a new instance of {@code Type} with the specified diagram generator.
		 * @param generator the diagram generator to be used by this instance
		 */
		Type(DiagramGenerator generator) {
			this.generator = generator;
		}

	}

}