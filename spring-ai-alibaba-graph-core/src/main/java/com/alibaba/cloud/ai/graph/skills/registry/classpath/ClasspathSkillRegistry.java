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
package com.alibaba.cloud.ai.graph.skills.registry.classpath;

import com.alibaba.cloud.ai.graph.skills.SkillMetadata;
import com.alibaba.cloud.ai.graph.skills.registry.AbstractSkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.filesystem.SkillScanner;

import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.alibaba.cloud.ai.graph.skills.registry.filesystem.FileSystemSkillRegistry.DEFAULT_SYSTEM_PROMPT_TEMPLATE;

/**
 * Classpath-based implementation of SkillRegistry.
 *
 * This implementation loads skills from classpath:resources/skills, supporting both
 * filesystem (development) and JAR (production) environments.
 *
 * <p>Key features:
 * <ul>
 *   <li>Automatically detects if resources are in filesystem or JAR</li>
 *   <li>Supports nested resources in Spring Boot executable JARs</li>
 *   <li>Manages JAR FileSystem lifecycle</li>
 *   <li>Caches skill content for JAR resources (since Path.of() cannot access JAR paths)</li>
 * </ul>
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * // Use default classpath location (resources/skills)
 * ClasspathSkillRegistry registry = ClasspathSkillRegistry.builder().build();
 *
 * // Custom classpath path
 * ClasspathSkillRegistry registry = ClasspathSkillRegistry.builder()
 *     .classpathPath("custom/skills")
 *     .build();
 * }</pre>
 */
public class ClasspathSkillRegistry extends AbstractSkillRegistry {

	private static final Logger logger = LoggerFactory.getLogger(ClasspathSkillRegistry.class);
	private final String classpathPath;
	private final Path basePath;
	private final SkillScanner scanner = new SkillScanner();
	private final SystemPromptTemplate systemPromptTemplate;
	// Map to cache fullContent for skills loaded from JAR (skill name -> fullContent)
	// This is needed because JAR paths cannot be accessed via Path.of() with default filesystem
	private final Map<String, String> jarSkillContentCache = new HashMap<>();
	// JAR FileSystem for classpath resources (only created if resource is in JAR)
	private FileSystem jarFileSystem;
	private boolean ownsJarFileSystem;
	// Each registry owns an isolated workspace containing its immutable generations.
	private Path bootExtractionWorkspace;

	private ClasspathSkillRegistry(Builder builder) {
		this.classpathPath = builder.classpathPath != null && !builder.classpathPath.isEmpty()
				? builder.classpathPath
				: "skills";

		// Set basePath - default to /tmp if not specified
		this.basePath = builder.basePath != null && !builder.basePath.isEmpty()
				? Path.of(builder.basePath)
				: Path.of("/tmp");

		// Ensure basePath directory exists
		try {
			Files.createDirectories(basePath);
		}
		catch (IOException e) {
			logger.error("Failed to create basePath directory {}: {}", basePath, e.getMessage());
		}

		// Set system prompt template - use provided or default
		if (builder.systemPromptTemplate != null) {
			this.systemPromptTemplate = builder.systemPromptTemplate;
		}
		else {
			this.systemPromptTemplate = SystemPromptTemplate.builder()
					.template(DEFAULT_SYSTEM_PROMPT_TEMPLATE)
					.build();
		}

		// Load skills during initialization if auto-load is enabled (default: true)
		if (builder.autoLoad) {
			loadSkillsToRegistry();
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Loads skills from classpath into the registry.
	 * Supports both filesystem (development) and JAR (production) environments.
	 */
	@Override
	protected void loadSkillsToRegistry() {
		Map<String, SkillMetadata> loadedSkills = new HashMap<>();

		// Clear JAR skill content cache on reload
		jarSkillContentCache.clear();

		try {
			URL resource = getClass().getClassLoader().getResource(classpathPath);
			if (resource == null) {
				logger.debug("No '{}' resource found in classpath", classpathPath);
				this.skills = loadedSkills;
				return;
			}

			Path classpathSkillsPath = null;
			try {
				URI uri = resource.toURI();

				if ("file".equals(uri.getScheme())) {
					// Resource is on filesystem (development mode)
					classpathSkillsPath = Path.of(uri);
				}
				else if ("jar".equals(uri.getScheme())) {
					if (isSpringBootJar(uri)) {
						try {
							loadSkillsFromSpringBootJar(loadedSkills);
							this.skills = loadedSkills;
						}
						catch (Exception e) {
							logger.error("Failed to reload Spring Boot classpath skills; keeping the previous registry: {}",
								e.getMessage(), e);
						}
						return;
					}
					// Resource is in a JAR file (production mode)
					// Create or reuse JAR FileSystem
					if (jarFileSystem == null || !jarFileSystem.isOpen()) {
						jarFileSystem = getOrCreateJarFileSystem(uri);
					}
					// Get path within the JAR file system
					// URI format: jar:file:/path/to.jar!/skills
					// The path after ! is the path within the JAR
					String jarPath = uri.getSchemeSpecificPart();
					int separatorIndex = jarPath.indexOf('!');
					if (separatorIndex != -1 && separatorIndex + 1 < jarPath.length()) {
						String pathInJar = jarPath.substring(separatorIndex + 1);
						// Ensure path starts with / for JAR filesystem
						if (!pathInJar.startsWith("/")) {
							pathInJar = "/" + pathInJar;
						}
						classpathSkillsPath = jarFileSystem.getPath(pathInJar);
					}
					else {
						// Fallback: use the resource path directly
						classpathSkillsPath = jarFileSystem.getPath("/skills");
					}
				}
				else {
					logger.debug("Unsupported classpath resource protocol: {}", uri.getScheme());
					this.skills = loadedSkills;
					return;
				}
			}
			catch (URISyntaxException e) {
				logger.debug("Invalid resource URI: {}", e.getMessage());
				this.skills = loadedSkills;
				return;
			}
			catch (IOException e) {
				logger.debug("Failed to create JAR filesystem: {}", e.getMessage());
				this.skills = loadedSkills;
				return;
			}

			if (classpathSkillsPath != null && Files.exists(classpathSkillsPath)) {
				// Scan the classpath directory (works for both filesystem and JAR)
				List<SkillMetadata> classpathSkills = scanClasspathDirectory(classpathSkillsPath, "classpath");
				for (SkillMetadata skill : classpathSkills) {
					loadedSkills.put(skill.getName(), skill);
				}
				logger.info("Loaded {} skills from classpath: {}", loadedSkills.size(), classpathPath);
			}
		}
		catch (Exception e) {
			logger.debug("Failed to load skills from classpath: {}", e.getMessage());
		}

		this.skills = loadedSkills;
	}

	private boolean isSpringBootJar(URI uri) {
		String uriString = uri.toString();
		return uriString.startsWith("jar:nested:") || uriString.contains("BOOT-INF/");
	}

	/**
	 * Loads skills from a Spring Boot executable JAR. Spring Boot uses nested JAR URLs
	 * that are not supported by the JDK's NIO JAR file system provider, so resources
	 * are resolved through Spring and copied to the configured base path first.
	 */
	private void loadSkillsFromSpringBootJar(Map<String, SkillMetadata> loadedSkills) throws IOException {
		Path targetRoot = basePath.resolve(classpathPath).normalize();
		Files.createDirectories(targetRoot);

		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(
				getClass().getClassLoader());
		Map<String, Integer> classpathRootOrder = getClasspathRootOrder(resolver);
		Resource[] resources = resolver.getResources("classpath*:" + classpathPath + "/**/*");
		Map<String, Map<String, List<ResolvedSkillResource>>> skillCandidates = new LinkedHashMap<>();

		for (Resource resource : resources) {
			if (!resource.isReadable()) {
				continue;
			}

			ResolvedResourcePath resolvedPath = getResolvedResourcePath(resource);
			Path relativePath = resolvedPath != null ? resolvedPath.relativePath() : null;
			if (relativePath == null || relativePath.getNameCount() < 2) {
				continue;
			}

			String skillDirectoryName = relativePath.getName(0).toString();
			String sourceRoot = resolvedPath.sourceRoot();
			classpathRootOrder.computeIfAbsent(sourceRoot, ignored -> classpathRootOrder.size());
			skillCandidates.computeIfAbsent(skillDirectoryName, ignored -> new LinkedHashMap<>())
				.computeIfAbsent(sourceRoot, ignored -> new ArrayList<>())
				.add(new ResolvedSkillResource(resource, relativePath));
		}

		// Select the highest-precedence complete source; ancillary files alone must not
		// shadow a lower-precedence skill that contains SKILL.md.
		Map<String, List<ResolvedSkillResource>> selectedSkillResources = new LinkedHashMap<>();
		for (Map.Entry<String, Map<String, List<ResolvedSkillResource>>> skillCandidate : skillCandidates
			.entrySet()) {
			String selectedSourceRoot = null;
			for (Map.Entry<String, List<ResolvedSkillResource>> sourceCandidate : skillCandidate.getValue()
				.entrySet()) {
				if (containsSkillManifest(sourceCandidate.getValue()) && (selectedSourceRoot == null
						|| classpathRootOrder.get(sourceCandidate.getKey()) < classpathRootOrder.get(selectedSourceRoot))) {
					selectedSourceRoot = sourceCandidate.getKey();
				}
			}
			if (selectedSourceRoot != null) {
				selectedSkillResources.put(skillCandidate.getKey(), skillCandidate.getValue().get(selectedSourceRoot));
			}
		}

		// Build an unpublished generation first. Its content-addressed published path is
		// immutable and remains readable until close(), while identical reloads reuse it.
		if (bootExtractionWorkspace == null || !Files.isDirectory(bootExtractionWorkspace)) {
			bootExtractionWorkspace = Files.createTempDirectory(targetRoot, ".registry-");
		}
		Path stagingRoot = Files.createTempDirectory(bootExtractionWorkspace, ".staging-");
		Path extractionRoot = null;
		boolean publishedNewGeneration = false;
		try {
			for (Map.Entry<String, List<ResolvedSkillResource>> skillResources : selectedSkillResources.entrySet()) {
				for (ResolvedSkillResource skillResource : skillResources.getValue()) {
					Path targetFile = stagingRoot.resolve(skillResource.relativePath()).normalize();
					if (!targetFile.startsWith(stagingRoot)) {
						throw new IOException("Classpath skill resource resolves outside extraction directory: "
								+ skillResource.resource());
					}
					Files.createDirectories(targetFile.getParent());
					try (InputStream inputStream = skillResource.resource().getInputStream()) {
						Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
					}
				}
			}

			PublishedExtraction publishedExtraction = publishExtractionGeneration(stagingRoot, bootExtractionWorkspace);
			extractionRoot = publishedExtraction.path();
			publishedNewGeneration = publishedExtraction.created();
			Map<String, SkillMetadata> generationSkills = new HashMap<>();
			for (String skillDirectoryName : selectedSkillResources.keySet()) {
				Path skillDirectory = extractionRoot.resolve(skillDirectoryName);
				SkillMetadata metadata = scanner.loadSkill(skillDirectory, "classpath");
				if (metadata == null) {
					throw new IOException("Failed to scan extracted classpath skill: " + skillDirectory);
				}
				metadata.setSkillPath(skillDirectory.toString());
				generationSkills.put(metadata.getName(), metadata);
			}

			loadedSkills.putAll(generationSkills);
		}
		catch (Exception e) {
			IOException loadFailure = e instanceof IOException ioException ? ioException
					: new IOException("Failed to extract Spring Boot classpath skills", e);
			try {
				deleteDirectoryRecursively(stagingRoot);
			}
			catch (IOException cleanupFailure) {
				loadFailure.addSuppressed(cleanupFailure);
			}
			if (publishedNewGeneration && extractionRoot != null) {
				try {
					deleteDirectoryRecursively(extractionRoot);
				}
				catch (IOException cleanupFailure) {
					loadFailure.addSuppressed(cleanupFailure);
				}
			}
			throw loadFailure;
		}

		logger.info("Loaded {} skills from Spring Boot classpath: {}", loadedSkills.size(), classpathPath);
	}

	private PublishedExtraction publishExtractionGeneration(Path stagingRoot, Path targetRoot) throws IOException {
		String expectedDigest = calculateExtractionDigest(stagingRoot);
		String extractionName = ".extracted-" + expectedDigest;
		int suffix = 0;
		while (true) {
			Path extractionRoot = targetRoot.resolve(suffix == 0 ? extractionName : extractionName + "-" + suffix);
			if (Files.exists(extractionRoot)) {
				if (isMatchingExtraction(extractionRoot, expectedDigest)) {
					deleteDirectoryRecursively(stagingRoot);
					return new PublishedExtraction(extractionRoot, false);
				}
				suffix++;
				continue;
			}

			try {
				try {
					Files.move(stagingRoot, extractionRoot, StandardCopyOption.ATOMIC_MOVE);
				}
				catch (AtomicMoveNotSupportedException e) {
					Files.move(stagingRoot, extractionRoot);
				}
				return new PublishedExtraction(extractionRoot, true);
			}
			catch (FileAlreadyExistsException e) {
				// Another publisher won the race; validate that path on the next iteration.
			}
		}
	}

	private boolean isMatchingExtraction(Path extractionRoot, String expectedDigest) {
		if (!Files.isDirectory(extractionRoot)) {
			return false;
		}
		try {
			return expectedDigest.equals(calculateExtractionDigest(extractionRoot));
		}
		catch (IOException e) {
			logger.warn("Failed to validate existing Spring Boot skill extraction directory {}: {}", extractionRoot,
					e.getMessage());
			return false;
		}
	}

	private String calculateExtractionDigest(Path extractionRoot) throws IOException {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is not available", e);
		}

		try (Stream<Path> paths = Files.walk(extractionRoot)) {
			for (Path path : paths.filter(Files::isRegularFile)
				.sorted(Comparator.comparing(file -> extractionRoot.relativize(file).toString()))
				.toList()) {
				byte[] relativePath = extractionRoot.relativize(path)
					.toString()
					.replace('\\', '/')
					.getBytes(StandardCharsets.UTF_8);
				digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(relativePath.length).array());
				digest.update(relativePath);
				digest.update(ByteBuffer.allocate(Long.BYTES).putLong(Files.size(path)).array());
				try (InputStream inputStream = Files.newInputStream(path)) {
					byte[] buffer = new byte[8192];
					int bytesRead;
					while ((bytesRead = inputStream.read(buffer)) != -1) {
						digest.update(buffer, 0, bytesRead);
					}
				}
			}
		}
		return HexFormat.of().formatHex(digest.digest());
	}

	private boolean containsSkillManifest(List<ResolvedSkillResource> resources) {
		return resources.stream()
			.anyMatch(resource -> resource.relativePath().getNameCount() == 2
					&& "SKILL.md".equals(resource.relativePath().getFileName().toString()));
	}

	private Map<String, Integer> getClasspathRootOrder(PathMatchingResourcePatternResolver resolver) throws IOException {
		Map<String, Integer> rootOrder = new LinkedHashMap<>();
		for (Resource root : resolver.getResources("classpath*:" + classpathPath)) {
			String rootUrl = getDecodedResourceUrl(root);
			while (rootUrl.endsWith("/")) {
				rootUrl = rootUrl.substring(0, rootUrl.length() - 1);
			}
			rootOrder.putIfAbsent(rootUrl, rootOrder.size());
		}
		return rootOrder;
	}

	private ResolvedResourcePath getResolvedResourcePath(Resource resource) throws IOException {
		String decodedResourceUrl = getDecodedResourceUrl(resource);
		String marker = "/" + classpathPath + "/";
		int archiveRootIndex = decodedResourceUrl.lastIndexOf("!/");
		int markerIndex = decodedResourceUrl.indexOf(marker, archiveRootIndex >= 0 ? archiveRootIndex + 1 : 0);
		if (markerIndex < 0) {
			logger.debug("Unable to determine relative path for classpath skill resource: {}", resource);
			return null;
		}

		String sourceRoot = decodedResourceUrl.substring(0, markerIndex + marker.length() - 1);
		Path relativePath = Path.of(decodedResourceUrl.substring(markerIndex + marker.length())).normalize();
		return new ResolvedResourcePath(sourceRoot, relativePath);
	}

	private String getDecodedResourceUrl(Resource resource) throws IOException {
		String resourceUrl = resource.getURL().toExternalForm();
		int queryIndex = resourceUrl.indexOf('?');
		if (queryIndex >= 0) {
			resourceUrl = resourceUrl.substring(0, queryIndex);
		}

		try {
			return StringUtils.uriDecode(resourceUrl, StandardCharsets.UTF_8);
		}
		catch (IllegalArgumentException e) {
			throw new IOException("Invalid classpath skill resource URL: " + resourceUrl, e);
		}
	}

	private void deleteDirectoryRecursively(Path directory) throws IOException {
		if (!Files.exists(directory)) {
			return;
		}

		try (Stream<Path> paths = Files.walk(directory)) {
			for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.deleteIfExists(path);
			}
		}
	}

	private record ResolvedResourcePath(String sourceRoot, Path relativePath) {
	}

	private record ResolvedSkillResource(Resource resource, Path relativePath) {
	}

	private record PublishedExtraction(Path path, boolean created) {
	}

	FileSystem getOrCreateJarFileSystem(URI uri) throws IOException {
		if (jarFileSystem != null && jarFileSystem.isOpen()) {
			return jarFileSystem;
		}
		try {
			jarFileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
			ownsJarFileSystem = true;
		}
		catch (FileSystemAlreadyExistsException ex) {
			jarFileSystem = FileSystems.getFileSystem(uri);
			ownsJarFileSystem = false;
		}
		return jarFileSystem;
	}

	/**
	 * Scans a directory (filesystem or JAR) for skills.
	 * This method works with both regular filesystem paths and JAR FileSystem paths.
	 *
	 * @param skillsPath the path to the skills directory
	 * @param source the source identifier for the skills
	 * @return list of discovered skills
	 */
	private List<SkillMetadata> scanClasspathDirectory(Path skillsPath, String source) {
		List<SkillMetadata> skills = new ArrayList<>();

		if (!Files.exists(skillsPath)) {
			logger.debug("Classpath skills directory does not exist: {}", skillsPath);
			return skills;
		}

		if (!Files.isDirectory(skillsPath)) {
			logger.debug("Classpath skills path is not a directory: {}", skillsPath);
			return skills;
		}

		boolean isJarPath = (jarFileSystem != null && skillsPath.getFileSystem() == jarFileSystem);

		try (var stream = Files.list(skillsPath)) {
			stream.filter(Files::isDirectory)
					.forEach(skillDir -> {
						try {
							SkillMetadata metadata = scanner.loadSkill(skillDir, source);
							if (metadata != null) {
								// Copy skill resources (references, scripts, assets, etc.) to basePath
								Path targetSkillPath = copySkillResources(skillDir, metadata.getName(), isJarPath);

								// Update skillPath to point to the copied location in basePath
								if (targetSkillPath != null) {
									metadata.setSkillPath(targetSkillPath.toString());
								}

								// For JAR resources, cache the content that was already loaded by scanner.loadSkill()
								// because Path.of() cannot access JAR filesystem paths
								if (isJarPath) {
									String fullContent = metadata.getFullContent();
									if (fullContent != null) {
										jarSkillContentCache.put(metadata.getName(), fullContent);
									}
								}
								skills.add(metadata);
								logger.debug("Loaded skill from classpath: {} from {}", metadata.getName(), skillDir);
							}
						}
						catch (Exception e) {
							logger.error("Failed to load skill from classpath {}: {}", skillDir, e.getMessage(), e);
						}
					});
		}
		catch (IOException e) {
			logger.error("Failed to scan classpath skills directory {}: {}", skillsPath, e.getMessage(), e);
		}

		return skills;
	}

	/**
	 * Copies skill resources (SKILL.md and subdirectories like references, scripts, assets)
	 * from classpath to basePath, maintaining the same directory structure.
	 *
	 * @param skillDir the source skill directory (from classpath, may be JAR or filesystem)
	 * @param skillName the skill name (e.g., "web-research/web-research")
	 * @param isJarPath whether the source is from a JAR file
	 * @return the target skill directory path in basePath, or null if copy failed
	 */
	private Path copySkillResources(Path skillDir, String skillName, boolean isJarPath) {
		try {
			// Extract the skill directory name from skillName (format: "dirName/name")
			String skillDirName = skillName.contains("/")
					? skillName.substring(0, skillName.indexOf("/"))
					: skillName;

			// Create target path: basePath/classpathPath/skillDirName
			Path targetSkillPath = basePath.resolve(classpathPath).resolve(skillDirName);
			Files.createDirectories(targetSkillPath);

			if (isJarPath) {
				// For JAR resources, use InputStream to copy files
				copySkillResourcesFromJar(skillDir, targetSkillPath);
			}
			else {
				// For filesystem resources, use Files.copy
				copySkillResourcesFromFilesystem(skillDir, targetSkillPath);
			}

			logger.debug("Copied skill resources from {} to {}", skillDir, targetSkillPath);
			return targetSkillPath;
		}
		catch (Exception e) {
			logger.warn("Failed to copy skill resources from {}: {}", skillDir, e.getMessage());
			return null;
		}
	}

	/**
	 * Copies skill resources from JAR filesystem to target path using InputStream.
	 */
	private void copySkillResourcesFromJar(Path sourceSkillDir, Path targetSkillPath) throws IOException {
		// Copy all files and subdirectories from sourceSkillDir
		try (Stream<Path> entries = Files.list(sourceSkillDir)) {
			entries.forEach(entry -> {
				try {
					String entryName = entry.getFileName().toString();
					Path targetEntry = targetSkillPath.resolve(entryName);

					if (Files.isDirectory(entry)) {
						// Copy subdirectories (references, scripts, assets, etc.)
						Files.createDirectories(targetEntry);
						copyDirectoryFromJar(entry, targetEntry);
					}
					else {
						// Copy files (SKILL.md and any other files in root)
						Files.createDirectories(targetEntry.getParent());
						try (InputStream is = Files.newInputStream(entry)) {
							Files.copy(is, targetEntry, StandardCopyOption.REPLACE_EXISTING);
						}
					}
				}
				catch (IOException e) {
					logger.warn("Failed to copy entry {}: {}", entry, e.getMessage());
				}
			});
		}
	}

	/**
	 * Recursively copies a directory from JAR filesystem to target path.
	 */
	private void copyDirectoryFromJar(Path sourceDir, Path targetDir) throws IOException {
		try (Stream<Path> entries = Files.walk(sourceDir)) {
			entries.forEach(sourcePath -> {
				try {
					Path relativePath = sourceDir.relativize(sourcePath);
					Path targetPath = targetDir.resolve(relativePath);

					if (Files.isDirectory(sourcePath)) {
						Files.createDirectories(targetPath);
					}
					else {
						Files.createDirectories(targetPath.getParent());
						try (InputStream is = Files.newInputStream(sourcePath)) {
							Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
						}
					}
				}
				catch (IOException e) {
					logger.warn("Failed to copy file {}: {}", sourcePath, e.getMessage());
				}
			});
		}
	}

	/**
	 * Copies skill resources from filesystem to target path using Files.copy.
	 */
	private void copySkillResourcesFromFilesystem(Path sourceSkillDir, Path targetSkillPath) throws IOException {
		// Copy all files and subdirectories from sourceSkillDir
		try (Stream<Path> entries = Files.list(sourceSkillDir)) {
			entries.forEach(entry -> {
				try {
					String entryName = entry.getFileName().toString();
					Path targetEntry = targetSkillPath.resolve(entryName);

					if (Files.isDirectory(entry)) {
						// Copy subdirectories (references, scripts, assets, etc.)
						copyDirectoryFromFilesystem(entry, targetEntry);
					}
					else {
						// Copy files (SKILL.md and any other files in root)
						Files.copy(entry, targetEntry, StandardCopyOption.REPLACE_EXISTING);
					}
				}
				catch (IOException e) {
					logger.warn("Failed to copy entry {}: {}", entry, e.getMessage());
				}
			});
		}
	}

	/**
	 * Recursively copies a directory from filesystem to target path.
	 */
	private void copyDirectoryFromFilesystem(Path sourceDir, Path targetDir) throws IOException {
		try (Stream<Path> entries = Files.walk(sourceDir)) {
			entries.forEach(sourcePath -> {
				try {
					Path relativePath = sourceDir.relativize(sourcePath);
					Path targetPath = targetDir.resolve(relativePath);

					if (Files.isDirectory(sourcePath)) {
						Files.createDirectories(targetPath);
					}
					else {
						Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
					}
				}
				catch (IOException e) {
					logger.warn("Failed to copy file {}: {}", sourcePath, e.getMessage());
				}
			});
		}
	}

	@Override
	public synchronized void reload() {
		logger.info("Reloading skills from classpath...");
		loadSkillsToRegistry();
	}

	@Override
	public String readSkillContent(String name) throws IOException {
		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException("Skill name cannot be null or empty");
		}

		// Get the skill by name
		Optional<SkillMetadata> skillOpt = get(name);
		if (skillOpt.isEmpty()) {
			throw new IllegalStateException("Skill not found: " + name);
		}

		SkillMetadata skill = skillOpt.get();

		// Check if this skill is from JAR (cached content available)
		String cachedContent = jarSkillContentCache.get(name);
		if (cachedContent != null) {
			return cachedContent;
		}

		// For filesystem classpath resources, use the normal loadFullContent method
		return skill.loadFullContent();
	}

	@Override
	public String getSkillLoadInstructions() {
		return "**Skill Location:**\n"
				+ String.format("- **Classpath Skills**: `classpath:%s`\n", classpathPath)
				+ "\n"
				+ "**Skill Path Format:**\n"
				+ "Each skill has a unique id shown in the skill list above. "
				+ "Skill ids identify registry entries and are not direct tool names. "
				+ "Use the exact id shown when calling `read_skill` to read the SKILL.md file.\n";
	}

	@Override
	public String getRegistryType() {
		return "Classpath";
	}

	@Override
	public SystemPromptTemplate getSystemPromptTemplate() {
		return systemPromptTemplate;
	}

	/**
	 * Closes the JAR FileSystem if it was created and removes managed extraction
	 * generations.
	 * Should be called when the registry is no longer needed.
	 */
	public synchronized void close() {
		if (ownsJarFileSystem && jarFileSystem != null) {
			try {
				jarFileSystem.close();
			}
			catch (IOException e) {
				logger.warn("Failed to close JAR filesystem: {}", e.getMessage());
			}
		}
		jarFileSystem = null;
		ownsJarFileSystem = false;

		if (bootExtractionWorkspace != null) {
			try {
				deleteDirectoryRecursively(bootExtractionWorkspace);
			}
			catch (IOException e) {
				logger.warn("Failed to remove Spring Boot skill extraction workspace {}: {}", bootExtractionWorkspace,
						e.getMessage());
			}
		}
		bootExtractionWorkspace = null;
	}

	/**
	 * Builder for creating ClasspathSkillRegistry instances.
	 */
	public static class Builder {
		private String classpathPath;
		private String basePath;
		private boolean autoLoad = true;
		private SystemPromptTemplate systemPromptTemplate;

		/**
		 * Sets the classpath path for skills.
		 * <p><b>Optional</b>: If not set, defaults to <code>skills</code>
		 *
		 * @param classpathPath the classpath path (e.g., "skills", "custom/skills")
		 * @return this builder
		 */
		public Builder classpathPath(String classpathPath) {
			this.classpathPath = classpathPath;
			return this;
		}

		/**
		 * Sets the base path for storing copied skill resources.
		 * <p><b>Optional</b>: If not set, defaults to <code>/tmp</code>
		 *
		 * @param basePath the base path for storing skill resources (e.g., "/tmp", "/var/skills")
		 * @return this builder
		 */
		public Builder basePath(String basePath) {
			this.basePath = basePath;
			return this;
		}

		/**
		 * Sets whether to automatically load skills during initialization.
		 * <p><b>Optional</b>: Defaults to <code>true</code>
		 *
		 * @param autoLoad true to auto-load skills, false to skip auto-loading
		 * @return this builder
		 */
		public Builder autoLoad(boolean autoLoad) {
			this.autoLoad = autoLoad;
			return this;
		}

		/**
		 * Sets a custom system prompt template for skills.
		 * <p><b>Optional</b>: If not set, uses the default template for ClasspathSkillRegistry.
		 *
		 * @param systemPromptTemplate the custom SystemPromptTemplate to use
		 * @return this builder
		 */
		public Builder systemPromptTemplate(SystemPromptTemplate systemPromptTemplate) {
			this.systemPromptTemplate = systemPromptTemplate;
			return this;
		}

		/**
		 * Sets a custom system prompt template from a template string.
		 * <p><b>Optional</b>: If not set, uses the default template for ClasspathSkillRegistry.
		 *
		 * @param template the template string
		 * @return this builder
		 */
		public Builder systemPromptTemplate(String template) {
			this.systemPromptTemplate = SystemPromptTemplate.builder()
					.template(template)
					.build();
			return this;
		}

		/**
		 * Builds the ClasspathSkillRegistry instance with the configured parameters.
		 *
		 * @return a new ClasspathSkillRegistry instance
		 */
		public ClasspathSkillRegistry build() {
			return new ClasspathSkillRegistry(this);
		}
	}
}
