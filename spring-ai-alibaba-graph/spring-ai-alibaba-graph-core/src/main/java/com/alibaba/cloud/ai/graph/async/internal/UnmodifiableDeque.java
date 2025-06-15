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
package com.alibaba.cloud.ai.graph.async.internal;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;

public class UnmodifiableDeque<T> implements Deque<T> {

	private final Deque<T> deque;

	public UnmodifiableDeque(Deque<T> deque) {
		this.deque = deque;
	}

	@Override
	public boolean add(T t) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean offer(T t) {
		throw new UnsupportedOperationException();
	}

	@Override
	public T remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public T poll() {
		throw new UnsupportedOperationException();
	}

	@Override
	public T element() {
		return deque.element();
	}

	@Override
	public T peek() {
		return deque.peek();
	}

	@Override
	public void addFirst(T t) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addLast(T t) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean offerFirst(T t) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean offerLast(T t) {
		throw new UnsupportedOperationException();
	}

	@Override
	public T removeFirst() {
		throw new UnsupportedOperationException();
	}

	@Override
	public T removeLast() {
		throw new UnsupportedOperationException();
	}

	@Override
	public T pollFirst() {
		throw new UnsupportedOperationException();
	}

	@Override
	public T pollLast() {
		throw new UnsupportedOperationException();
	}

	@Override
	public T getFirst() {
		return deque.getFirst();
	}

	@Override
	public T getLast() {
		return deque.getLast();
	}

	@Override
	public T peekFirst() {
		return deque.peekFirst();
	}

	@Override
	public T peekLast() {
		return deque.peekLast();
	}

	@Override
	public boolean removeFirstOccurrence(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeLastOccurrence(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return deque.containsAll(c);
	}

	@Override
	public boolean contains(Object o) {
		return deque.contains(o);
	}

	@Override
	public int size() {
		return deque.size();
	}

	@Override
	public boolean isEmpty() {
		return deque.isEmpty();
	}

	@Override
	public Iterator<T> iterator() {
		return deque.iterator();
	}

	@Override
	public Object[] toArray() {
		return deque.toArray();
	}

	@Override
	public <T1> T1[] toArray(T1[] a) {
		return deque.toArray(a);
	}

	@Override
	public Iterator<T> descendingIterator() {
		return deque.descendingIterator();
	}

	@Override
	public void push(T t) {
		throw new UnsupportedOperationException();
	}

	@Override
	public T pop() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

}
