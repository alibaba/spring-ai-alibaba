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

package com.alibaba.cloud.ai.studio.admin.builder.controller;

import com.alibaba.cloud.ai.studio.core.base.service.SkillService;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.runtime.domain.skill.Skill;
import com.alibaba.cloud.ai.studio.runtime.domain.skill.SkillFileContent;
import com.alibaba.cloud.ai.studio.runtime.domain.skill.SkillFileNode;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

/**
 * REST APIs for agent skill management.
 *
 * @since 1.0.0.3
 */
@RestController
@Tag(name = "skill")
@RequestMapping("/console/v1/skills")
public class SkillController {

	private final SkillService skillService;

	public SkillController(SkillService skillService) {
		this.skillService = skillService;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Result<String> createSkill(@RequestPart("file") MultipartFile file,
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "description", required = false) String description) {
		RequestContext context = RequestContextHolder.getRequestContext();
		String skillId = skillService.createSkill(file, name, description);
		return Result.success(context.getRequestId(), skillId);
	}

	@PutMapping(value = "/{skillId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Result<Void> updateSkill(@PathVariable("skillId") String skillId,
			@RequestPart(value = "file", required = false) MultipartFile file,
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "description", required = false) String description) {
		RequestContext context = RequestContextHolder.getRequestContext();
		if (StringUtils.isBlank(skillId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("skillId"));
		}
		skillService.updateSkill(skillId, file, name, description);
		return Result.success(context.getRequestId(), null);
	}

	@DeleteMapping("/{skillId}")
	public Result<Void> deleteSkill(@PathVariable("skillId") String skillId) {
		RequestContext context = RequestContextHolder.getRequestContext();
		if (Objects.isNull(skillId)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("skillId"));
		}
		skillService.deleteSkill(skillId);
		return Result.success(context.getRequestId(), null);
	}

	@GetMapping("/{skillId}")
	public Result<Skill> getSkill(@PathVariable("skillId") String skillId) {
		RequestContext context = RequestContextHolder.getRequestContext();
		Skill skill = skillService.getSkill(skillId);
		return Result.success(context.getRequestId(), skill);
	}

	@GetMapping("/{skillId}/files")
	public Result<List<SkillFileNode>> listSkillFiles(@PathVariable("skillId") String skillId) {
		RequestContext context = RequestContextHolder.getRequestContext();
		List<SkillFileNode> files = skillService.listSkillFiles(skillId);
		return Result.success(context.getRequestId(), files);
	}

	@GetMapping("/{skillId}/file")
	public Result<SkillFileContent> readSkillFile(@PathVariable("skillId") String skillId,
			@RequestParam("path") String path) {
		RequestContext context = RequestContextHolder.getRequestContext();
		if (StringUtils.isBlank(path)) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("path"));
		}
		SkillFileContent content = skillService.readSkillFile(skillId, path);
		return Result.success(context.getRequestId(), content);
	}

	@GetMapping
	public Result<PagingList<Skill>> listSkills(@ModelAttribute BaseQuery query) {
		RequestContext context = RequestContextHolder.getRequestContext();
		PagingList<Skill> skills = skillService.listSkills(query);
		return Result.success(context.getRequestId(), skills);
	}

	@PostMapping("/query-by-ids")
	public Result<List<Skill>> queryByIds(@RequestBody List<String> skillIds) {
		RequestContext context = RequestContextHolder.getRequestContext();
		List<Skill> skills = skillService.getSkills(skillIds);
		return Result.success(context.getRequestId(), skills);
	}

}
