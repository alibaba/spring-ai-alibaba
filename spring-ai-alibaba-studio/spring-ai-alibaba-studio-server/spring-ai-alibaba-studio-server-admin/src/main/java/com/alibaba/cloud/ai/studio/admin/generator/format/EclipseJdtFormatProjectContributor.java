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
package com.alibaba.cloud.ai.studio.admin.generator.format;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.stream.Stream;

import io.spring.initializr.generator.project.contributor.ProjectContributor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;

/**
 * Use the CodeFormatter from Eclipse JDT Core to format all generated Java files.
 */
// TODO: 移除未使用的import语句
public class EclipseJdtFormatProjectContributor implements ProjectContributor {

	@Override
	public void contribute(Path projectRoot) throws IOException {
		Path javaSrc = projectRoot.resolve("src/main/java");
		if (!Files.exists(javaSrc)) {
			return;
		}

		// Configure formatting options
		Map<String, String> options = DefaultCodeFormatterConstants.getEclipseDefaultSettings();
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_17);
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_17);
		options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_17);
		// Use space indentation, 4 spaces
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, "space");
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");

		// Line length and wrapping configuration
		options.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, "120");

		// Method chain formatting - moderate wrapping
		options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_SELECTOR_IN_METHOD_INVOCATION,
				DefaultCodeFormatterConstants.createAlignmentValue(false, // Don't force
																			// wrapping
						DefaultCodeFormatterConstants.WRAP_COMPACT, // Compact wrapping
																	// when needed
						DefaultCodeFormatterConstants.INDENT_DEFAULT));

		// Keep simple statements on one line
		options.put(DefaultCodeFormatterConstants.FORMATTER_KEEP_SIMPLE_IF_ON_ONE_LINE,
				DefaultCodeFormatterConstants.TRUE);

		CodeFormatter formatter = ToolFactory.createCodeFormatter(options);

		try (Stream<Path> files = Files.walk(javaSrc)) {
			files.filter(Files::isRegularFile)
				.filter(path -> path.toString().endsWith(".java"))
				.forEach(this.applyFormatting(formatter));
		}
	}

	private java.util.function.Consumer<Path> applyFormatting(CodeFormatter formatter) {
		return javaFile -> {
			try {
				String source = Files.readString(javaFile);
				TextEdit edit = formatter.format(CodeFormatter.K_COMPILATION_UNIT, source, 0, source.length(), 0,
						System.lineSeparator());
				if (edit != null) {
					Document document = new Document(source);
					edit.apply(document);
					Files.writeString(javaFile, document.get(), StandardOpenOption.TRUNCATE_EXISTING);
				}
			}
			catch (IOException e) {
				throw new UncheckedIOException("I/O error while formatting " + javaFile, e);
			}
			catch (Exception e) {
				throw new IllegalStateException("Failed to format " + javaFile, e);
			}
		};
	}

}
