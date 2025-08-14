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

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Title web upload request.<br>
 * Description web upload request.<br>
 *
 * @since 1.0.0.3
 */

@Data
public class WebUploadRequest implements Serializable {

	/** file usage category, like document, image, etc */
	private String category;

	/** file list with meta info */
	private List<FileMeta> files;

	@Data
	public static class FileMeta implements Serializable {

		/** file name */
		private String name;

	}

}
