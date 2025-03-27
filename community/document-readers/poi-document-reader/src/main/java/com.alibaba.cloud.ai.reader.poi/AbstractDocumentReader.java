package com.alibaba.cloud.ai.reader.poi;

import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * Abstract class for document readers providing common functionality.
 */
public abstract class AbstractDocumentReader {

	protected final Resource resource;

	public AbstractDocumentReader(Resource resource) {
		this.resource = resource;
	}

	/**
	 * Returns the name of the resource. If the filename is not present, it returns the
	 * URI.
	 * @return Name or URI of the resource
	 */
	protected String resourceName() {
		try {
			String resourceName = this.resource.getFilename();
			if (!StringUtils.hasText(resourceName)) {
				resourceName = this.resource.getURI().toString();
			}
			return resourceName;
		}
		catch (IOException e) {
			return String.format("Invalid source URI: %s", e.getMessage());
		}
	}

}
