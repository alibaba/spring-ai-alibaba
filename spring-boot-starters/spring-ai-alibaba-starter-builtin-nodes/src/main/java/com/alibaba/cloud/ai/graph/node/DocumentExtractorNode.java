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
package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.document.JsonDocumentParser;
import com.alibaba.cloud.ai.document.TextDocumentParser;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.parser.bshtml.BsHtmlDocumentParser;
import com.alibaba.cloud.ai.parser.markdown.MarkdownDocumentParser;
import com.alibaba.cloud.ai.parser.tika.TikaDocumentParser;
import com.alibaba.cloud.ai.parser.yaml.YamlDocumentParser;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.ai.document.Document;
import org.springframework.ai.util.json.JsonParser;

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * @author HeYQ
 * @since 2025-05-02 17:03
 */
public class DocumentExtractorNode implements NodeAction {

	private static final Path DEFAULT_LOCAL_FILE_ROOT = Paths.get("").toAbsolutePath().normalize();

	private static final int REMOTE_CONNECT_TIMEOUT_MILLIS = 10_000;

	private static final int REMOTE_READ_TIMEOUT_MILLIS = 30_000;

	private static final long MAX_REMOTE_RESOURCE_SIZE_BYTES = 10 * 1024 * 1024;

	private final String paramsKey;

	private final String outputKey;

	private final List<String> fileList;

	private final boolean inputIsArray;

	private final Path localFileRoot;

	private final boolean remoteResourcesAllowed;

	private final Map<String, Function<InputStream, List<Document>>> extractors = new HashMap<>();

	public DocumentExtractorNode(String paramsKey, String outputKey, List<String> fileList, boolean inputIsArray) {
		this(paramsKey, outputKey, fileList, inputIsArray, DEFAULT_LOCAL_FILE_ROOT, false);
	}

	private DocumentExtractorNode(String paramsKey, String outputKey, List<String> fileList, boolean inputIsArray,
			Path localFileRoot, boolean remoteResourcesAllowed) {
		this.paramsKey = paramsKey;
		this.outputKey = outputKey;
		this.fileList = fileList;
		this.inputIsArray = inputIsArray;
		this.localFileRoot = localFileRoot.toAbsolutePath().normalize();
		this.remoteResourcesAllowed = remoteResourcesAllowed;
		extractors.put("txt", inputStream -> new TextDocumentParser().parse(inputStream));
		extractors.put("markdown", inputStream -> new MarkdownDocumentParser().parse(inputStream));
		extractors.put("md", inputStream -> new MarkdownDocumentParser().parse(inputStream));
		extractors.put("html", inputStream -> new BsHtmlDocumentParser().parse(inputStream));
		extractors.put("htm", inputStream -> new BsHtmlDocumentParser().parse(inputStream));
		extractors.put("xml", inputStream -> new BsHtmlDocumentParser().parse(inputStream));
		extractors.put("json", inputStream -> new JsonDocumentParser().parse(inputStream));
		extractors.put("yaml", inputStream -> new YamlDocumentParser().parse(inputStream));
		extractors.put("yml", inputStream -> new YamlDocumentParser().parse(inputStream));
		extractors.put("pdf", inputStream -> new TikaDocumentParser().parse(inputStream));
		extractors.put("doc", inputStream -> new TikaDocumentParser().parse(inputStream));
		extractors.put("docx", inputStream -> new TikaDocumentParser().parse(inputStream));
		extractors.put("csv", inputStream -> new TikaDocumentParser().parse(inputStream));
		extractors.put("xls", inputStream -> new TikaDocumentParser().parse(inputStream));
		extractors.put("xlsx", inputStream -> new TikaDocumentParser().parse(inputStream));
		extractors.put("ppt", inputStream -> new TikaDocumentParser().parse(inputStream));
		extractors.put("pptx", inputStream -> new TikaDocumentParser().parse(inputStream));
	}

	private InputStream getInputStream(String filePath) throws IOException {
		if (filePath.startsWith("file:")) {
			return openLocalInputStream(Paths.get(URI.create(filePath)));
		}
		if (!filePath.startsWith("http://") && !filePath.startsWith("https://")) {
			if (filePath.contains("://")) {
				throw new IOException("Unsupported document resource scheme");
			}
			return openLocalInputStream(Paths.get(filePath));
		}
		URI uri = URI.create(filePath);
		if (!this.remoteResourcesAllowed) {
			throw new IOException("Remote document resources are disabled");
		}
		return openRemoteInputStream(uri);
	}

	private InputStream openLocalInputStream(Path path) throws IOException {
		Path normalizedPath = path.toAbsolutePath().normalize();
		if (!normalizedPath.startsWith(this.localFileRoot)) {
			throw new IOException("Document resource is outside the configured local file root");
		}
		Path root = this.localFileRoot.toRealPath();
		Path relativePath = this.localFileRoot.relativize(normalizedPath);
		if (relativePath.getNameCount() == 0) {
			throw new IOException("Document resource must be a regular file");
		}
		try (DirectoryStream<Path> rootDirectoryStream = Files.newDirectoryStream(root)) {
			if (!(rootDirectoryStream instanceof SecureDirectoryStream<?>)) {
				throw new IOException("Secure local document access is not supported by this file system");
			}
			return openSecureLocalInputStream(asSecureDirectoryStream(rootDirectoryStream), relativePath);
		}
	}

	@SuppressWarnings("unchecked")
	private static SecureDirectoryStream<Path> asSecureDirectoryStream(DirectoryStream<Path> directoryStream) {
		return (SecureDirectoryStream<Path>) directoryStream;
	}

	private static InputStream openSecureLocalInputStream(SecureDirectoryStream<Path> rootDirectory,
			Path relativePath) throws IOException {
		SecureDirectoryStream<Path> directory = rootDirectory;
		try {
			for (int i = 0; i < relativePath.getNameCount() - 1; i++) {
				SecureDirectoryStream<Path> childDirectory = directory.newDirectoryStream(relativePath.getName(i),
						LinkOption.NOFOLLOW_LINKS);
				if (directory != rootDirectory) {
					directory.close();
				}
				directory = childDirectory;
			}
			Path fileName = relativePath.getFileName();
			BasicFileAttributeView attributeView = directory.getFileAttributeView(fileName,
					BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
			if (attributeView == null || !attributeView.readAttributes().isRegularFile()) {
				throw new IOException("Document resource must be a regular file");
			}
			SeekableByteChannel channel = directory.newByteChannel(fileName,
					Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS));
			return new BufferedInputStream(Channels.newInputStream(channel));
		}
		finally {
			directory.close();
		}
	}

	private InputStream openRemoteInputStream(URI uri) throws IOException {
		RequestConfig requestConfig = RequestConfig.custom()
			.setConnectTimeout(REMOTE_CONNECT_TIMEOUT_MILLIS)
			.setSocketTimeout(REMOTE_READ_TIMEOUT_MILLIS)
			.setRedirectsEnabled(false)
			.build();
		CloseableHttpClient httpClient = createHttpClient(requestConfig);
		CloseableHttpResponse response = null;
		try {
			response = httpClient.execute(new HttpGet(uri));
			int status = response.getStatusLine().getStatusCode();
			if (status < HttpStatus.SC_OK || status >= HttpStatus.SC_MULTIPLE_CHOICES) {
				throw new IOException("Remote document resource returned HTTP status " + status);
			}
			if (response.getEntity() == null) {
				throw new IOException("Remote document resource has no response body");
			}
			long contentLength = response.getEntity().getContentLength();
			if (contentLength > MAX_REMOTE_RESOURCE_SIZE_BYTES) {
				throw new IOException("Remote document resource exceeds the maximum allowed size");
			}
			InputStream responseStream = new HttpResponseInputStream(response.getEntity().getContent(), response, httpClient);
			response = null;
			httpClient = null;
			return new LimitedInputStream(new BufferedInputStream(responseStream), MAX_REMOTE_RESOURCE_SIZE_BYTES);
		}
		finally {
			if (response != null) {
				response.close();
			}
			if (httpClient != null) {
				httpClient.close();
			}
		}
	}

	private static CloseableHttpClient createHttpClient(RequestConfig requestConfig) {
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
			.register("http", PlainConnectionSocketFactory.getSocketFactory())
			.register("https", SSLConnectionSocketFactory.getSocketFactory())
			.build();
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry,
				DocumentExtractorNode::resolveAndValidateRemoteAddresses);
		return HttpClients.custom()
			.setConnectionManager(connectionManager)
			.setDefaultRequestConfig(requestConfig)
			.disableRedirectHandling()
			.build();
	}

	private static InetAddress[] resolveAndValidateRemoteAddresses(String host) throws UnknownHostException {
		if (host == null || host.isBlank()) {
			throw new UnknownHostException("Remote document resource must have a host");
		}
		InetAddress[] addresses = InetAddress.getAllByName(host);
		for (InetAddress address : addresses) {
			if (isBlockedRemoteAddress(address)) {
				throw new UnknownHostException("Remote document resource resolves to a blocked address");
			}
		}
		return addresses;
	}

	static boolean isBlockedRemoteAddress(InetAddress address) {
		if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
				|| address.isSiteLocalAddress() || address.isMulticastAddress()) {
			return true;
		}
		if (address instanceof Inet6Address) {
			byte[] bytes = address.getAddress();
			if (isIpv4MappedIpv6Address(bytes)) {
				return isBlockedIpv4Address(Arrays.copyOfRange(bytes, 12, 16));
			}
			byte firstByte = bytes[0];
			return (firstByte & 0xFE) == 0xFC;
		}
		return isBlockedIpv4Address(address.getAddress());
	}

	private static boolean isIpv4MappedIpv6Address(byte[] bytes) {
		if (bytes.length != 16 || bytes[10] != (byte) 0xFF || bytes[11] != (byte) 0xFF) {
			return false;
		}
		for (int i = 0; i < 10; i++) {
			if (bytes[i] != 0) {
				return false;
			}
		}
		return true;
	}

	private static boolean isBlockedIpv4Address(byte[] bytes) {
		int first = Byte.toUnsignedInt(bytes[0]);
		int second = Byte.toUnsignedInt(bytes[1]);
		if (first == 0 || first >= 224 || (first == 100 && second >= 64 && second <= 127)
				|| (first == 198 && (second == 18 || second == 19)) || first == 10 || first == 127
				|| (first == 169 && second == 254) || (first == 172 && second >= 16 && second <= 31)
				|| (first == 192 && second == 168)) {
			return true;
		}
		return false;
	}

	private List<String> getDocument(List<String> fileList) {
		return fileList.stream().map(String::trim).map(file -> {
			try (InputStream inputStream = this.getInputStream(file.trim())) {
				return this.extractTextByFileExtension(inputStream, getFileExtension(file));
			}
			catch (Exception e) {
				throw new RuntimeException("Failed to parse test file: " + file, e);
			}
		}).toList();
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		if (paramsKey == null && fileList == null) {
			throw new RuntimeException("File variable not found for selector");
		}
		List<String> fileList;
		Object fileObj = state.value(paramsKey).orElse(this.fileList);
		if (this.inputIsArray) {
			if (fileObj instanceof List<?>) {
				fileList = (List<String>) fileObj;
			}
			else if (fileObj instanceof String[]) {
				fileList = Arrays.asList((String[]) fileObj);
			}
			else {
				// Try to parse as Json string, if failed the input is invalid
				try {
					fileList = JsonParser.fromJson(fileObj.toString(), new TypeReference<List<String>>() {
					});
				}
				catch (Exception ignore) {
					fileList = null;
				}
			}
			if (fileList == null || fileList.isEmpty()) {
				throw new RuntimeException("Variable fileList is not an ArrayFileSegment");
			}
		}
		else {
			// Single file, add directly to the list
			fileList = List.of(fileObj.toString());
		}
		List<String> documentContents = this.getDocument(fileList);

		String key = Optional.ofNullable(this.outputKey).orElse("text");
		if (!this.inputIsArray) {
			return Map.of(key, documentContents.get(0));
		}
		else {
			return Map.of(key, documentContents);
		}
	}

	private String extractTextByFileExtension(InputStream fileContent, String fileExtension) {

		Function<InputStream, List<Document>> extractor = this.extractors.get(fileExtension);
		if (extractor == null) {
			throw new RuntimeException("Unsupported Extension Type: " + fileExtension);
		}

		return extractor.apply(fileContent).get(0).getText();
	}

	private String getFileExtension(String filePath) {
		Path path = Paths.get(filePath);
		String fileName = path.getFileName().toString();
		int dotIndex = fileName.lastIndexOf('.');

		return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String paramsKey;

		private String outputKey;

		private List<String> fileList;

		private boolean inputIsArray = false;

		private Path localFileRoot = DEFAULT_LOCAL_FILE_ROOT;

		private boolean remoteResourcesAllowed = false;

		public Builder paramsKey(String paramsKey) {
			this.paramsKey = paramsKey;
			return this;
		}

		public Builder outputKey(String outputKey) {
			this.outputKey = outputKey;
			return this;
		}

		public Builder fileList(List<String> fileList) {
			this.fileList = fileList;
			return this;
		}

		public Builder inputIsArray(boolean inputIsArray) {
			this.inputIsArray = inputIsArray;
			return this;
		}

		/**
		 * Configures the root directory used to resolve local document resources.
		 */
		public Builder localFileRoot(Path localFileRoot) {
			this.localFileRoot = localFileRoot;
			return this;
		}

		/**
		 * Enables HTTP(S) document resources. Private, loopback, link-local and multicast
		 * addresses remain blocked.
		 */
		public Builder allowRemoteResources(boolean remoteResourcesAllowed) {
			this.remoteResourcesAllowed = remoteResourcesAllowed;
			return this;
		}

		public DocumentExtractorNode build() {
			return new DocumentExtractorNode(paramsKey, outputKey, fileList, inputIsArray, localFileRoot,
					remoteResourcesAllowed);
		}

	}

	static final class LimitedInputStream extends FilterInputStream {

		private final long maxBytes;

		private long bytesRead;

		LimitedInputStream(InputStream inputStream, long maxBytes) {
			super(inputStream);
			this.maxBytes = maxBytes;
		}

		@Override
		public int read() throws IOException {
			if (this.bytesRead == this.maxBytes) {
				return readPastLimit();
			}
			int value = super.read();
			if (value != -1) {
				this.bytesRead++;
			}
			return value;
		}

		@Override
		public int read(byte[] bytes, int offset, int length) throws IOException {
			if (length == 0) {
				return 0;
			}
			long remaining = this.maxBytes - this.bytesRead;
			if (remaining <= 0) {
				return readPastLimit();
			}
			int count = super.read(bytes, offset, (int) Math.min(length, remaining));
			if (count > 0) {
				this.bytesRead += count;
			}
			return count;
		}

		@Override
		public long skip(long count) throws IOException {
			if (count <= 0) {
				return 0;
			}
			long remaining = this.maxBytes - this.bytesRead;
			if (remaining <= 0) {
				readPastLimit();
				return 0;
			}
			long skipped = super.skip(Math.min(count, remaining));
			this.bytesRead += skipped;
			return skipped;
		}

		private int readPastLimit() throws IOException {
			if (super.read() != -1) {
				throw new IOException("Remote document resource exceeds the maximum allowed size");
			}
			return -1;
		}

	}

	private static final class HttpResponseInputStream extends FilterInputStream {

		private final CloseableHttpResponse response;

		private final CloseableHttpClient httpClient;

		private HttpResponseInputStream(InputStream inputStream, CloseableHttpResponse response,
				CloseableHttpClient httpClient) {
			super(inputStream);
			this.response = response;
			this.httpClient = httpClient;
		}

		@Override
		public void close() throws IOException {
			try {
				super.close();
			}
			finally {
				try {
					this.response.close();
				}
				finally {
					this.httpClient.close();
				}
			}
		}

	}

}
