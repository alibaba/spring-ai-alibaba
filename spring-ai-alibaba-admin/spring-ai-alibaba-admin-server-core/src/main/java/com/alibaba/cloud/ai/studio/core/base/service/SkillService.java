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

package com.alibaba.cloud.ai.studio.core.base.service;

import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.alibaba.cloud.ai.studio.runtime.domain.PagingList;
import com.alibaba.cloud.ai.studio.runtime.domain.skill.Skill;
import com.alibaba.cloud.ai.studio.runtime.domain.skill.SkillFileContent;
import com.alibaba.cloud.ai.studio.runtime.domain.skill.SkillFileNode;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service for managing agent skills.
 *
 * @since 1.0.0.3
 */
public interface SkillService {

	String createSkill(MultipartFile file, String name, String description);

	void updateSkill(String skillId, MultipartFile file, String name, String description);

	void deleteSkill(String skillId);

	Skill getSkill(String skillId);

	PagingList<Skill> listSkills(BaseQuery query);

	List<Skill> getSkills(List<String> skillIds);

	List<SkillFileNode> listSkillFiles(String skillId);

	SkillFileContent readSkillFile(String skillId, String relativePath);

}
