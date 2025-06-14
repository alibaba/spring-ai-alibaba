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
package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.OverAllStateFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ListOperatorNodeTest {

	@Test
	public void testNumberListOperatorNode() throws Exception {
		OverAllState t = new OverAllState();
		t.input(Map.of("input", "[1,3,4,5.6,2,3.5,1,2,0.3,4,5,0.6,7,8,9,23]"));
		// Define a list operation where the elements are nodes of numbers (integers or
		// floating-point numbers).
		ListOperatorNode<ListOperatorNode.NumberElement> node = ListOperatorNode.<ListOperatorNode.NumberElement>builder()
			.elementClassType(ListOperatorNode.NumberElement.class)
			.inputTextKey("input")
			.outputTextKey("output")
			// Filter the elements to retain only the integer parts of the list.
			.filter(ListOperatorNode.NumberElement::isInteger)
			// Sort in descending order.
			.comparator(ListOperatorNode.NumberElement::compareToReverse)
			.limitNumber(10)
			.build();
		assertEquals("[23,9,8,7,5,4,4,3,2,2]", node.apply(t).get("output").toString());
	}

	@Test
	public void testStringListOperatorNode() throws Exception {
		OverAllState t = new OverAllState();
		t.input(Map.of("input", "[\"123456\", \"12345678\", \"1234\", \"234567\", \"abcdefg\"]"));
		// Define a list operation where the elements are nodes of strings.
		ListOperatorNode<ListOperatorNode.StringElement> node = ListOperatorNode.<ListOperatorNode.StringElement>builder()
			.elementClassType(ListOperatorNode.StringElement.class)
			.inputTextKey("input")
			.outputTextKey("output")
			// Retain strings with the prefix '1234'.
			.filter(x -> x.getValue().startsWith("1234"))
			// Retain strings with a length greater than 4.
			.filter(x -> x.getValue().length() > 4)
			// Sort in lexicographical ascending order.
			.comparator(ListOperatorNode.StringElement::compareTo)
			.limitNumber(10)
			.build();
		assertEquals("[\"123456\",\"12345678\"]", node.apply(t).get("output").toString());
	}

	@Test
	public void testFileListOperatorNode() throws Exception {
		OverAllState t = new OverAllState();
		// Simulate JSON file objects.
		String jsonList = "["
				+ "{\"type\":\"image\",\"size\":2048000,\"name\":\"sunset.jpg\",\"url\":\"https://example.com/files/sunset.jpg\",\"extension\":\"jpg\",\"mime_type\":\"image/jpeg\",\"transfer_method\":\"http\"},"
				+ "{\"type\":\"document\",\"size\":512000,\"name\":\"report_2023.pdf\",\"url\":\"https://example.com/docs/report.pdf\",\"extension\":\"pdf\",\"mime_type\":\"application/pdf\",\"transfer_method\":\"s3\"},"
				+ "{\"type\":\"video\",\"size\":52428800,\"name\":\"demo-video.mp4\",\"url\":\"https://example.com/videos/demo.mp4\",\"extension\":\"mp4\",\"mime_type\":\"video/mp4\",\"transfer_method\":\"ftp\"},"
				+ "{\"type\":\"text\",\"size\":0,\"name\":\"empty-file.txt\",\"url\":\"https://example.com/files/empty.txt\",\"extension\":\"txt\",\"mime_type\":\"text/plain\",\"transfer_method\":\"local\"},"
				+ "{\"type\":\"archive\",\"size\":102400,\"name\":\"data_2023-10.zip\",\"url\":\"https://example.com/archives/data.zip\",\"extension\":\"zip\",\"mime_type\":\"application/zip\",\"transfer_method\":\"sftp\"}"
				+ "]";
		t.input(Map.of("input", jsonList));
		// Define a list operation where the elements are nodes of file objects.
		ListOperatorNode<ListOperatorNode.FileElement> node = ListOperatorNode.<ListOperatorNode.FileElement>builder()
			.elementClassType(ListOperatorNode.FileElement.class)
			// Exclude file objects with certain extensions.
			.filter(x -> x.excludeExtension("jpg", "pdf"))
			// Define the minimum file size.
			.filter(x -> x.sizeNoLessThan(1))
			// Sort files by size first.
			.comparator(ListOperatorNode.FileElement::compareSize)
			// Then sort by filename.
			.comparator(ListOperatorNode.FileElement::compareName)
			.build();
		assertEquals(
				"[{\"type\":\"archive\",\"size\":102400,\"name\":\"data_2023-10.zip\",\"url\":\"https://example.com/archives/data.zip\",\"extension\":\"zip\",\"mime_type\":\"application/zip\",\"transfer_method\":\"sftp\"},{\"type\":\"video\",\"size\":52428800,\"name\":\"demo-video.mp4\",\"url\":\"https://example.com/videos/demo.mp4\",\"extension\":\"mp4\",\"mime_type\":\"video/mp4\",\"transfer_method\":\"ftp\"}]",
				node.apply(t).get("output").toString());
	}

	@Test
	public void testNodeInGraph() throws Exception {
		// Take NumberElement as an example.
		ListOperatorNode<ListOperatorNode.NumberElement> node = ListOperatorNode.<ListOperatorNode.NumberElement>builder()
			.elementClassType(ListOperatorNode.NumberElement.class)
			.inputTextKey("input")
			.outputTextKey("output")
			.filter(ListOperatorNode.NumberElement::isInteger)
			.comparator(ListOperatorNode.NumberElement::compareToReverse)
			.limitNumber(10)
			.build();
		OverAllStateFactory stateFactory = () -> {
			OverAllState state = new OverAllState();
			state.registerKeyAndStrategy("input", new ReplaceStrategy());
			state.registerKeyAndStrategy("output", new ReplaceStrategy());
			return state;
		};
		CompiledGraph compiledGraph = new StateGraph("ListOperatorNode Workflow Demo", stateFactory)
			.addNode("number_list_operator", node_async(node))
			.addEdge(START, "number_list_operator")
			.addEdge("number_list_operator", END)
			.compile();
		assertEquals("[23,9,8,7,5,4,4,3,2,2]",
				compiledGraph.invoke(Map.of("input", "[1,3,4,5.6,2,3.5,1,2,0.3,4,5,0.6,7,8,9,23]"))
					.orElseThrow()
					.value("output")
					.orElseThrow()
					.toString());
	}

}
