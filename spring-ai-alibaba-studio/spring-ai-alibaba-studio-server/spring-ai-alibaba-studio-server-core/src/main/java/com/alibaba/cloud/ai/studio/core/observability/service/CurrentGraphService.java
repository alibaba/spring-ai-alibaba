package com.alibaba.cloud.ai.studio.core.observability.service;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.studio.core.observability.model.EnhancedNodeOutput;
import com.alibaba.cloud.ai.studio.core.observability.model.ResponseBody;
import com.alibaba.cloud.ai.studio.core.observability.model.SAAGraphFlow;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Service interface for managing and interacting with the current graph.
 */
public interface CurrentGraphService {

	ResponseEntity<Void> run();

	ResponseEntity<ResponseBody> switchTo(String graphId);

	SAAGraphFlow getCurrentGraph();

	/**
	 * Streams state snapshots after each node completes.
	 *
	 * Example Request: GET /write/stream_snapshots?text=I went to the West Lake today, the weather was very good, and I felt very happy
	 *
	 * @param inputText The input text to process.
	 * @return A Flux of maps representing the state snapshots.
	 */
	Flux<Map<String, Object>> writeStreamSnapshots(String inputText);

	/**
	 * Streams basic node outputs as they are generated.
	 *
	 * Example Request: GET /write/stream?text=I went to the West Lake today, the weather was very good, and I felt very happy
	 *
	 * @param inputText The input text to process.
	 * @return A Flux of {@link NodeOutput}.
	 */
	Flux<NodeOutput> writeStream(String inputText);

	/**
	 * Streams enhanced node outputs with complete information.
	 *
	 * This includes node name, ID, execution status, timestamps, etc.
	 * Example Request: GET /write/stream_enhanced?text=I went to the West Lake today, the weather was very good, and I felt very happy
	 *
	 * @param inputText The input text to process.
	 * @return A Flux of {@link EnhancedNodeOutput}.
	 */
	Flux<EnhancedNodeOutput> writeStreamEnhanced(String inputText);

}
