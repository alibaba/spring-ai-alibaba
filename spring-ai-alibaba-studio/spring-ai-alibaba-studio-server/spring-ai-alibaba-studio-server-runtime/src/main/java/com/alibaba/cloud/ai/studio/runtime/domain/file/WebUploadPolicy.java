/*
* Copyright 2024 the original author or authors.
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

package com.alibaba.cloud.ai.studio.runtime.domain.file;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.alibaba.cloud.ai.studio.runtime.enums.UploadType;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Title web upload policy for oss.<br>
 * Description //TODO.<br>
 *
 * @since 1.0.0.3
 */

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class WebUploadPolicy extends UploadPolicy {

	/** oss access id */
	@JsonProperty("access_id")
	private String accessId;

	/** oss policy */
	private String policy;

	/** oss signature */
	private String host;

	/** oss expire */
	private Long expire;

	/** oss signature */
	private String signature;

	/** oss security token */
	@JsonProperty("security_token")
	private String securityToken;

	/** upload file */
	@JsonProperty("upload_type")
	@Builder.Default
	private UploadType uploadType = UploadType.OSS;

}
