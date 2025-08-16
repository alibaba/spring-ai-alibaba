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

package com.alibaba.cloud.ai.studio.core.base.domain;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

import java.net.URI;

/**
 * A custom HTTP DELETE request implementation that supports request body. Extends
 * HttpEntityEnclosingRequestBase to enable sending data in the request body.
 *
 * @since 1.0.0.3
 */
public class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {

	/** HTTP DELETE method name */
	public final static String METHOD_NAME = "DELETE";

	/** Default constructor */
	public HttpDeleteWithBody() {
		super();
	}

	/**
	 * Constructor with URI
	 * @param uri the target URI for the DELETE request
	 */
	public HttpDeleteWithBody(final URI uri) {
		super();
		setURI(uri);
	}

	/**
	 * Constructor with URI string
	 * @param uri the target URI string for the DELETE request
	 * @throws IllegalArgumentException if the URI string is invalid
	 */
	public HttpDeleteWithBody(final String uri) {
		super();
		setURI(URI.create(uri));
	}

	@Override
	public String getMethod() {
		return METHOD_NAME;
	}

}
