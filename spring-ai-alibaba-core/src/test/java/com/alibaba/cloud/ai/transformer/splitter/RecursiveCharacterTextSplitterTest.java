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
package com.alibaba.cloud.ai.transformer.splitter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RecursiveCharacterTextSplitter
 */
public class RecursiveCharacterTextSplitterTest {

	private RecursiveCharacterTextSplitter splitter;

	@BeforeEach
	public void setUp() {
		splitter = new RecursiveCharacterTextSplitter(10);
	}

	@Test
	public void testSplitText_BasicFunctionality() {
		String text = "你好，这是一个测试文本。";
		List<String> result = splitter.splitText(text);

		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals("你好", result.get(0));
		assertEquals("这是一个测试文本", result.get(1));
	}

	@Test
	public void testSplitText_EmptyInput() {
		List<String> result = splitter.splitText("");
		assertTrue(result.isEmpty());
	}

	@Test
	public void testSplitText_ExactChunkSize() {
		String text = "0123456789"; // Exactly matches chunk size
		List<String> result = splitter.splitText(text);

		assertEquals(1, result.size());
		assertEquals(text, result.get(0));
	}

	@Test
	public void testSplitText_LongerThanChunkSize() {
		String text = "01234567890123456789"; // 20 characters
		List<String> result = splitter.splitText(text);

		assertEquals(2, result.size());
		assertEquals("0123456789", result.get(0));
		assertEquals("0123456789", result.get(1));
	}

	@Test
	public void testSplitText_WithCustomSeparators() {
		String[] separators = { "##" };
		splitter = new RecursiveCharacterTextSplitter(10, separators);

		String text = "Hello##world##This##is##a##test";
		List<String> result = splitter.splitText(text);

		assertTrue(result.size() > 1);
		assertEquals("Hello", result.get(0));
		assertEquals("world", result.get(1));
	}

	@Test
	public void testSplitText_MultipleSeparators() {
		String[] separators = { "\n\n", "\n", "。" };
		splitter = new RecursiveCharacterTextSplitter(10, separators);

		String text = "Paragraph1。Paragraph2\nParagraph3\n\nParagraph4";
		List<String> result = splitter.splitText(text);

		assertTrue(result.size() > 1);
		assertEquals("Paragraph1", result.get(0));
		assertEquals("Paragraph2", result.get(1));
		assertEquals("Paragraph3", result.get(2));
		assertEquals("Paragraph4", result.get(3));
	}

	@Test
	public void testSplitText_NestedSplitting() {
		String text = "A##B##C##D##E##F##G##H##I##J##K";
		splitter = new RecursiveCharacterTextSplitter(2, new String[] { "##" });

		List<String> result = splitter.splitText(text);

		assertTrue(result.size() > 5);
		assertEquals("A", result.get(0));
		assertEquals("B", result.get(1));
	}

	@Test
	public void testSplitText_WithDifferentChunkSize() {
		splitter = new RecursiveCharacterTextSplitter(5);
		String text = "0123456789ABCDE";
		List<String> result = splitter.splitText(text);

		assertEquals(3, result.size());
		assertEquals("01234", result.get(0));
		assertEquals("56789", result.get(1));
		assertEquals("ABCDE", result.get(2));
	}

	@Test
	public void testConstructor_WithNegativeChunkSize() {
		assertThrows(IllegalArgumentException.class, () -> {
			new RecursiveCharacterTextSplitter(-5);
		});
	}

	@Test
	public void testConstructor_WithZeroChunkSize() {
		assertThrows(IllegalArgumentException.class, () -> {
			new RecursiveCharacterTextSplitter(0);
		});
	}

	@Test
	public void testSplitText_WithSpecialCharacters() {
		String text = "特殊字符测试！@#$%^&*()_+{}[]";
		List<String> result = splitter.splitText(text);

		assertNotNull(result);
		assertFalse(result.isEmpty());
		assertEquals("特殊字符测试", result.get(0));
		assertEquals("@#$%^&*()_", result.get(1));
		assertEquals("+{}[]", result.get(2));
	}

	@Test
	public void testSplitText_WithTabAndNewline() {
		String text = "Tab\tcharacter\ttest\nNewline\ntest";
		List<String> result = splitter.splitText(text);

		assertNotNull(result);
		assertFalse(result.isEmpty());
		assertEquals("Tab\tcharac", result.get(0));
		assertEquals("ter\ttest", result.get(1));
		assertEquals("Newline", result.get(2));
		assertEquals("test", result.get(3));
	}

}
