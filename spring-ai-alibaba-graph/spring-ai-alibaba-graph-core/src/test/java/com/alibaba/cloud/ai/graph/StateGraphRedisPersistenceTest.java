package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant;
import com.alibaba.cloud.ai.graph.checkpoint.savers.RedisSaver;
import com.alibaba.cloud.ai.graph.state.NodeState;
import com.alibaba.cloud.ai.graph.state.AppenderChannel;
import com.alibaba.cloud.ai.graph.state.Channel;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.alibaba.cloud.ai.graph.utils.CollectionsUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.*;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static com.alibaba.cloud.ai.graph.utils.CollectionsUtils.mapOf;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class StateGraphRedisPersistenceTest {

	static class MessagesState extends NodeState {

		static Map<String, Channel<?>> SCHEMA = CollectionsUtils.mapOf("messages",
				AppenderChannel.<String>of(ArrayList::new));

		public MessagesState(Map<String, Object> initData) {
			super(initData);
		}

		int steps() {
			return value("steps", 0);
		}

		List<String> messages() {
			return this.<List<String>>value("messages").orElseThrow(() -> new RuntimeException("messages not found"));
		}

		Optional<String> lastMessage() {
			List<String> messages = messages();
			if (messages.isEmpty()) {
				return Optional.empty();
			}
			return Optional.of(messages.get(messages.size() - 1));
		}

	}

	@Test
	public void testRedisPersistenceApi() throws Exception {
		Config config = new Config();
		config.useSingleServer().setAddress("redis://host:6379").setPassword("password").setDatabase(0);
		RedissonClient redisson = Redisson.create(config);
		RedisSaver saver = new RedisSaver(redisson);

		RunnableConfig runnableConfig = RunnableConfig.builder().checkPointId("test").threadId("test-thread-2").build();
		Map<String, Object> message = Map.of("message", "hello world");
		Checkpoint checkPoint = Checkpoint.builder()
			.nodeId("agent_1")
			.state(message)
			.nextNodeId(StateGraph.END)
			.build();

		// put
		RunnableConfig put = saver.put(runnableConfig, checkPoint);
		System.out.println("put = " + put);

		// get
		Optional<Checkpoint> checkpoint = saver.get(runnableConfig);
		System.out.println("checkpoint = " + checkpoint);

		// list
		Collection<Checkpoint> list = saver.list(runnableConfig);
		System.out.println("list = " + list);

		// clear
		boolean clear = saver.clear(runnableConfig.withCheckPointId(checkPoint.getId()));
		System.out.println("clear = " + clear);
		Collection<Checkpoint> list1 = saver.list(runnableConfig);
		System.out.println("list1 = " + list1);
	}

	@Test
	public void testCheckpointSaverResubmit() throws Exception {
		int expectedSteps = 5;

		StateGraph<MessagesState> workflow = new StateGraph<>(MessagesState.SCHEMA, MessagesState::new)
			.addEdge(StateGraph.START, "agent_1")
			.addNode("agent_1", AsyncNodeAction.node_async(state -> {
				int steps = state.steps() + 1;
				log.info("agent_1: step: {}", steps);
				return CollectionsUtils.mapOf("steps", steps, "messages", format("agent_1:step %d", steps));
			}))
			.addConditionalEdges("agent_1", AsyncEdgeAction.edge_async(state -> {
				int steps = state.steps();
				if (steps >= expectedSteps) {
					return "exit";
				}
				return "next";
			}), CollectionsUtils.mapOf("next", "agent_1", "exit", StateGraph.END));

		Config config = new Config();
		config.useSingleServer().setAddress("redis://host:6379").setPassword("password").setDatabase(0);
		RedissonClient redisson = Redisson.create(config);
		RedisSaver saver = new RedisSaver(redisson);
		SaverConfig saverConfig = SaverConfig.builder()
			.type(SaverConstant.REDIS)
			.register(SaverConstant.REDIS, saver)
			.build();
		CompileConfig compileConfig = CompileConfig.builder().saverConfig(saverConfig).build();

		CompiledGraph<MessagesState> app = workflow.compile(compileConfig);

		RunnableConfig runnableConfig_1 = RunnableConfig.builder().threadId("thread_3").build();

		RunnableConfig runnableConfig_2 = RunnableConfig.builder().threadId("thread_4").build();

		try {

			for (int execution = 0; execution < 2; execution++) {

				Optional<MessagesState> state = app.invoke(CollectionsUtils.mapOf(), runnableConfig_1);

				assertTrue(state.isPresent());
				assertEquals(expectedSteps + (execution * 2), state.get().steps());

				List<String> messages = state.get().messages();
				assertFalse(messages.isEmpty());

				log.info("thread_1: execution: {} messages:\n{}\n", execution, messages);

				assertEquals(expectedSteps + execution * 2, messages.size());
				for (int i = 0; i < messages.size(); i++) {
					assertEquals(format("agent_1:step %d", (i + 1)), messages.get(i));
				}

				StateSnapshot<MessagesState> snapshot = app.getState(runnableConfig_1);

				assertNotNull(snapshot);
				log.info("SNAPSHOT:\n{}\n", snapshot);

				// SUBMIT NEW THREAD 2

				state = app.invoke(emptyMap(), runnableConfig_2);

				assertTrue(state.isPresent());
				assertEquals(expectedSteps + execution, state.get().steps());
				messages = state.get().messages();

				log.info("thread_2: execution: {} messages:\n{}\n", execution, messages);

				assertEquals(expectedSteps + execution, messages.size());

				// RE-SUBMIT THREAD 1
				state = app.invoke(CollectionsUtils.mapOf(), runnableConfig_1);

				assertTrue(state.isPresent());
				assertEquals(expectedSteps + 1 + execution * 2, state.get().steps());
				messages = state.get().messages();

				log.info("thread_1: execution: {} messages:\n{}\n", execution, messages);

				assertEquals(expectedSteps + 1 + execution * 2, messages.size());

			}
		}
		finally {

			saver.clear(runnableConfig_1);
			saver.clear(runnableConfig_2);
		}
	}

}
