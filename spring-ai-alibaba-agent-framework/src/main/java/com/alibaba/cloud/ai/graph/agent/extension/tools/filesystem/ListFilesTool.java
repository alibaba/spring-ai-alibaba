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
package com.alibaba.cloud.ai.graph.agent.extension.tools.filesystem;

import com.alibaba.cloud.ai.graph.agent.extension.file.FileInfo;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.nio.file.Paths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * Tool for listing files in a directory.
 */
public class ListFilesTool implements BiFunction<String, ToolContext, String> {

	public static final String DESCRIPTION = """
			Lists all files in the filesystem, filtering by directory.
			
			Usage:
			- The path parameter must be an absolute path, not a relative path
			- The list_files tool will return a list of all files in the specified directory.
			- This is very useful for exploring the file system and finding the right file to read or edit.
			- You should almost ALWAYS use this tool before using the Read or Edit tools.
			""";

	public ListFilesTool() {
	}

	@Override
	public String apply(
			@ToolParam(description = "The directory path to list files from") String path,
			ToolContext toolContext) {
		try {
			Path dirPath = Paths.get(path);
			List<FileInfo> fileInfos = listFilesContent(dirPath, null, false);
			
			// Format as simple path list for backward compatibility
			List<String> filePaths = new ArrayList<>();
			for (FileInfo info : fileInfos) {
				filePaths.add(info.getPath());
			}
			
			return filePaths.isEmpty() ? "Directory is empty" : String.join("\n", filePaths);
		}
		catch (Exception e) {
			return "Error listing directory '" + path + "': " + e.getMessage();
		}
	}

	/**
	 * Core logic for listing files and directories in a directory.
	 * This method can be reused by other classes like FileSystemTools.
	 *
	 * @param dirPath The directory path to list files from
	 * @param cwd Current working directory for virtual mode path processing (null to disable virtual mode)
	 * @param virtualMode Whether to use virtual mode (strip cwd prefix from paths)
	 * @return List of FileInfo objects for files and directories
	 */
	public static List<FileInfo> listFilesContent(Path dirPath, Path cwd, boolean virtualMode) {
		List<FileInfo> results = new ArrayList<>();
		
		try {
			if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
				return results;
			}

			String cwdStr = cwd != null ? cwd.toString() : null;
			if (cwdStr != null && !cwdStr.endsWith("/")) {
				cwdStr += "/";
			}

			try (Stream<Path> paths = Files.list(dirPath)) {
				for (Path childPath : paths.toList()) {
					try {
						boolean isFile = Files.isRegularFile(childPath, LinkOption.NOFOLLOW_LINKS);
						boolean isDir = Files.isDirectory(childPath, LinkOption.NOFOLLOW_LINKS);

						String absPath = childPath.toString();

						if (!virtualMode || cwd == null) {
							// Non-virtual mode: use absolute paths
							if (isFile) {
								try {
									BasicFileAttributes attrs = Files.readAttributes(childPath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
									results.add(new FileInfo(
										absPath,
										false,
										attrs.size(),
										formatTimestamp(attrs.lastModifiedTime().toInstant())
									));
								}
								catch (IOException e) {
									results.add(new FileInfo(absPath, false, null, null));
								}
							}
							else if (isDir) {
								try {
									BasicFileAttributes attrs = Files.readAttributes(childPath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
									results.add(new FileInfo(
										absPath + "/",
										true,
										0L,
										formatTimestamp(attrs.lastModifiedTime().toInstant())
									));
								}
								catch (IOException e) {
									results.add(new FileInfo(absPath + "/", true, null, null));
								}
							}
						}
						else {
							// Virtual mode: strip cwd prefix
							String relativePath;
							if (cwdStr != null && absPath.startsWith(cwdStr)) {
								relativePath = absPath.substring(cwdStr.length());
							}
							else if (cwdStr != null && absPath.startsWith(cwd.toString())) {
								relativePath = absPath.substring(cwd.toString().length());
								if (relativePath.startsWith("/")) {
									relativePath = relativePath.substring(1);
								}
							}
							else {
								relativePath = absPath;
							}

							String virtPath = "/" + relativePath;

							if (isFile) {
								try {
									BasicFileAttributes attrs = Files.readAttributes(childPath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
									results.add(new FileInfo(
										virtPath,
										false,
										attrs.size(),
										formatTimestamp(attrs.lastModifiedTime().toInstant())
									));
								}
								catch (IOException e) {
									results.add(new FileInfo(virtPath, false, null, null));
								}
							}
							else if (isDir) {
								try {
									BasicFileAttributes attrs = Files.readAttributes(childPath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
									results.add(new FileInfo(
										virtPath + "/",
										true,
										0L,
										formatTimestamp(attrs.lastModifiedTime().toInstant())
									));
								}
								catch (IOException e) {
									results.add(new FileInfo(virtPath + "/", true, null, null));
								}
							}
						}
					}
					catch (Exception ignored) {
						// Skip files that can't be accessed
					}
				}
			}

			// Keep deterministic order by path
			results.sort(Comparator.comparing(FileInfo::getPath));
			return results;
		}
		catch (Exception e) {
			return results;
		}
	}

	/**
	 * Format timestamp as ISO offset date time string.
	 */
	private static String formatTimestamp(Instant instant) {
		return instant.atOffset(ZoneOffset.UTC)
			.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}

	public static ToolCallback createListFilesToolCallback(String description) {
		return FunctionToolCallback.builder("ls", new ListFilesTool())
				.description(description)
				.inputType(String.class)
				.build();
	}
}

