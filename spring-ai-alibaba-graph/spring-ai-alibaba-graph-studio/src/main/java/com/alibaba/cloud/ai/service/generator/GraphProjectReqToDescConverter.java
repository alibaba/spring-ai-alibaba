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
package com.alibaba.cloud.ai.service.generator;

import com.alibaba.cloud.ai.exception.NotImplementedException;
import com.alibaba.cloud.ai.model.AppModeEnum;
import io.spring.initializr.generator.buildsystem.BuildSystem;
import io.spring.initializr.generator.language.Language;
import io.spring.initializr.generator.packaging.Packaging;
import io.spring.initializr.generator.project.ProjectDescription;
import io.spring.initializr.generator.version.Version;
import io.spring.initializr.metadata.InitializrConfiguration;
import io.spring.initializr.metadata.InitializrMetadata;
import io.spring.initializr.metadata.Type;
import io.spring.initializr.metadata.DefaultMetadataElement;
import io.spring.initializr.metadata.Dependency;
import io.spring.initializr.metadata.support.MetadataBuildItemMapper;
import io.spring.initializr.web.project.InvalidProjectRequestException;
import io.spring.initializr.web.project.ProjectRequest;
import io.spring.initializr.web.project.ProjectRequestPlatformVersionTransformer;
import io.spring.initializr.web.project.ProjectRequestToDescriptionConverter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class GraphProjectReqToDescConverter implements ProjectRequestToDescriptionConverter<GraphProjectRequest> {

	private final ProjectRequestPlatformVersionTransformer platformVersionTransformer;

	public GraphProjectReqToDescConverter() {
		this((version, metadata) -> version);
	}

	public GraphProjectReqToDescConverter(ProjectRequestPlatformVersionTransformer platformVersionTransformer) {
		Assert.notNull(platformVersionTransformer, "PlatformVersionTransformer must not be null");
		this.platformVersionTransformer = platformVersionTransformer;
	}

	@Override
	public ProjectDescription convert(GraphProjectRequest request, InitializrMetadata metadata) {
		GraphProjectDescription description = new GraphProjectDescription();
		doConvert(request, description, metadata);
		return description;
	}

	/**
	 * Validate the specified {@link ProjectRequest request} and initialize the specified
	 * {@link ProjectDescription description}. Override any attribute of the description
	 * that are managed by this instance.
	 * @param request the request to validate
	 * @param description the description to initialize
	 * @param metadata the metadata instance to use to apply defaults if necessary
	 */
	public void doConvert(GraphProjectRequest request, GraphProjectDescription description,
			InitializrMetadata metadata) {
		validate(request, metadata);
		Version platformVersion = getPlatformVersion(request, metadata);
		List<Dependency> resolvedDependencies = getResolvedDependencies(request, platformVersion, metadata);
		validateDependencyRange(platformVersion, resolvedDependencies);

		description.setApplicationName(request.getApplicationName());
		description.setArtifactId(cleanInputValue(request.getArtifactId()));
		description.setBaseDirectory(request.getBaseDir());
		description.setBuildSystem(getBuildSystem(request, metadata));
		description.setDescription(request.getDescription());
		description.setGroupId(cleanInputValue(request.getGroupId()));
		description.setLanguage(Language.forId(request.getLanguage(), request.getJavaVersion()));
		description.setName(cleanInputValue(request.getName()));
		description.setPackageName(cleanInputValue(request.getPackageName()));
		description.setPackaging(Packaging.forId(request.getPackaging()));
		description.setPlatformVersion(platformVersion);
		description.setVersion(request.getVersion());
		resolvedDependencies.forEach((dependency) -> description.addDependency(dependency.getId(),
				MetadataBuildItemMapper.toDependency(dependency)));
		// graph related params
		description.setDsl(request.getDsl());
		AppModeEnum appModeEnum = Optional.ofNullable(AppModeEnum.of(request.getAppMode()))
			.orElseThrow(() -> new NotImplementedException("Unsupported appMode: " + request.getAppMode()));
		description.setAppMode(appModeEnum);
	}

	/**
	 * Clean input value to rely on US-ascii character as much as possible.
	 * @param value the input value to clean
	 * @return a value that can be used as part of an identifier
	 */
	protected String cleanInputValue(String value) {
		return StringUtils.hasText(value) ? Normalizer.normalize(value, Normalizer.Form.NFKD).replaceAll("\\p{M}", "")
				: value;
	}

	private void validate(ProjectRequest request, InitializrMetadata metadata) {
		validatePlatformVersion(request, metadata);
		validateType(request.getType(), metadata);
		validateLanguage(request.getLanguage(), metadata);
		validatePackaging(request.getPackaging(), metadata);
		validateDependencies(request, metadata);
	}

	private void validatePlatformVersion(ProjectRequest request, InitializrMetadata metadata) {
		Version platformVersion = Version.safeParse(request.getBootVersion());
		InitializrConfiguration.Platform platform = metadata.getConfiguration().getEnv().getPlatform();
		if (platformVersion != null && !platform.isCompatibleVersion(platformVersion)) {
			throw new InvalidProjectRequestException("Invalid Spring Boot version '" + platformVersion
					+ "', Spring Boot compatibility range is " + platform.determineCompatibilityRangeRequirement());
		}
	}

	private void validateType(String type, InitializrMetadata metadata) {
		if (type != null) {
			Type typeFromMetadata = metadata.getTypes().get(type);
			if (typeFromMetadata == null) {
				throw new InvalidProjectRequestException("Unknown type '" + type + "' check project metadata");
			}
			if (!typeFromMetadata.getTags().containsKey("build")) {
				throw new InvalidProjectRequestException(
						"Invalid type '" + type + "' (missing build tag) check project metadata");
			}
		}
	}

	private void validateLanguage(String language, InitializrMetadata metadata) {
		if (language != null) {
			DefaultMetadataElement languageFromMetadata = metadata.getLanguages().get(language);
			if (languageFromMetadata == null) {
				throw new InvalidProjectRequestException("Unknown language '" + language + "' check project metadata");
			}
		}
	}

	private void validatePackaging(String packaging, InitializrMetadata metadata) {
		if (packaging != null) {
			DefaultMetadataElement packagingFromMetadata = metadata.getPackagings().get(packaging);
			if (packagingFromMetadata == null) {
				throw new InvalidProjectRequestException(
						"Unknown packaging '" + packaging + "' check project metadata");
			}
		}
	}

	private void validateDependencies(ProjectRequest request, InitializrMetadata metadata) {
		List<String> dependencies = request.getDependencies();
		dependencies.forEach((dep) -> {
			Dependency dependency = metadata.getDependencies().get(dep);
			if (dependency == null) {
				throw new InvalidProjectRequestException("Unknown dependency '" + dep + "' check project metadata");
			}
		});
	}

	private void validateDependencyRange(Version platformVersion, List<Dependency> resolvedDependencies) {
		resolvedDependencies.forEach((dep) -> {
			if (!dep.match(platformVersion)) {
				throw new InvalidProjectRequestException(
						"Dependency '" + dep.getId() + "' is not compatible " + "with Spring Boot " + platformVersion);
			}
		});
	}

	private BuildSystem getBuildSystem(ProjectRequest request, InitializrMetadata metadata) {
		Map<String, String> typeTags = metadata.getTypes().get(request.getType()).getTags();
		String id = typeTags.get("build");
		String dialect = typeTags.get("dialect");
		return BuildSystem.forIdAndDialect(id, dialect);
	}

	private Version getPlatformVersion(ProjectRequest request, InitializrMetadata metadata) {
		String versionText = (request.getBootVersion() != null) ? request.getBootVersion()
				: metadata.getBootVersions().getDefault().getId();
		Version version = Version.parse(versionText);
		return this.platformVersionTransformer.transform(version, metadata);
	}

	private List<Dependency> getResolvedDependencies(ProjectRequest request, Version platformVersion,
			InitializrMetadata metadata) {
		List<String> depIds = request.getDependencies();
		return depIds.stream().map((it) -> {
			Dependency dependency = metadata.getDependencies().get(it);
			return dependency.resolve(platformVersion);
		}).collect(Collectors.toList());
	}

}
