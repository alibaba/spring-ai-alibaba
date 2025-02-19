package com.alibaba.cloud.ai.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.bsc.async.AsyncGenerator;
import org.bsc.async.AsyncGeneratorQueue;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static com.alibaba.cloud.ai.graph.utils.CollectionsUtils.listOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AsyncTest {

	@Test
	public void asyncIteratorTest() throws Exception {

		String[] myArray = { "e1", "e2", "e3", "e4", "e5" };

		final AsyncGenerator<String> it = new AsyncGenerator<String>() {

			private int cursor = 0;

			@Override
			public Data<String> next() {

				if (cursor == myArray.length) {
					return Data.done();
				}

				return Data.of(completedFuture(myArray[cursor++]));
			}
		};

		List<String> result = new ArrayList<>();

		it.forEachAsync(result::add).thenAccept(t -> {
			System.out.println("Finished");
		}).join();

		for (String i : it) {
			result.add(i);
			System.out.println(i);
		}
		System.out.println("Finished");

		assertEquals(myArray.length, result.size());
		assertIterableEquals(listOf(myArray), result);
	}

	@Test
	public void asyncQueueTest() throws Exception {

		final AsyncGenerator<String> it = AsyncGeneratorQueue.of(new LinkedBlockingQueue<AsyncGenerator.Data<String>>(),
				queue -> {
					for (int i = 0; i < 10; ++i) {
						queue.add(AsyncGenerator.Data.of(completedFuture("e" + i)));
					}
				});

		List<String> result = new ArrayList<>();

		it.forEachAsync(result::add).thenAccept((t) -> {
			System.out.println("Finished");
		}).join();

		assertEquals(10, result.size());
		assertIterableEquals(listOf("e0", "e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8", "e9"), result);

	}

	@Test
	public void asyncQueueToStreamTest() throws Exception {

		// AsyncQueue initialized with a direct executor. No thread is used on next()
		// invocation
		final AsyncGenerator<String> it = AsyncGeneratorQueue.of(new LinkedBlockingQueue<AsyncGenerator.Data<String>>(),
				queue -> {
					for (int i = 0; i < 10; ++i) {
						queue.add(AsyncGenerator.Data.of(completedFuture("e" + i)));
					}
				});

		java.util.stream.Stream<String> result = it.stream();

		java.util.Optional<String> lastElement = result.reduce((a, b) -> b);

		assertTrue(lastElement.isPresent());
		assertEquals(lastElement.get(), "e9");

	}

	@Test
	public void asyncQueueIteratorExceptionTest() throws Exception {

		final AsyncGenerator<String> it = AsyncGeneratorQueue.of(new LinkedBlockingQueue<AsyncGenerator.Data<String>>(),
				queue -> {
					for (int i = 0; i < 10; ++i) {
						queue.add(AsyncGenerator.Data.of(completedFuture("e" + i)));

						if (i == 2) {
							throw new RuntimeException("error test");
						}
					}

				});

		java.util.stream.Stream<String> result = it.stream();

		assertThrows(Exception.class, () -> result.reduce((a, b) -> b));

	}

	@Test
	public void asyncQueueForEachExceptionTest() throws Exception {

		final AsyncGenerator<String> it = AsyncGeneratorQueue.of(new LinkedBlockingQueue<AsyncGenerator.Data<String>>(),
				queue -> {
					for (int i = 0; i < 10; ++i) {
						queue.add(AsyncGenerator.Data.of(completedFuture("e" + i)));

						if (i == 2) {
							throw new RuntimeException("error test");
						}
					}

				});

		assertThrows(Exception.class, () -> it.forEachAsync(System.out::println).get());

	}

}
