package com.alibaba.cloud.ai.graph.node.rag;

import com.alibaba.cloud.ai.graph.NodeActionDescriptor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.function.Supplier;

/**
 * @author Dolphin
 * @version V1.0
 * @date 2025/1/2 19:43
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class DocumentRetrieverNodeActionDescriptor extends NodeActionDescriptor {

	private VectorStore vectorStore;

	private Double similarityThreshold;

	private Integer topK;

	private Supplier<Filter.Expression> filterExpression;

}
