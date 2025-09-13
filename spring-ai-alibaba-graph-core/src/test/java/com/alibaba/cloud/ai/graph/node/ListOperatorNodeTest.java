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
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ListOperatorNodeTest {

	@Test
	@DisplayName("Test Array Input and Output")
	public void testArray() throws Exception {
		OverAllState t = new OverAllState();
		t.input(Map.of("input", new Number[] { 1, 3, 4, 5.6, 2, 3.5, 1, 2, 0.3, 4, 5, 0.6, 7, 8, 9, 23 }));
		ListOperatorNode<Number> node = ListOperatorNode.<Number>builder()
			.mode(ListOperatorNode.Mode.ARRAY)
			.inputKey("input")
			.outputKey("output")
			.filter(x -> x instanceof Integer)
			.comparator(Comparator.comparing(Number::intValue).reversed())
			.limitNumber(10)
			.build();
		Object[] outputs = (Object[]) node.apply(t).getOrDefault("output", null);
		assertEquals(Arrays.asList(23, 9, 8, 7, 5, 4, 4, 3, 2, 2), Arrays.asList(outputs));
	}

	@Test
	@DisplayName("Test List Input and Output")
	public void testList() throws Exception {
		OverAllState t = new OverAllState();
		t.input(Map.of("input", Arrays.asList("123456", "12345678", "1234", "234567", "abcdefg")));
		// Define a list operation where the elements are nodes of strings.
		ListOperatorNode<String> node = ListOperatorNode.<String>builder()
			.mode(ListOperatorNode.Mode.LIST)
			.inputKey("input")
			.outputKey("output")
			// Retain strings with the prefix '1234'.
			.filter(x -> x.startsWith("1234"))
			// Retain strings with a length greater than 4.
			.filter(x -> x.length() > 4)
			// Sort in lexicographical ascending order.
			.comparator(String::compareTo)
			.limitNumber(10)
			.build();
		assertEquals(Arrays.asList("123456", "12345678"), node.apply(t).getOrDefault("output", null));
	}

	@Test
	@DisplayName("Test Json Input and Output")
	public void testJson() throws Exception {
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
		ListOperatorNode<FileElement> node = ListOperatorNode.<FileElement>builder()
			.elementClassType(FileElement.class)
			// Exclude file objects with certain extensions.
			.filter(x -> x.excludeExtension("jpg", "pdf"))
			// Define the minimum file size.
			.filter(x -> x.sizeNoLessThan(1))
			// Sort files by size first.
			.comparator(Comparator.comparing(FileElement::size))
			// Then sort by filename.
			.comparator(Comparator.comparing(FileElement::name))
			.build();
		ObjectMapper mapper = new ObjectMapper();
		List<FileElement> exceptList = mapper.readValue(
				"[{\"type\":\"archive\",\"size\":102400,\"name\":\"data_2023-10.zip\",\"url\":\"https://example.com/archives/data.zip\",\"extension\":\"zip\",\"mime_type\":\"application/zip\",\"transfer_method\":\"sftp\"},{\"type\":\"video\",\"size\":52428800,\"name\":\"demo-video.mp4\",\"url\":\"https://example.com/videos/demo.mp4\",\"extension\":\"mp4\",\"mime_type\":\"video/mp4\",\"transfer_method\":\"ftp\"}]",
				new TypeReference<List<FileElement>>() {
				});
		assertEquals(exceptList,
				mapper.readValue(node.apply(t).get("output").toString(), new TypeReference<List<FileElement>>() {
				}));
	}

	@Test
	@DisplayName("Test Node in Graph")
	public void testNodeInGraph() throws Exception {
		// Take NumberElement as an example.
		ListOperatorNode<Number> node = ListOperatorNode.<Number>builder()
			.mode(ListOperatorNode.Mode.LIST)
			.inputKey("input")
			.outputKey("output")
			.filter(x -> x instanceof Integer)
			.comparator(Comparator.comparing(Number::intValue).reversed())
			.limitNumber(10)
			.build();
		CompiledGraph compiledGraph = new StateGraph("ListOperatorNode Workflow Demo",
				() -> Map.of("input", new ReplaceStrategy(), "output", new ReplaceStrategy()))
			.addNode("number_list_operator", node_async(node))
			.addEdge(START, "number_list_operator")
			.addEdge("number_list_operator", END)
			.compile();
		assertEquals(Arrays.asList(23, 9, 8, 7, 5, 4, 4, 3, 2, 2),
				compiledGraph
					.call(Map.of("input", Arrays.asList(1, 3, 4, 5.6, 2, 3.5, 1, 2, 0.3, 4, 5, 0.6, 7, 8, 9, 23)))
					.orElseThrow()
					.value("output")
					.orElseThrow());
	}

	record FileElement(String type, Integer size, String name, String url, String extension,
			@JsonProperty("mime_type") String mimeType, @JsonProperty("transfer_method") String transferMethod) {

		public boolean sizeNoLessThan(int sz) {
			return this.size() >= sz;
		}

		public boolean excludeExtension(String... extensions) {
			for (String extension : extensions) {
				if (this.extension().equals(extension)) {
					return false;
				}
			}
			return true;
		}
	}

}
