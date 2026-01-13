/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.tool;

import org.springframework.ai.chat.model.ToolContext;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for streaming tool callbacks.
 * Tools implementing this interface can emit results incrementally as they process.
 *
 * <p>Extends {@link AsyncToolCallback} to inherit async execution capabilities.
 * The streaming interface provides a more fine-grained way to report progress,
 * emitting intermediate results while processing continues.</p>
 *
 * <p>The stream should emit {@link ToolResult} chunks and end with a final result
 * that has {@code isFinal=true}. The final result signals stream completion.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * public class StreamingSearchTool implements StreamingToolCallback {
 *
 *     @Override
 *     public ToolDefinition getToolDefinition() {
 *         return ToolDefinition.builder()
 *             .name("streamingSearch")
 *             .description("Search with streaming progress updates")
 *             .build();
 *     }
 *
 *     @Override
 *     public Flux<ToolResult> callStream(String arguments, ToolContext context) {
 *         return Flux.interval(Duration.ofMillis(100))
 *             .take(10)
 *             .map(i -> ToolResult.chunk("Processing batch " + i + "...\n"))
 *             .concatWith(Flux.just(ToolResult.text("Search completed!").withFinal(true)));
 *     }
 * }
 * }</pre>
 *
 * <p>For tools that generate multimodal content:</p>
 * <pre>{@code
 * public class ImageGeneratorTool implements StreamingToolCallback {
 *
 *     @Override
 *     public Flux<ToolResult> callStream(String arguments, ToolContext context) {
 *         return Flux.interval(Duration.ofMillis(500))
 *             .take(10)
 *             .map(i -> ToolResult.chunk("Generating... " + (i * 10) + "%\n"))
 *             .concatWith(Flux.defer(() -> {
 *                 byte[] imageData = generateImage(arguments);
 *                 Media imageMedia = new Media(MimeTypeUtils.IMAGE_PNG, imageData);
 *                 return Flux.just(
 *                     ToolResult.mixed("Image generated!", List.of(imageMedia))
 *                         .withFinal(true)
 *                 );
 *             }));
 *     }
 * }
 * }</pre>
 *
 * @author disaster
 * @since 1.0.0
 * @see AsyncToolCallback
 * @see ToolResult
 */
public interface StreamingToolCallback extends AsyncToolCallback {

	/**
	 * Executes the tool and returns a stream of results.
	 * The stream should emit ToolResult chunks and end with a final result.
	 *
	 * <p>Implementation guidelines:</p>
	 * <ul>
	 *   <li>Emit {@link ToolResult#chunk(String)} for progress updates</li>
	 *   <li>End with a result where {@link ToolResult#isFinal()} returns true</li>
	 *   <li>Handle errors within the stream using {@code onErrorResume}</li>
	 *   <li>Respect cancellation through proper Flux subscription handling</li>
	 * </ul>
	 *
	 * @param arguments the tool arguments as JSON string
	 * @param context the tool execution context
	 * @return a Flux of ToolResult chunks
	 */
	Flux<ToolResult> callStream(String arguments, ToolContext context);

	/**
	 * Default async implementation that reduces the stream to a single result.
	 * The stream is collected and merged into a final string result.
	 *
	 * @param arguments the tool arguments as JSON string
	 * @param context the tool execution context
	 * @return a CompletableFuture that completes with the merged result
	 */
	@Override
	default CompletableFuture<String> callAsync(String arguments, ToolContext context) {
		return callStream(arguments, context).reduce(ToolResult::merge).map(ToolResult::toStringResult).toFuture();
	}

	/**
	 * Returns whether this tool supports streaming.
	 * @return true (always streaming for this interface)
	 */
	default boolean isStreaming() {
		return true;
	}

}
