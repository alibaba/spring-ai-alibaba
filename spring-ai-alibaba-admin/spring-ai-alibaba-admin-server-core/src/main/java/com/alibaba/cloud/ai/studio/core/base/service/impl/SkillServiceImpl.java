/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.studio.core.base.service.impl;

import com.alibaba.cloud.ai.studio.core.agent.skill.SkillMdParser;
import com.alibaba.cloud.ai.studio.core.agent.skill.SkillPackageUtils;
import com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants;
import com.alibaba.cloud.ai.studio.core.base.entity.SkillEntity;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.base.mapper.SkillMapper;
import com.alibaba.cloud.ai.studio.core.base.service.SkillService;
import com.alibaba.cloud.ai.studio.core.config.StudioProperties;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.utils.common.IdGenerator;
import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.skill.Skill;
import com.alibaba.cloud.ai.studio.runtime.domain.skill.SkillFileContent;
import com.alibaba.cloud.ai.studio.runtime.domain.skill.SkillFileNode;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.enums.SkillStatus;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Skill service implementation.
 *
 * @since 1.0.0.3
 */
@Service
public class SkillServiceImpl extends ServiceImpl<SkillMapper, SkillEntity> implements SkillService {

	private final StudioProperties studioProperties;

	private final RedisManager redisManager;

	public SkillServiceImpl(StudioProperties studioProperties, RedisManager redisManager) {
		this.studioProperties = studioProperties;
		this.redisManager = redisManager;
	}

	@Override
	public String createSkill(MultipartFile file, String name, String description) {
		try {
			RequestContext context = RequestContextHolder.getRequestContext();
			validateZip(file);

			String skillId = IdGenerator.idStr();
			Path extractRoot = buildExtractRoot(context, skillId);
			SkillMdParser.ParsedSkillMd parsed = unpackAndParse(file, extractRoot);

			String displayName = StringUtils.defaultIfBlank(name, parsed.getName());
			String displayDesc = StringUtils.defaultIfBlank(description, parsed.getDescription());

			if (getBySkillName(context.getWorkspaceId(), parsed.getName()) != null) {
				SkillPackageUtils.deleteRecursively(extractRoot);
				throw new BizException(ErrorCode.SKILL_NAME_EXISTS.toError());
			}
			if (getByDisplayName(context.getWorkspaceId(), displayName) != null) {
				SkillPackageUtils.deleteRecursively(extractRoot);
				throw new BizException(ErrorCode.SKILL_NAME_EXISTS.toError());
			}

			SkillEntity entity = new SkillEntity();
			entity.setSkillId(skillId);
			entity.setWorkspaceId(context.getWorkspaceId());
			entity.setName(displayName);
			entity.setDescription(displayDesc);
			entity.setSkillName(parsed.getName());
			entity.setStoragePath(toRelativeStoragePath(extractRoot));
			entity.setStatus(SkillStatus.NORMAL);
			entity.setSource("user");
			entity.setGmtCreate(new Date());
			entity.setGmtModified(new Date());
			entity.setCreator(context.getAccountId());
			entity.setModifier(context.getAccountId());
			this.save(entity);

			redisManager.put(cacheKey(entity.getWorkspaceId(), entity.getSkillId()), entity);
			return skillId;
		}
		catch (BizException e) {
			throw e;
		}
		catch (Exception e) {
			throw new BizException(ErrorCode.CREATE_SKILL_ERROR.toError(), e);
		}
	}

	@Override
	public void updateSkill(String skillId, MultipartFile file, String name, String description) {
		try {
			RequestContext context = RequestContextHolder.getRequestContext();
			SkillEntity entity = getEntityById(context.getWorkspaceId(), skillId);
			if (entity == null) {
				throw new BizException(ErrorCode.SKILL_NOT_FOUND.toError());
			}

			if (file != null && !file.isEmpty()) {
				validateZip(file);
				Path extractRoot = resolveAbsolutePath(entity.getStoragePath());
				SkillPackageUtils.deleteRecursively(extractRoot);
				SkillMdParser.ParsedSkillMd parsed = unpackAndParse(file, extractRoot);

				SkillEntity conflict = getBySkillName(context.getWorkspaceId(), parsed.getName());
				if (conflict != null && !Objects.equals(conflict.getId(), entity.getId())) {
					throw new BizException(ErrorCode.SKILL_NAME_EXISTS.toError());
				}
				entity.setSkillName(parsed.getName());
				if (StringUtils.isBlank(name)) {
					entity.setName(parsed.getName());
				}
				if (StringUtils.isBlank(description)) {
					entity.setDescription(parsed.getDescription());
				}
			}

			if (StringUtils.isNotBlank(name)) {
				SkillEntity conflict = getByDisplayName(context.getWorkspaceId(), name);
				if (conflict != null && !Objects.equals(conflict.getId(), entity.getId())) {
					throw new BizException(ErrorCode.SKILL_NAME_EXISTS.toError());
				}
				entity.setName(name);
			}
			if (description != null) {
				entity.setDescription(description);
			}

			entity.setGmtModified(new Date());
			entity.setModifier(context.getAccountId());
			this.updateById(entity);
			redisManager.put(cacheKey(entity.getWorkspaceId(), entity.getSkillId()), entity);
		}
		catch (BizException e) {
			throw e;
		}
		catch (Exception e) {
			throw new BizException(ErrorCode.UPDATE_SKILL_ERROR.toError(), e);
		}
	}

	@Override
	public void deleteSkill(String skillId) {
		RequestContext context = RequestContextHolder.getRequestContext();
		SkillEntity entity = getEntityById(context.getWorkspaceId(), skillId);
		if (entity == null) {
			throw new BizException(ErrorCode.SKILL_NOT_FOUND.toError());
		}
		entity.setStatus(SkillStatus.DELETED);
		entity.setGmtModified(new Date());
		entity.setModifier(context.getAccountId());
		this.updateById(entity);
		redisManager.delete(cacheKey(entity.getWorkspaceId(), entity.getSkillId()));
	}

	@Override
	public Skill getSkill(String skillId) {
		RequestContext context = RequestContextHolder.getRequestContext();
		SkillEntity entity = getEntityById(context.getWorkspaceId(), skillId);
		if (entity == null) {
			throw new BizException(ErrorCode.SKILL_NOT_FOUND.toError());
		}
		return toDto(entity);
	}

	@Override
	public PagingList<Skill> listSkills(BaseQuery query) {
		RequestContext context = RequestContextHolder.getRequestContext();
		LambdaQueryWrapper<SkillEntity> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(SkillEntity::getWorkspaceId, context.getWorkspaceId());
		wrapper.ne(SkillEntity::getStatus, SkillStatus.DELETED.getStatus());
		if (StringUtils.isNotBlank(query.getName())) {
			wrapper.and(w -> w.like(SkillEntity::getName, query.getName())
				.or()
				.like(SkillEntity::getSkillName, query.getName()));
		}
		wrapper.orderByDesc(SkillEntity::getId);

		Page<SkillEntity> page = new Page<>(query.getCurrent(), query.getSize());
		IPage<SkillEntity> pageResult = this.page(page, wrapper);
		List<Skill> skills = CollectionUtils.isEmpty(pageResult.getRecords()) ? new ArrayList<>()
				: pageResult.getRecords().stream().map(this::toDto).toList();
		return new PagingList<>(query.getCurrent(), query.getSize(), pageResult.getTotal(), skills);
	}

	@Override
	public List<Skill> getSkills(List<String> skillIds) {
		if (CollectionUtils.isEmpty(skillIds)) {
			return List.of();
		}
		RequestContext context = RequestContextHolder.getRequestContext();
		LambdaQueryWrapper<SkillEntity> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(SkillEntity::getWorkspaceId, context.getWorkspaceId());
		wrapper.in(SkillEntity::getSkillId, skillIds);
		wrapper.ne(SkillEntity::getStatus, SkillStatus.DELETED.getStatus());
		List<SkillEntity> entities = this.list(wrapper);
		return entities.stream().map(this::toDto).toList();
	}

	@Override
	public List<SkillFileNode> listSkillFiles(String skillId) {
		RequestContext context = RequestContextHolder.getRequestContext();
		SkillEntity entity = getEntityById(context.getWorkspaceId(), skillId);
		if (entity == null) {
			throw new BizException(ErrorCode.SKILL_NOT_FOUND.toError());
		}
		Path root = resolveAbsolutePath(entity.getStoragePath()).toAbsolutePath().normalize();
		if (!Files.isDirectory(root)) {
			throw new BizException(ErrorCode.SKILL_PACKAGE_INVALID.toError("skill storage directory not found"));
		}
		try {
			return buildFileTree(root, root);
		}
		catch (IOException e) {
			throw new BizException(ErrorCode.SYSTEM_ERROR.toError(), e);
		}
	}

	@Override
	public SkillFileContent readSkillFile(String skillId, String relativePath) {
		RequestContext context = RequestContextHolder.getRequestContext();
		SkillEntity entity = getEntityById(context.getWorkspaceId(), skillId);
		if (entity == null) {
			throw new BizException(ErrorCode.SKILL_NOT_FOUND.toError());
		}
		if (StringUtils.isBlank(relativePath) || relativePath.contains("..")) {
			throw new BizException(ErrorCode.INVALID_PARAMS.toError("path", "invalid relative path"));
		}
		Path root = resolveAbsolutePath(entity.getStoragePath()).toAbsolutePath().normalize();
		Path target = root.resolve(relativePath).normalize();
		if (!target.startsWith(root) || !Files.isRegularFile(target)) {
			throw new BizException(ErrorCode.FILE_NOT_FOUND.toError());
		}
		try {
			long size = Files.size(target);
			boolean binary = isBinaryFile(target);
			String content = binary ? null : Files.readString(target, StandardCharsets.UTF_8);
			String contentType = Files.probeContentType(target);
			return SkillFileContent.builder()
				.path(relativePath.replace('\\', '/'))
				.content(content)
				.binary(binary)
				.contentType(contentType)
				.size(size)
				.build();
		}
		catch (IOException e) {
			throw new BizException(ErrorCode.SYSTEM_ERROR.toError(), e);
		}
	}

	private List<SkillFileNode> buildFileTree(Path root, Path current) throws IOException {
		List<SkillFileNode> nodes = new ArrayList<>();
		try (Stream<Path> stream = Files.list(current)) {
			List<Path> children = stream.sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
				.toList();
			for (Path child : children) {
				String relative = root.relativize(child).toString().replace('\\', '/');
				if (Files.isDirectory(child)) {
					nodes.add(SkillFileNode.builder()
						.name(child.getFileName().toString())
						.path(relative)
						.type("directory")
						.children(buildFileTree(root, child))
						.build());
				}
				else if (Files.isRegularFile(child)) {
					nodes.add(SkillFileNode.builder()
						.name(child.getFileName().toString())
						.path(relative)
						.type("file")
						.size(Files.size(child))
						.build());
				}
			}
		}
		return nodes;
	}

	private boolean isBinaryFile(Path file) throws IOException {
		String name = file.getFileName().toString().toLowerCase();
		if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif")
				|| name.endsWith(".webp") || name.endsWith(".pdf") || name.endsWith(".zip") || name.endsWith(".gz")
				|| name.endsWith(".jar") || name.endsWith(".class") || name.endsWith(".exe") || name.endsWith(".so")
				|| name.endsWith(".dylib") || name.endsWith(".bin")) {
			return true;
		}
		try (InputStream in = Files.newInputStream(file)) {
			byte[] sample = in.readNBytes(8000);
			for (byte b : sample) {
				if (b == 0) {
					return true;
				}
			}
		}
		return false;
	}

	private SkillMdParser.ParsedSkillMd unpackAndParse(MultipartFile file, Path extractRoot) throws Exception {
		try (InputStream in = file.getInputStream()) {
			SkillPackageUtils.extractZip(in, extractRoot);
		}
		Path skillRoot;
		try {
			skillRoot = SkillPackageUtils.resolveSkillRoot(extractRoot);
		}
		catch (IOException e) {
			SkillPackageUtils.deleteRecursively(extractRoot);
			throw new BizException(ErrorCode.SKILL_PACKAGE_INVALID.toError(e.getMessage()));
		}
		// If nested, move contents up so storage_path points at skill root consistently
		if (!skillRoot.equals(extractRoot)) {
			Path staging = extractRoot.resolveSibling(extractRoot.getFileName() + "_staging");
			SkillPackageUtils.deleteRecursively(staging);
			Files.move(skillRoot, staging);
			SkillPackageUtils.deleteRecursively(extractRoot);
			Files.move(staging, extractRoot);
			skillRoot = extractRoot;
		}
		String content = Files.readString(skillRoot.resolve(SkillPackageUtils.SKILL_MD), StandardCharsets.UTF_8);
		try {
			return SkillMdParser.parse(content);
		}
		catch (IllegalArgumentException e) {
			SkillPackageUtils.deleteRecursively(extractRoot);
			throw new BizException(ErrorCode.SKILL_PACKAGE_INVALID.toError(e.getMessage()));
		}
	}

	private void validateZip(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("file"));
		}
		String ext = FilenameUtils.getExtension(file.getOriginalFilename());
		if (!"zip".equalsIgnoreCase(ext)) {
			throw new BizException(ErrorCode.SKILL_PACKAGE_INVALID.toError("only .zip packages are supported"));
		}
	}

	private Path buildExtractRoot(RequestContext context, String skillId) {
		String relative = "skill" + File.separator + context.getAccountId() + File.separator + context.getWorkspaceId()
				+ File.separator + skillId;
		return Path.of(studioProperties.getStoragePath(), relative);
	}

	private String toRelativeStoragePath(Path absolute) {
		Path storageRoot = Path.of(studioProperties.getStoragePath()).toAbsolutePath().normalize();
		Path abs = absolute.toAbsolutePath().normalize();
		return storageRoot.relativize(abs).toString().replace('\\', '/');
	}

	public Path resolveAbsolutePath(String relativeStoragePath) {
		return Path.of(studioProperties.getStoragePath(), relativeStoragePath);
	}

	private SkillEntity getEntityById(String workspaceId, String skillId) {
		String key = cacheKey(workspaceId, skillId);
		SkillEntity cached = redisManager.get(key);
		if (cached != null) {
			if (Objects.equals(cached.getId(), CacheConstants.CACHE_EMPTY_ID)) {
				return null;
			}
			if (cached.getStatus() == SkillStatus.DELETED) {
				return null;
			}
			return cached;
		}

		LambdaQueryWrapper<SkillEntity> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(SkillEntity::getWorkspaceId, workspaceId);
		wrapper.eq(SkillEntity::getSkillId, skillId);
		wrapper.ne(SkillEntity::getStatus, SkillStatus.DELETED.getStatus());
		SkillEntity entity = this.getOne(wrapper);
		if (entity != null) {
			redisManager.put(key, entity);
		}
		return entity;
	}

	private SkillEntity getBySkillName(String workspaceId, String skillName) {
		LambdaQueryWrapper<SkillEntity> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(SkillEntity::getWorkspaceId, workspaceId);
		wrapper.eq(SkillEntity::getSkillName, skillName);
		wrapper.ne(SkillEntity::getStatus, SkillStatus.DELETED.getStatus());
		return this.getOne(wrapper);
	}

	private SkillEntity getByDisplayName(String workspaceId, String name) {
		LambdaQueryWrapper<SkillEntity> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(SkillEntity::getWorkspaceId, workspaceId);
		wrapper.eq(SkillEntity::getName, name);
		wrapper.ne(SkillEntity::getStatus, SkillStatus.DELETED.getStatus());
		return this.getOne(wrapper);
	}

	private Skill toDto(SkillEntity entity) {
		return Skill.builder()
			.skillId(entity.getSkillId())
			.name(entity.getName())
			.description(entity.getDescription())
			.skillName(entity.getSkillName())
			.storagePath(entity.getStoragePath())
			.source(entity.getSource())
			.gmtCreate(entity.getGmtCreate())
			.gmtModified(entity.getGmtModified())
			.build();
	}

	private String cacheKey(String workspaceId, String skillId) {
		return String.format(CacheConstants.CACHE_SKILL_WORKSPACE_ID_PREFIX, workspaceId, skillId);
	}

}
