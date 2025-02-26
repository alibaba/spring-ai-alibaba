package com.alibaba.cloud.ai.graph;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant;
import com.alibaba.cloud.ai.graph.state.AppenderChannel;
import lombok.extern.slf4j.Slf4j;
import com.alibaba.cloud.ai.graph.checkpoint.savers.FileSystemSaver;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static java.lang.String.format;
import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static com.alibaba.cloud.ai.graph.utils.CollectionsUtils.mapOf;
import static java.util.Collections.unmodifiableList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for simple App.
 */
@Slf4j
public class StateGraphFileSystemPersistenceTest {



	final String rootPath = Paths.get("target", "checkpoint").toString();

    private static void removeFromList(List<Object> result, AppenderChannel.RemoveIdentifier<Object> removeIdentifier) {
        for (int i = 0; i < result.size(); i++) {
            if (removeIdentifier.compareTo(result.get(i), i) == 0) {
                result.remove(i);
                break;
            }
        }
    }


    private static AppenderChannel.RemoveData<Object> evaluateRemoval(List<Object> oldValues, List<?> newValues) {

        final var result = new AppenderChannel.RemoveData<>(oldValues, newValues);

        newValues.stream()
                .filter(value -> value instanceof AppenderChannel.RemoveIdentifier<?>)
                .forEach(value -> {
                    result.newValues().remove(value);
                    var removeIdentifier = (AppenderChannel.RemoveIdentifier<Object>) value;
                    removeFromList(result.oldValues(), removeIdentifier);

                });
        return result;

    }
    @NotNull
    private static OverAllState getOverAllState() {
        return new OverAllState()
                .input(Map.of())
                .registerKeyAndStrategy("steps", (o, o2) -> o2)
                .registerKeyAndStrategy("messages", (oldValue, newValue) -> {
                    if (newValue == null) {
                        return oldValue;
                    }


                    boolean oldValueIsList = oldValue instanceof List<?>;

                    if (oldValueIsList && newValue instanceof AppenderChannel.RemoveIdentifier<?>) {
                        var result = new ArrayList<>((List<Object>) oldValue);
                        removeFromList(result, (AppenderChannel.RemoveIdentifier) newValue);
                        return unmodifiableList(result);
                    }

                    List<Object> list = null;
                    if (newValue instanceof List) {
                        list = new ArrayList<>((List<?>) newValue);
                    } else if (newValue.getClass().isArray()) {
                        list = new ArrayList<>(Arrays.asList((Object[]) newValue));
                    } else if (newValue instanceof Collection) {
                        list = new ArrayList<>((Collection<?>) newValue);
                    }


                    if (oldValueIsList) {
                        List<Object> oldList = (List<Object>) oldValue;
                        if (list != null) {
                            if (list.isEmpty()) {
                                return oldValue;
                            }
                            if (oldValueIsList) {
                                var result = evaluateRemoval((List<Object>) oldValue, list);
                                List<Object> mergedList = Stream.concat(result.oldValues().stream(), result.newValues().stream())
                                        .distinct()
                                        .collect(Collectors.toList());
                                return mergedList;
                            }
                            oldList.addAll(list);
                        } else {
                            oldList.add(newValue);
                        }
                        return oldList;
                    } else {
                        ArrayList<Object> arrayResult = new ArrayList<>();
                        arrayResult.add(newValue);
                        return arrayResult;
                    }
                });
    }

	@Test
	public void testCheckpointSaverResubmit() throws Exception {
		int expectedSteps = 5;
        OverAllState overAllState = getOverAllState();
        StateGraph workflow = new StateGraph(overAllState).addEdge(START, "agent_1")
			.addNode("agent_1", node_async(state -> {
                Integer o = null;
                if (state.value("steps").isPresent()){
                    o = (Integer) state.value("steps").get();
                }else {
                    o = 0;
                }
                int steps = o + 1;
				log.info("agent_1: step: {}", steps);
				return mapOf("steps", steps, "messages", format("agent_1:step %d", steps));
			}))
			.addConditionalEdges("agent_1", edge_async(state -> {
                Integer steps = (Integer) state.value("steps").get();
				if (steps >= expectedSteps) {
					return "exit";
				}
				return "next";
			}), mapOf("next", "agent_1", "exit", END));

		FileSystemSaver saver = new FileSystemSaver(Paths.get(rootPath, "testCheckpointSaverResubmit"),
				workflow.getStateSerializer());

		SaverConfig saverConfig = SaverConfig.builder().register(SaverConstant.FILE, saver).build();

		CompileConfig compileConfig = CompileConfig.builder().saverConfig(saverConfig).build();

		CompiledGraph app = workflow.compile(compileConfig);

		RunnableConfig runnableConfig_1 = RunnableConfig.builder().threadId("thread_1").build();

		RunnableConfig runnableConfig_2 = RunnableConfig.builder().threadId("thread_2").build();

		try {

			for (int execution = 0; execution < 2; execution++) {

				Optional<OverAllState> state = app.invoke(Map.of(),runnableConfig_1);

				assertTrue(state.isPresent());
				assertEquals(expectedSteps + (execution * 2), (Integer) state.get().value("steps").get());

				List<String> messages = (List<String>) state.get().value("messages").get();
				assertFalse(messages.isEmpty());

				log.info("thread_1: execution: {} messages:\n{}\n", execution, messages);

				assertEquals(expectedSteps + execution * 2, messages.size());
				for (int i = 0; i < messages.size(); i++) {
					assertEquals(format("agent_1:step %d", (i + 1)), messages.get(i));
				}

				StateSnapshot snapshot = app.getState(runnableConfig_1);

				assertNotNull(snapshot);
				log.info("SNAPSHOT:\n{}\n", snapshot);

				// SUBMIT NEW THREAD 2
                overAllState.reset();
				state = app.invoke(Map.of(),runnableConfig_2);

				assertTrue(state.isPresent());
				assertEquals(expectedSteps  + execution, (Integer) state.get().value("steps").get());
				messages = (List<String>) state.get().value("messages").get();;

				log.info("thread_2: execution: {} messages:\n{}\n", execution, messages);

				assertEquals(expectedSteps + execution, messages.size());

				// RE-SUBMIT THREAD 1
                overAllState.reset();
				state = app.invoke(Map.of(), runnableConfig_1);

				assertTrue(state.isPresent());
				assertEquals(expectedSteps  + 1 + execution * 2, (Integer) state.get().value("steps").get());
				messages = (List<String>) state.get().value("messages").get();;

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
