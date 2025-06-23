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
package com.alibaba.cloud.ai.dashscope.api;

import com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants;
import com.alibaba.cloud.ai.dashscope.common.DashScopeException;
import com.alibaba.cloud.ai.dashscope.common.ErrorCodeEnum;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentTransformerOptions;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeStoreOptions;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.ChatModelDescription;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author nuocheng.lxm
 * @author yuluo
 * @author YunKui Lu
 * @since 1.0.0-M2
 */
public class DashScopeApi {

	private static final Predicate<String> SSE_DONE_PREDICATE = "[DONE]"::equals;

	// Store config fields for mutate/copy
	private final String baseUrl;

	private final ApiKey apiKey;

	private final String completionsPath;

	private final String embeddingsPath;

	private final MultiValueMap<String, String> headers;

	/**
	 * Default chat model
	 */
	public static final String DEFAULT_CHAT_MODEL = ChatModel.QWEN_PLUS.getValue();

	public static final String DEFAULT_EMBEDDING_MODEL = EmbeddingModel.EMBEDDING_V2.getValue();

	public static final String DEFAULT_EMBEDDING_TEXT_TYPE = EmbeddingTextType.DOCUMENT.getValue();

	private final RestClient restClient;

	private final WebClient webClient;

	private final ResponseErrorHandler responseErrorHandler;

	/**
	 * Returns a builder pre-populated with the current configuration for mutation.
	 */
	public Builder mutate() {
		return new Builder(this);
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Create a new chat completion api.
	 * @param baseUrl api base URL.
	 * @param apiKey OpenAI apiKey.
	 * @param header the http headers to use.
	 * @param completionsPath the path to the chat completions endpoint.
	 * @param embeddingsPath the path to the embeddings endpoint.
	 * @param workSpaceId the workspace ID to use.
	 * @param restClientBuilder RestClient builder.
	 * @param webClientBuilder WebClient builder.
	 * @param responseErrorHandler Response error handler.
	 */
	// @formatter:off
	public DashScopeApi(
			String baseUrl,
			ApiKey apiKey,
			MultiValueMap<String, String> header,
			String completionsPath,
			String embeddingsPath,
			// Add request header.
			String workSpaceId,
			RestClient.Builder restClientBuilder,
			WebClient.Builder webClientBuilder,
			ResponseErrorHandler responseErrorHandler
	) {

		this.baseUrl = baseUrl;
		this.apiKey = apiKey;
		this.headers = header;
		this.completionsPath = completionsPath;
		this.embeddingsPath = embeddingsPath;
		this.responseErrorHandler = responseErrorHandler;

		// For DashScope API, the workspace ID is passed in the headers.
		if (StringUtils.hasText(workSpaceId)) {
			this.headers.add(DashScopeApiConstants.HEADER_WORK_SPACE_ID, workSpaceId);
		}

		// Check API Key in headers.
		Consumer<HttpHeaders> finalHeaders = h -> {
			if (!(apiKey instanceof NoopApiKey)) {
				h.setBearerAuth(apiKey.getValue());
			}

			h.setContentType(MediaType.APPLICATION_JSON);
			h.addAll(headers);
		};

		this.restClient = restClientBuilder.clone()
				.baseUrl(baseUrl)
				.defaultHeaders(finalHeaders)
				.defaultStatusHandler(responseErrorHandler)
				.build();

		this.webClient = webClientBuilder
				.baseUrl(baseUrl)
				.defaultHeaders(finalHeaders)
				.build();
	}
	// @formatter:on

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record CommonResponse<T>(@JsonProperty("code") String code, @JsonProperty("message") String message,
			@JsonProperty("data") T data) {
	}

	/**
	 * Spring AI Alibaba Dashscope implements all models that support the dashscope
	 * platform, and only the Qwen series models are listed here. For more model options,
	 * refer to: <a href="https://help.aliyun.com/zh/model-studio/models">Model List</a>
	 */
	public enum ChatModel implements ChatModelDescription {

		/**
		 * The model supports an 8k tokens context, and to ensure normal use and output,
		 * the API limits user input to 6k tokens.
		 */
		QWEN_PLUS("qwen-plus"),

		/**
		 * The model supports a context of 32k tokens. To ensure normal use and output,
		 * the API limits user input to 30k tokens.
		 */
		QWEN_TURBO("qwen-turbo"),

		/**
		 * The model supports an 8k tokens context, and to ensure normal use and output,
		 * the API limits user input to 6k tokens.
		 */
		QWEN_MAX("qwen-max"),

		/**
		 * The model supports a context of 30k tokens. To ensure normal use and output,
		 * the API limits user input to 28k tokens.
		 */
		QWEN_MAX_LONGCONTEXT("qwen-max-longcontext"),

		/**
		 * The Qwen3, QwQ (based on Qwen2.5) and DeepSeek-R1 models have powerful
		 * inference capabilities. The model outputs the thought process first, and then
		 * the response.
		 * <a href="https://help.aliyun.com/zh/model-studio/deep-thinking">qwen3</a>
		 */
		QWQ_PLUS("qwq-plus"),

		/**
		 * The QwQ inference model trained based on the Qwen2.5-32B model greatly improves
		 * the model inference ability through reinforcement learning. The core indicators
		 * such as the mathematical code of the model (AIME 24/25, LiveCodeBench) and some
		 * general indicators (IFEval, LiveBench, etc.) have reached the level of
		 * DeepSeek-R1 full blood version, and all indicators significantly exceed the
		 * DeepSeek-R1-Distill-Qwen-32B, which is also based on Qwen2.5-32B.
		 * <a href="https://help.aliyun.com/zh/model-studio/deep-thinking">qwen3</a>
		 */
		QWEN_3_32B("qwq-32b"),

		/**
		 * The QWEN-OMNI series models support the input of multiple modalities of data,
		 * including video, audio, image, text, and output audio and text
		 * <a href="https://help.aliyun.com/zh/model-studio/qwen-omni">qwen-omni</a>
		 */
		QWEN_OMNI_TURBO("qwen-omni-turbo"),

		/**
		 * The qwen-vl model can answer based on the pictures you pass in.
		 * <a href="https://help.aliyun.com/zh/model-studio/vision">qwen-vl</a>
		 */
		QWEN_VL_MAX("qwen-vl-max"),

		// =================== DeepSeek Model =====================
		// The third-party models of the Dashscope platform are currently only listed on
		// Deepseek, refer: https://help.aliyun.com/zh/model-studio/models for
		// more models

		DEEPSEEK_R1("deepseek-r1"),

		DEEPSEEK_V3("deepseek-v3");

		public final String value;

		ChatModel(String value) {
			this.value = value;
		}

		public String getValue() {
			return this.value;
		}

		@Override
		public String getName() {
			return this.value;
		}

	}

	/*******************************************
	 * Embedding
	 **********************************************/

	/**
	 * <a href="https://help.aliyun.com/zh/model-studio/embedding">Embedding Models</a>
	 */
	public enum EmbeddingModel {

		/**
		 * DIMENSION: 1536
		 */
		EMBEDDING_V1("text-embedding-v1"),

		/**
		 * DIMENSION: 1536
		 */
		EMBEDDING_V2("text-embedding-v2"),

		/**
		 * 1,024(Default)、768、512、256、128 or 64
		 */
		EMBEDDING_V3("text-embedding-v3"),

		/**
		 * 2,048、1,536、1,024(Default)、768、512、256、128 or 64
		 */
		EMBEDDING_V4("text-embedding-v4");

		public final String value;

		EmbeddingModel(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

	}

	public enum EmbeddingTextType {

		QUERY("query"),

		DOCUMENT("document");

		public final String value;

		EmbeddingTextType(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record EmbeddingUsage(@JsonProperty("total_tokens") Long totalTokens) implements Usage {
		@Override
		public Integer getPromptTokens() {
			return null;
		}

		@Override
		public Integer getCompletionTokens() {
			return null;
		}

		@Override
		public Object getNativeUsage() {
			return null;
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Embedding(@JsonProperty("text_index") Integer textIndex,
			@JsonProperty("embedding") float[] embedding) {
	}

	// @formatter:off
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record EmbeddingList(
			@JsonProperty("request_id") String requestId,
			@JsonProperty("code") String code,
			@JsonProperty("message") String message,
			@JsonProperty("output") Embeddings output,
			@JsonProperty("usage") EmbeddingUsage usage) {
	}
	// @formatter:on

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Embeddings(@JsonProperty("embeddings") List<Embedding> embeddings) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record EmbeddingRequestInput(@JsonProperty("texts") List<String> texts) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record EmbeddingRequestInputParameters(@JsonProperty("text_type") String textType,
			@JsonProperty("dimension") Integer dimension) {

		@Deprecated
		public EmbeddingRequestInputParameters(String textType) {
			this(textType, null);
		}

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {

			private String textType;

			private Integer dimension;

			private Builder() {

			}

			public Builder textType(String textType) {
				this.textType = textType;
				return this;
			}

			public Builder dimension(Integer dimension) {
				this.dimension = dimension;
				return this;
			}

			public EmbeddingRequestInputParameters build() {
				return new EmbeddingRequestInputParameters(textType, dimension);
			}

		}
	}

	/**
	 * Creates an embedding vector representing the input text.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record EmbeddingRequest(@JsonProperty("model") String model,
			@JsonProperty("input") EmbeddingRequestInput input,
			@JsonProperty("parameters") EmbeddingRequestInputParameters parameters) {

		@Deprecated
		public EmbeddingRequest(String text) {
			this(DEFAULT_EMBEDDING_MODEL, new EmbeddingRequestInput(List.of(text)),
					new EmbeddingRequestInputParameters(DEFAULT_EMBEDDING_TEXT_TYPE));
		}

		@Deprecated
		public EmbeddingRequest(String text, String model) {
			this(model, new EmbeddingRequestInput(List.of(text)),
					new EmbeddingRequestInputParameters(DEFAULT_EMBEDDING_TEXT_TYPE));
		}

		@Deprecated
		public EmbeddingRequest(String text, String model, String textType) {
			this(model, new EmbeddingRequestInput(List.of(text)), new EmbeddingRequestInputParameters(textType));
		}

		@Deprecated
		public EmbeddingRequest(List<String> texts) {
			this(DEFAULT_EMBEDDING_MODEL, new EmbeddingRequestInput(texts),
					new EmbeddingRequestInputParameters(DEFAULT_EMBEDDING_TEXT_TYPE));
		}

		@Deprecated
		public EmbeddingRequest(List<String> texts, String model) {
			this(model, new EmbeddingRequestInput(texts),
					new EmbeddingRequestInputParameters(DEFAULT_EMBEDDING_TEXT_TYPE));
		}

		@Deprecated
		public EmbeddingRequest(List<String> texts, String model, String textType) {
			this(model, new EmbeddingRequestInput(texts), new EmbeddingRequestInputParameters(textType));
		}

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {

			private final List<String> texts = new ArrayList<>();

			private String model = DEFAULT_EMBEDDING_MODEL;

			private String textType;

			private Integer dimension;

			private Builder() {
			}

			public Builder model(String model) {
				this.model = model;
				return this;
			}

			public Builder texts(String... texts) {
				this.texts.addAll(List.of(texts));
				return this;
			}

			public Builder texts(List<String> texts) {
				this.texts.addAll(texts);
				return this;
			}

			public Builder textType(String textType) {
				this.textType = textType;
				return this;
			}

			public Builder dimension(Integer dimension) {
				this.dimension = dimension;
				return this;
			}

			public EmbeddingRequest build() {
				return new EmbeddingRequest(model, new EmbeddingRequestInput(texts),
						EmbeddingRequestInputParameters.builder().textType(textType).dimension(dimension).build());
			}

		}
	}

	public ResponseEntity<EmbeddingList> embeddings(EmbeddingRequest embeddingRequest) {

		Assert.notNull(embeddingRequest, "The request body can not be null.");
		Assert.notNull(embeddingRequest.input(), "The input can not be null.");
		Assert.isTrue(!CollectionUtils.isEmpty(embeddingRequest.input().texts()), "The input texts can not be empty.");
		Assert.isTrue(embeddingRequest.input().texts().size() <= 25, "The input texts limit 25.");

		return this.restClient.post()
			.uri(this.embeddingsPath)
			.headers(this::addDefaultHeadersIfMissing)
			.body(embeddingRequest)
			.retrieve()
			.toEntity(EmbeddingList.class);
	}

	/*******************************************
	 * Data center.
	 **********************************************/
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record UploadRequest(@JsonProperty("category_id") String categoryId,
			@JsonProperty("file_name") String fileName, @JsonProperty("size_bytes") long fileLength,
			@JsonProperty("content_md5") String fileMD5) {

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record AddFileRequest(@JsonProperty("lease_id") String leaseId, @JsonProperty("parser") String parser) {
		}

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record QueryFileRequest(@JsonProperty("file_id") String fileId) {
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record UploadLeaseResponse(@JsonProperty("code") String code, @JsonProperty("message") String message,
			@JsonProperty("data") UploadLeaseResponseData data) {

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record UploadLeaseResponseData(@JsonProperty("lease_id") String leaseId,
				@JsonProperty("type") String type, @JsonProperty("param") UploadLeaseParamData param) {
		}

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record UploadLeaseParamData(@JsonProperty("url") String url, @JsonProperty("method") String method,
				@JsonProperty("headers") Map<String, String> header) {
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record AddFileResponseData(@JsonProperty("file_id") String fileId, @JsonProperty("parser") String method) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record QueryFileResponseData(@JsonProperty("category") String category,
			@JsonProperty("file_id") String fileId, @JsonProperty("file_name") String fileName,
			@JsonProperty("file_type") String fileType, @JsonProperty("size_bytes") Long sizeBytes,
			@JsonProperty("status") String status, @JsonProperty("upload_time") String uploadtime) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record QueryFileParseResultData(@JsonProperty("file_id") String fileId,
			@JsonProperty("file_name") String fileName, @JsonProperty("lease_id") String leaseId,
			@JsonProperty("type") String type, @JsonProperty("param") DownloadFileParam param) {
		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record DownloadFileParam(@JsonProperty("method") String method, @JsonProperty("url") String url,
				@JsonProperty("headers") Map<String, String> headers) {
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record DocumentSplitRequest(@JsonProperty("text") String text, @JsonProperty("chunk_size") Integer chunkSize,
			@JsonProperty("overlap_size") Integer overlapSize, @JsonProperty("file_type") String fileType,
			@JsonProperty("language") String language, @JsonProperty("separator") String separator) {

	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record DocumentSplitResponse(@JsonProperty("chunkService") DocumentSplitResponseData chunkService) {

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record DocumentSplitResponseData(@JsonProperty("chunkResult") List<DocumentChunk> chunkResult) {

		}

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record DocumentChunk(@JsonProperty("chunk_id") int chunkId, @JsonProperty("content") String content,
				@JsonProperty("title") String title, @JsonProperty("hier_title") String hierTitle,
				@JsonProperty("nid") String nid, @JsonProperty("parent") String parent) {
		}
	}

	public String upload(File file, UploadRequest request) {
		// apply to upload
		ResponseEntity<UploadLeaseResponse> responseEntity = uploadLease(request);
		var uploadLeaseResponse = responseEntity.getBody();
		if (uploadLeaseResponse == null) {
			throw new DashScopeException(ErrorCodeEnum.READER_APPLY_LEASE_ERROR);
		}
		if (!"SUCCESS".equalsIgnoreCase(uploadLeaseResponse.code())) {
			throw new DashScopeException("ApplyLease Failed,code:%s,message:%s".formatted(uploadLeaseResponse.code(),
					uploadLeaseResponse.message()));
		}
		uploadFile(file, uploadLeaseResponse);
		return addFile(uploadLeaseResponse.data.leaseId(), request);
	}

	public ResponseEntity<CommonResponse<QueryFileResponseData>> queryFileInfo(String categoryId,
			UploadRequest.QueryFileRequest request) {
		return this.restClient.post()
			.uri("/api/v1/datacenter/category/{category}/file/{fileId}/query", categoryId, request.fileId)
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
	}

	public String getFileParseResult(String categoryId, UploadRequest.QueryFileRequest request) {
		ResponseEntity<CommonResponse<QueryFileParseResultData>> fileParseResponse = this.restClient.post()
			.uri("/api/v1/datacenter/category/{categoryId}/file/{fileId}/download_lease", categoryId, request.fileId())
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
		if (fileParseResponse == null || fileParseResponse.getBody() == null) {
			throw new DashScopeException("GetDocumentParseResultError");
		}
		CommonResponse<QueryFileParseResultData> commonResponse = fileParseResponse.getBody();

		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		for (String key : commonResponse.data.param.headers.keySet()) {
			headers.set(key, commonResponse.data.param.headers.get(key));
		}
		try {
			HttpEntity<InputStreamResource> requestEntity = new HttpEntity<>(null, headers);
			ResponseEntity<String> response = restTemplate.exchange(new URI(commonResponse.data.param.url),
					HttpMethod.GET, requestEntity, String.class);
			return response.getBody();
		}
		catch (Exception ex) {
			throw new DashScopeException("GetDocumentParseResultError");
		}
	}

	private String addFile(String leaseId, UploadRequest request) {
		try {
			UploadRequest.AddFileRequest addFileRequest = new UploadRequest.AddFileRequest(leaseId,
					DashScopeApiConstants.DEFAULT_PARSER_NAME);
			ResponseEntity<CommonResponse<AddFileResponseData>> response = this.restClient.post()
				.uri("/api/v1/datacenter/category/{categoryId}/add_file", request.categoryId)
				.body(addFileRequest)
				.retrieve()
				.toEntity(new ParameterizedTypeReference<>() {
				});
			CommonResponse<AddFileResponseData> addFileResponse = response.getBody();
			if (addFileResponse == null || !"SUCCESS".equals(addFileResponse.code.toUpperCase())) {
				throw new DashScopeException(ErrorCodeEnum.READER_ADD_FILE_ERROR);
			}
			AddFileResponseData addFileResult = addFileResponse.data;
			return addFileResult.fileId();
		}
		catch (Exception ex) {
			throw new DashScopeException(ErrorCodeEnum.READER_ADD_FILE_ERROR);
		}
	}

	private void uploadFile(File file, UploadLeaseResponse uploadLeaseResponse) {
		try {
			UploadLeaseResponse.UploadLeaseParamData uploadParam = uploadLeaseResponse.data.param;
			OkHttpClient client = new OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS)
				.writeTimeout(60, TimeUnit.SECONDS)
				.readTimeout(60, TimeUnit.SECONDS)
				.build();

			okhttp3.Headers.Builder headersBuilder = new okhttp3.Headers.Builder();
			String contentType = uploadParam.header.remove("Content-Type");

			for (String key : uploadParam.header.keySet()) {
				headersBuilder.add(key, uploadParam.header.get(key));
			}

			RequestBody requestBody;
			if (StringUtils.hasLength(contentType)) {
				requestBody = RequestBody.create(file, okhttp3.MediaType.parse(contentType));
			}
			else {
				requestBody = RequestBody.create(file, null);
				headersBuilder.add("Content-Type", "");
			}

			Request request = new Request.Builder().url(uploadParam.url)
				.headers(headersBuilder.build())
				.put(requestBody)
				.build();

			try (Response response = client.newCall(request).execute()) {
				if (!response.isSuccessful()) {
					throw new Exception("Unexpected response code: " + response.code());
				}
			}
		}
		catch (Exception ex) {
			throw new DashScopeException("Upload File Failed", ex);
		}
	}

	private ResponseEntity<UploadLeaseResponse> uploadLease(UploadRequest request) {
		return this.restClient.post()
			.uri("/api/v1/datacenter/category/{categoryId}/upload_lease", request.categoryId)
			.body(request)
			.retrieve()
			.toEntity(UploadLeaseResponse.class);
	}

	public ResponseEntity<DocumentSplitResponse> documentSplit(Document document,
			DashScopeDocumentTransformerOptions options) {
		DocumentSplitRequest request = new DocumentSplitRequest(document.getText(), options.getChunkSize(),
				options.getOverlapSize(), options.getFileType(), options.getLanguage(), options.getSeparator());
		return this.restClient.post()
			.uri("/api/v1/indices/component/configed_transformations/spliter")
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record UpsertPipelineRequest(@JsonProperty("name") String name,
			@JsonProperty("pipeline_type") String pipelineType,
			@JsonProperty("pipeline_description") String pipelineDescription,
			@JsonProperty("data_type") String dataType, @JsonProperty("config_model") String configModel,
			@JsonProperty("configured_transformations") List transformations,
			@JsonProperty("data_sources") List<DataSourcesConfig> dataSources,
			@JsonProperty("data_sinks") List<DataSinksConfig> dataSinks) {

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record DataSinksConfig(@JsonProperty("sink_type") String sinkType,
				@JsonProperty("component") DataSinksComponent component) {

			@JsonInclude(JsonInclude.Include.NON_NULL)
			public record DataSinksComponent() {
			}
		}

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record DataSourcesConfig(@JsonProperty("source_type") String sourceType,
				@JsonProperty("component") DataSourcesComponent component) {

			@JsonInclude(JsonInclude.Include.NON_NULL)
			public record DataSourcesComponent(@JsonProperty("doc_ids") List<String> docIds) {

			}

		}

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record ParserConfiguredTransformations(
				@JsonProperty("configurable_transformation_type") String transformationType,
				@JsonProperty("component") ParserComponent component) {
			@JsonInclude(JsonInclude.Include.NON_NULL)
			public record ParserComponent(@JsonProperty("chunk_size") Integer chunkSize,
					@JsonProperty("overlap_size") Integer overlapSize, @JsonProperty("input_type") String inputType,
					@JsonProperty("separator") String separator, @JsonProperty("language") String language) {

			}
		}

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record EmbeddingConfiguredTransformations(
				@JsonProperty("configurable_transformation_type") String transformationType,
				@JsonProperty("component") EmbeddingComponent component) {

			@JsonInclude(JsonInclude.Include.NON_NULL)
			public record EmbeddingComponent(@JsonProperty("model_name") String modelName) {

			}
		}

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record RetrieverConfiguredTransformations(
				@JsonProperty("configurable_transformation_type") String transformationType,
				@JsonProperty("component") RetrieverComponent component) {
			@JsonInclude(JsonInclude.Include.NON_NULL)
			public record RetrieverComponent(@JsonProperty("enable_rewrite") boolean enableRewrite,
					@JsonProperty("rewrite") List<CommonModelComponent> rewriteComponents,
					@JsonProperty("sparse_similarity_top_k") int sparseSimilarityTopK,
					@JsonProperty("dense_similarity_top_k") int denseSimilarityTopK,
					@JsonProperty("enable_reranking") boolean enableRerank,
					@JsonProperty("rerank") List<CommonModelComponent> rerankComponents,
					@JsonProperty("rerank_min_score") float rerankMinScore,
					@JsonProperty("rerank_top_n") int rerankTopN,
					@JsonProperty("search_filters") List<Map<String, Object>> searchFilters) {

			}

			@JsonInclude(JsonInclude.Include.NON_NULL)
			public record CommonModelComponent(@JsonProperty("model_name") String modelName) {
			}
		}

	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record UpsertPipelineResponse(@JsonProperty("id") String id,
			@JsonProperty("pipline_name") String pipline_name, @JsonProperty("status") String status,
			@JsonProperty("message") String message, @JsonProperty("code") String code) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record StartPipelineResponse(@JsonProperty("ingestionId") String ingestionId,
			@JsonProperty("status") String status, @JsonProperty("message") String message,
			@JsonProperty("code") String code, @JsonProperty("request_id") String requestId) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record QueryPipelineResponse(@JsonProperty("status") String status, @JsonProperty("message") String message,
			@JsonProperty("code") String code, @JsonProperty("id") String pipelineId) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record DelePipelineDocumentRequest(
			@JsonProperty("data_sources") List<DelePipelineDocumentDataSource> dataSources) {
		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record DelePipelineDocumentDataSource(@JsonProperty("source_type") String sourceType,
				@JsonProperty("component") List<DelePipelineDocumentDataSourceComponent> component) {
		}

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record DelePipelineDocumentDataSourceComponent(@JsonProperty("doc_ids") List<String> docIds) {
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record DelePipelineDocumentResponse(@JsonProperty("status") String status,
			@JsonProperty("message") String message, @JsonProperty("code") String code) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record DocumentRetrieveRequest(@JsonProperty("query") String query,
			@JsonProperty("dense_similarity_top_k") int denseSimilarityTopK,
			@JsonProperty("sparse_similarity_top_k") int sparseSimilarityTopK,
			@JsonProperty("enable_rewrite") boolean enableRewrite,
			@JsonProperty("rewrite") List<DocumentRetrieveModelConfig> rewrite,
			@JsonProperty("enable_reranking") boolean enableReranking,
			@JsonProperty("rerank") List<DocumentRetrieveModelConfig> rerank,
			@JsonProperty("rerank_min_score") float rerankMinScore, @JsonProperty("rerank_top_n") int rerankTopN,
			@JsonProperty("search_filters") List<Map<String, Object>> searchFilters) {
		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record DocumentRetrieveModelConfig(@JsonProperty("model_name") String modelName,
				@JsonProperty("class_name") String className) {
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record DocumentRetrieveResponse(@JsonProperty("status") String status,
			@JsonProperty("message") String message, @JsonProperty("code") String code,
			@JsonProperty("request_id") String requestId, @JsonProperty("total") int total,
			@JsonProperty("nodes") List<DocumentRetrieveResponseNode> nodes

	) {
		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record DocumentRetrieveResponseNode(@JsonProperty("score") double score,
				@JsonProperty("node") DocumentRetrieveResponseNodeData node) {
		}

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record DocumentRetrieveResponseNodeData(@JsonProperty("id_") String id,
				@JsonProperty("text") String text, @JsonProperty("metadata") Map<String, Object> metadata) {
		}

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record DocumentRetrieveResponseNodeMetaData(@JsonProperty("parent") String text,
				@JsonProperty("image_url") List<String> images, @JsonProperty("title") String title,
				@JsonProperty("doc_id") String documentId, @JsonProperty("doc_name") String docName,
				@JsonProperty("hier_title") String hierTitle) {
		}
	}

	public String getPipelineIdByName(String pipelineName) {
		ResponseEntity<QueryPipelineResponse> startPipelineResponse = this.restClient.get()
			.uri("/api/v1/indices/pipeline_simple?pipeline_name={pipelineName}", pipelineName)
			.retrieve()
			.toEntity(QueryPipelineResponse.class);
		if (startPipelineResponse == null || startPipelineResponse.getBody() == null
				|| startPipelineResponse.getBody().pipelineId() == null) {
			return null;
		}
		return startPipelineResponse.getBody().pipelineId;
	}

	public void upsertPipeline(List<Document> documents, DashScopeStoreOptions storeOptions) {
		String embeddingModelName = (storeOptions.getEmbeddingOptions() == null ? EmbeddingModel.EMBEDDING_V2.getValue()
				: storeOptions.getEmbeddingOptions().getModel());
		UpsertPipelineRequest.EmbeddingConfiguredTransformations embeddingConfig = new UpsertPipelineRequest.EmbeddingConfiguredTransformations(
				"DASHSCOPE_EMBEDDING",
				new UpsertPipelineRequest.EmbeddingConfiguredTransformations.EmbeddingComponent(embeddingModelName));
		DashScopeDocumentTransformerOptions transformerOptions = storeOptions.getTransformerOptions();
		if (transformerOptions == null) {
			transformerOptions = new DashScopeDocumentTransformerOptions();
		}
		UpsertPipelineRequest.ParserConfiguredTransformations parserConfig = new UpsertPipelineRequest.ParserConfiguredTransformations(
				"DASHSCOPE_JSON_NODE_PARSER",
				new UpsertPipelineRequest.ParserConfiguredTransformations.ParserComponent(
						transformerOptions.getChunkSize(), transformerOptions.getOverlapSize(), "idp",
						transformerOptions.getSeparator(), transformerOptions.getLanguage()));
		DashScopeDocumentRetrieverOptions retrieverOptions = storeOptions.getRetrieverOptions();
		if (retrieverOptions == null) {
			retrieverOptions = new DashScopeDocumentRetrieverOptions();
		}
		UpsertPipelineRequest.RetrieverConfiguredTransformations retrieverConfig = new UpsertPipelineRequest.RetrieverConfiguredTransformations(
				"DASHSCOPE_RETRIEVER",
				new UpsertPipelineRequest.RetrieverConfiguredTransformations.RetrieverComponent(
						retrieverOptions.isEnableRewrite(),
						Arrays.asList(new UpsertPipelineRequest.RetrieverConfiguredTransformations.CommonModelComponent(
								retrieverOptions.getRewriteModelName())),
						retrieverOptions.getSparseSimilarityTopK(), retrieverOptions.getDenseSimilarityTopK(),
						retrieverOptions.isEnableReranking(),
						Arrays.asList(new UpsertPipelineRequest.RetrieverConfiguredTransformations.CommonModelComponent(
								retrieverOptions.getRerankModelName())),
						retrieverOptions.getRerankMinScore(), retrieverOptions.getRerankTopN(),
						retrieverOptions.getSearchFilters()));
		List<String> documentIdList = documents.stream()
			.map(Document::getId)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
		UpsertPipelineRequest upsertPipelineRequest = new UpsertPipelineRequest(storeOptions.getIndexName(),
				"MANAGED_SHARED", null, "unstructured", "recommend",
				Arrays.asList(embeddingConfig, parserConfig, retrieverConfig),
				Arrays.asList(new UpsertPipelineRequest.DataSourcesConfig("DATA_CENTER_FILE",
						new UpsertPipelineRequest.DataSourcesConfig.DataSourcesComponent(documentIdList))),
				Arrays.asList(new UpsertPipelineRequest.DataSinksConfig("BUILT_IN", null))

		);
		ResponseEntity<UpsertPipelineResponse> upsertPipelineResponse = this.restClient.put()
			.uri("/api/v1/indices/pipeline")
			.body(upsertPipelineRequest)
			.retrieve()
			.toEntity(UpsertPipelineResponse.class);
		if (upsertPipelineResponse.getBody() == null
				|| !"SUCCESS".equalsIgnoreCase(upsertPipelineResponse.getBody().status)) {
			throw new DashScopeException(ErrorCodeEnum.CREATE_INDEX_ERROR);
		}
		String pipelineId = upsertPipelineResponse.getBody().id;
		ResponseEntity<StartPipelineResponse> startPipelineResponse = this.restClient.post()
			.uri("/api/v1/indices/pipeline/{pipeline_id}/managed_ingest", pipelineId)
			.body(upsertPipelineRequest)
			.retrieve()
			.toEntity(StartPipelineResponse.class);
		if (startPipelineResponse.getBody() == null || !"SUCCESS".equalsIgnoreCase(startPipelineResponse.getBody().code)
				|| startPipelineResponse.getBody().ingestionId == null) {
			throw new DashScopeException(ErrorCodeEnum.INDEX_ADD_DOCUMENT_ERROR);
		}
	}

	public boolean deletePipelineDocument(String pipelineId, List<String> idList) {
		DelePipelineDocumentRequest request = new DelePipelineDocumentRequest(Arrays
			.asList(new DelePipelineDocumentRequest.DelePipelineDocumentDataSource("DATA_CENTER_FILE",
					Arrays.asList(new DelePipelineDocumentRequest.DelePipelineDocumentDataSourceComponent(idList)))));
		ResponseEntity<DelePipelineDocumentResponse> deleDocumentResponse = this.restClient.post()
			.uri("/api/v1/indices/pipeline/{pipeline_id}/delete", pipelineId)
			.body(request)
			.retrieve()
			.toEntity(DelePipelineDocumentResponse.class);
		if (deleDocumentResponse == null || deleDocumentResponse.getBody() == null
				|| !"SUCCESS".equalsIgnoreCase(deleDocumentResponse.getBody().code)) {
			return false;
		}
		return true;
	}

	public List<Document> retriever(String pipelineId, String query, DashScopeDocumentRetrieverOptions searchOption) {
		DocumentRetrieveRequest request = new DocumentRetrieveRequest(query, searchOption.getDenseSimilarityTopK(),
				searchOption.getDenseSimilarityTopK(), searchOption.isEnableRewrite(),
				Arrays
					.asList(new DocumentRetrieveRequest.DocumentRetrieveModelConfig(
							searchOption.getRewriteModelName(), "DashScopeTextRewrite")),
				searchOption.isEnableReranking(),
				Arrays.asList(new DocumentRetrieveRequest.DocumentRetrieveModelConfig(searchOption.getRerankModelName(),
						null)),
				searchOption.getRerankMinScore(), searchOption.getRerankTopN(), searchOption.getSearchFilters());
		ResponseEntity<DocumentRetrieveResponse> deleDocumentResponse = this.restClient.post()
			.uri("/api/v1/indices/pipeline/{pipeline_id}/retrieve", pipelineId)
			.body(request)
			.retrieve()
			.toEntity(DocumentRetrieveResponse.class);
		if (deleDocumentResponse == null || deleDocumentResponse.getBody() == null
				|| !"SUCCESS".equalsIgnoreCase(deleDocumentResponse.getBody().code)) {
			throw new DashScopeException(ErrorCodeEnum.RETRIEVER_DOCUMENT_ERROR);
		}
		List<DocumentRetrieveResponse.DocumentRetrieveResponseNode> nodeList = deleDocumentResponse.getBody().nodes;
		if (nodeList == null || nodeList.isEmpty()) {
			return new ArrayList<>();
		}
		List<Document> documents = new ArrayList<>();
		nodeList.forEach(e -> {
			DocumentRetrieveResponse.DocumentRetrieveResponseNodeData nodeData = e.node;
			Document toDocument = new Document(nodeData.id, nodeData.text, nodeData.metadata);
			documents.add(toDocument);
		});
		return documents;
	}

	/**
	 * Represents a tool the model may call. Currently, only functions are supported as a
	 * tool.
	 *
	 * @param type The type of the tool. Currently, only 'function' is supported.
	 * @param function The function definition.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record FunctionTool(@JsonProperty("type") Type type, @JsonProperty("function") Function function) {

		/**
		 * Create a tool of type 'function' and the given function definition.
		 * @param function function definition.
		 */
		public FunctionTool(Function function) {
			this(Type.FUNCTION, function);
		}

		/**
		 * Create a tool of type 'function' and the given function definition.
		 */
		public enum Type {

			/**
			 * Function tool type.
			 */
			@JsonProperty("function")
			FUNCTION

		}

		/**
		 * Function definition.
		 *
		 * @param description A description of what the function does, used by the model
		 * to choose when and how to call the function.
		 * @param name The name of the function to be called. Must be a-z, A-Z, 0-9, or
		 * contain underscores and dashes, with a maximum length of 64.
		 * @param parameters The parameters the functions accepts, described as a JSON
		 * Schema object. To describe a function that accepts no parameters, provide the
		 * value {"type": "object", "properties": {}}.
		 */
		public record Function(@JsonProperty("description") String description, @JsonProperty("name") String name,
				@JsonProperty("parameters") Map<String, Object> parameters) {

			/**
			 * Create tool function definition.
			 * @param description tool function description.
			 * @param name tool function name.
			 * @param jsonSchema tool function schema as json.
			 */
			public Function(String description, String name, String jsonSchema) {
				this(description, name, ModelOptionsUtils.jsonToMap(jsonSchema));
			}
		}
	}

	/**
	 * Creates a model response for the given chat conversation.
	 *
	 * @param model ID of the model to use.
	 * @param input request input of chat.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ChatCompletionRequest(@JsonProperty("model") String model,
			@JsonProperty("input") ChatCompletionRequestInput input,
			@JsonProperty("parameters") ChatCompletionRequestParameter parameters,
			@JsonProperty("stream") Boolean stream, @JsonIgnore Boolean multiModel) {

		/**
		 * Shortcut constructor for a chat completion request with the given messages and
		 * model.
		 * @param model ID of the model to use.
		 * @param input request input of chat.
		 */
		public ChatCompletionRequest(String model, ChatCompletionRequestInput input, Boolean stream) {
			this(model, input, null, stream, false);
		}
	}

	/**
	 * Creates a model response for the given chat conversation.
	 *
	 * @param maxTokens The maximum number of tokens to generate in the chat completion.
	 * The total length of input tokens and generated tokens is limited by the model's
	 * context length.
	 * @param stop Up to 4 sequences where the API will stop generating further tokens.
	 * @param temperature What sampling temperature to use, between 0 and 1. Higher values
	 * like 0.8 will make the output more random, while lower values like 0.2 will make it
	 * more focused and deterministic. We generally recommend altering this or top_p but
	 * not both.
	 * @param topP An alternative to sampling with temperature, called nucleus sampling,
	 * where the model considers the results of the tokens with top_p probability mass. So
	 * 0.1 means only the tokens comprising the top 10% probability mass are considered.
	 * We generally recommend altering this or temperature but not both.
	 * @param tools A list of tools the model may call. Currently, only functions are
	 * supported as a tool. Use this to provide a list of functions the model may generate
	 * JSON inputs for.
	 * @param toolChoice Controls which (if any) function is called by the model. none
	 * means the model will not call a function and instead generates a message. auto
	 * means the model can pick between generating a message or calling a function.
	 * Specifying a particular function via {"type: "function", "function": {"name":
	 * "my_function"}} forces the model to call that function. none is the default when no
	 * functions are present. auto is the default if functions are present. Use the
	 * {@link ToolChoiceBuilder} to create the tool choice value.
	 * @param stream Whether to stream back partial progress. If set, tokens will be sent
	 * as data-only server-sent events as they become available, with the stream
	 * terminated by a data: [DONE] message.
	 * @param vlHighResolutionImages Whether to generate high-resolution images for
	 * visualization.
	 * @param enableThinking Whether to enable the model to think before generating
	 * responses. This is useful for complex tasks where the model needs to reason through
	 * the problem before providing an answer.
	 *
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ChatCompletionRequestParameter(@JsonProperty("result_format") String resultFormat,
			@JsonProperty("seed") Integer seed, @JsonProperty("max_tokens") Integer maxTokens,
			@JsonProperty("top_p") Double topP, @JsonProperty("top_k") Integer topK,
			@JsonProperty("repetition_penalty") Double repetitionPenalty,
			@JsonProperty("presence_penalty") Double presencePenalty, @JsonProperty("temperature") Double temperature,
			@JsonProperty("stop") List<Object> stop, @JsonProperty("enable_search") Boolean enableSearch,
			@JsonProperty("response_format") DashScopeResponseFormat responseFormat,
			@JsonProperty("incremental_output") Boolean incrementalOutput,
			@JsonProperty("tools") List<FunctionTool> tools, @JsonProperty("tool_choice") Object toolChoice,
			@JsonProperty("stream") Boolean stream,
			@JsonProperty("vl_high_resolution_images") Boolean vlHighResolutionImages,
			@JsonProperty("enable_thinking") Boolean enableThinking,
			@JsonProperty("search_options") SearchOptions searchOptions,
			@JsonProperty("parallel_tool_calls") Boolean parallelToolCalls) {

		/**
		 * shortcut constructor for chat request parameter
		 */
		public ChatCompletionRequestParameter() {
			this(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null);
		}

		/**
		 * Helper factory that creates a tool_choice of type 'none', 'auto' or selected
		 * function by name.
		 */
		public static class ToolChoiceBuilder {

			/**
			 * Model can pick between generating a message or calling a function.
			 */
			public static final String AUTO = "auto";

			/**
			 * Model will not call a function and instead generates a message
			 */
			public static final String NONE = "none";

			/**
			 * Specifying a particular function forces the model to call that function.
			 */
			public static Object function(String functionName) {
				return Map.of("type", "function", "function", Map.of("name", functionName));
			}

		}
	}

	/**
	 * Request input of chat
	 *
	 * @param messages chat messages
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ChatCompletionRequestInput(@JsonProperty("messages") List<ChatCompletionMessage> messages) {
	}

	/**
	 * Message comprising the conversation.
	 *
	 * @param rawContent The contents of the message. Can be either a {@link MediaContent}
	 * or a {@link String}. The response message content is always a {@link String}.
	 * @param role The role of the messages author. Could be one of the {@link Role}
	 * types.
	 * @param name An optional name for the participant. Provides the model information to
	 * differentiate between participants of the same role. In case of Function calling,
	 * the name is the function name that the message is responding to.
	 * @param toolCallId Tool call that this message is responding to. Only applicable for
	 * the {@link Role#TOOL} role and null otherwise.
	 * @param toolCalls The tool calls generated by the model, such as function calls.
	 * @param reasoningContent The reasoning content of the message. <a href=
	 * "https://help.aliyun.com/zh/model-studio/developer-reference/deepseek">DeepSeek
	 * ReasoningContent</a> Applicable only for {@link Role#ASSISTANT} role and null
	 * otherwise.
	 */
	// format: off
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ChatCompletionMessage(@JsonProperty("content") Object rawContent, @JsonProperty("role") Role role,
			@JsonProperty("name") String name, @JsonProperty("tool_call_id") String toolCallId,
			@JsonProperty("tool_calls") List<ToolCall> toolCalls,
			@JsonProperty("reasoning_content") String reasoningContent) {

		/**
		 * Get message content as String.
		 */
		public String content() {
			if (this.rawContent == null) {
				return "";
			}

			if (this.rawContent instanceof String text) {
				return text;
			}

			if (this.rawContent instanceof List list) {
				if (list.isEmpty()) {
					return "";
				}

				Object object = list.get(0);
				if (object instanceof Map map) {
					if (map.isEmpty() || map.get("text") == null) {
						return "";
					}

					return map.get("text").toString();
				}
			}
			throw new IllegalStateException("The content is not valid!");
		}

		/**
		 * Create a chat completion message with the given content and role. All other
		 * fields are null.
		 * @param content The contents of the message.
		 * @param role The role of the author of this message.
		 */
		public ChatCompletionMessage(Object content, Role role) {

			this(content, role, null, null, null, null);
		}
		// format: on

		/**
		 * The role of the author of this message.
		 */
		public enum Role {

			/**
			 * System message.
			 */
			@JsonProperty("system")
			SYSTEM,
			/**
			 * User message.
			 */
			@JsonProperty("user")
			USER,
			/**
			 * Assistant message.
			 */
			@JsonProperty("assistant")
			ASSISTANT,
			/**
			 * Tool message.
			 */
			@JsonProperty("tool")
			TOOL

		}

		/**
		 * An array of content parts with a defined type. Each MediaContent can be of
		 * either "text" or "image_url" type. Not both.
		 *
		 * @param text The text content of the message.
		 * @param image The image content of the message. You can pass multiple images
		 * @param video The image list of video. by adding multiple image_url content
		 * parts. Image input is only supported when using the glm-4v model.
		 */
		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record MediaContent(@JsonProperty("type") String type, @JsonProperty("text") String text,
				@JsonProperty("image") String image, @JsonProperty("video") List<String> video) {
			/**
			 * Shortcut constructor for a text content.
			 * @param text The text content of the message.
			 */
			public MediaContent(String text) {
				this("text", text, null, null);
			}
		}

		/**
		 * The relevant tool call.
		 *
		 * @param id The ID of the tool call. This ID must be referenced when you submit
		 * the tool outputs in using the Submit tool outputs to run endpoint.
		 * @param type The type of tool call the output is required for. For now, this is
		 * always function.
		 * @param function The function definition.
		 */
		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record ToolCall(@JsonProperty("id") String id, @JsonProperty("type") String type,
				@JsonProperty("function") ChatCompletionFunction function) {
		}

		/**
		 * The function definition.
		 *
		 * @param name The name of the function.
		 * @param arguments The arguments that the model expects you to pass to the
		 * function.
		 */
		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record ChatCompletionFunction(@JsonProperty("name") String name,
				@JsonProperty("arguments") String arguments) {
		}
	}

	public static String getTextContent(List<ChatCompletionMessage.MediaContent> content) {
		return content.stream()
			.filter(c -> "text".equals(c.type()))
			.map(ChatCompletionMessage.MediaContent::text)
			.reduce("", (a, b) -> a + b);
	}

	/**
	 * The reason the model stopped generating tokens.
	 */
	public enum ChatCompletionFinishReason {

		/**
		 * normal chunk message
		 */
		@JsonProperty("null")
		NULL,

		/**
		 * The model hit a natural stop point or a provided stop sequence.
		 */
		@JsonProperty("stop")
		STOP,
		/**
		 * The maximum number of tokens specified in the request was reached.
		 */
		@JsonProperty("length")
		LENGTH,
		/**
		 * The content was omitted due to a flag from our content filters.
		 */
		@JsonProperty("content_filter")
		CONTENT_FILTER,
		/**
		 * The model called a tool.
		 */
		@JsonProperty("tool_calls")
		TOOL_CALLS,
		/**
		 * (deprecated) The model called a function.
		 */
		@JsonProperty("function_call")
		FUNCTION_CALL,
		/**
		 * Only for compatibility with Mistral AI API.
		 */
		@JsonProperty("tool_call")
		TOOL_CALL

	}

	/**
	 * Represents a chat completion response returned by model, based on the provided
	 * input.
	 *
	 * @param requestId A unique identifier for the chat completion.
	 * @param output chat completion output.
	 * @param usage Usage statistics for the completion request.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ChatCompletion(@JsonProperty("request_id") String requestId,
			@JsonProperty("output") ChatCompletionOutput output, @JsonProperty("usage") TokenUsage usage) {
	}

	/**
	 * Represents a chat completion response returned by model, based on the provided
	 * input.
	 *
	 * @param text chat completion text if result format is text.
	 * @param choices A list of chat completion choices. Can be more than one if n is
	 * greater than 1. used in conjunction with the seed request parameter to understand
	 * when backend changes have been made that might impact determinism.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ChatCompletionOutput(@JsonProperty("text") String text,
			@JsonProperty("choices") List<Choice> choices) {

		/**
		 * Chat completion choice.
		 *
		 * @param finishReason The reason the model stopped generating tokens.
		 * @param message A chat completion message generated by the model.
		 */
		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record Choice(@JsonProperty("finish_reason") ChatCompletionFinishReason finishReason,
				@JsonProperty("message") ChatCompletionMessage message) {

		}
	}

	/**
	 * Log probability information for the choice.
	 *
	 * @param content A list of message content tokens with log probability information.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record LogProbs(@JsonProperty("content") List<Content> content) {

		/**
		 * Message content tokens with log probability information.
		 *
		 * @param token The token.
		 * @param logprob The log probability of the token.
		 * @param probBytes A list of integers representing the UTF-8 bytes representation
		 * of the token. Useful in instances where characters are represented by multiple
		 * tokens and their byte representations must be combined to generate the correct
		 * text representation. Can be null if there is no bytes representation for the
		 * token.
		 * @param topLogprobs List of the most likely tokens and their log probability, at
		 * this token position. In rare cases, there may be fewer than the number of
		 * requested top_logprobs returned.
		 */
		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record Content(@JsonProperty("token") String token, @JsonProperty("logprob") Float logprob,
				@JsonProperty("bytes") List<Integer> probBytes,
				@JsonProperty("top_logprobs") List<TopLogProbs> topLogprobs) {

			/**
			 * The most likely tokens and their log probability, at this token position.
			 *
			 * @param token The token.
			 * @param logprob The log probability of the token.
			 * @param probBytes A list of integers representing the UTF-8 bytes
			 * representation of the token. Useful in instances where characters are
			 * represented by multiple tokens and their byte representations must be
			 * combined to generate the correct text representation. Can be null if there
			 * is no bytes representation for the token.
			 */
			@JsonInclude(JsonInclude.Include.NON_NULL)
			public record TopLogProbs(@JsonProperty("token") String token, @JsonProperty("logprob") Float logprob,
					@JsonProperty("bytes") List<Integer> probBytes) {
			}
		}
	}

	/**
	 * Usage statistics for the completion request.
	 *
	 * @param outputTokens Number of tokens in the generated completion. Only applicable
	 * for completion requests.
	 * @param inputTokens Number of tokens in the prompt.
	 * @param totalTokens Total number of tokens used in the request (prompt +
	 * completion).
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record TokenUsage(@JsonProperty("output_tokens") Integer outputTokens,
			@JsonProperty("input_tokens") Integer inputTokens, @JsonProperty("total_tokens") Integer totalTokens) {

	}

	/**
	 * Represents a chat completion response returned by model, based on the provided
	 * input.
	 *
	 * @param requestId A unique identifier for the chat completion.
	 * @param output chat completion output.
	 * @param usage Usage statistics for the completion request.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ChatCompletionChunk(@JsonProperty("request_id") String requestId,
			@JsonProperty("output") ChatCompletionOutput output, @JsonProperty("usage") TokenUsage usage) {
	}

	/**
	 * Represents dashscope rerank request input
	 *
	 * @param query query string for rerank.
	 * @param documents list of documents for rerank.
	 */
	public record RerankRequestInput(@JsonProperty("query") String query,
			@JsonProperty("documents") List<String> documents) {
	}

	/**
	 * Represents rerank request parameters.
	 *
	 * @param topN return top n documents, it will return all the documents if top n not
	 * pass.
	 * @param returnDocuments if need to return original documents
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record RerankRequestParameter(@JsonProperty("top_n") Integer topN,
			@JsonProperty("return_documents") Boolean returnDocuments) {
	}

	/**
	 * Represents rerank request information.
	 *
	 * @param model ID of the model to use.
	 * @param input dashscope rerank input.
	 * @param parameters rerank parameters.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record RerankRequest(@JsonProperty("model") String model, @JsonProperty("input") RerankRequestInput input,
			@JsonProperty("parameters") RerankRequestParameter parameters) {
	}

	/**
	 * Represents rerank output result
	 *
	 * @param index index of input document list
	 * @param relevanceScore relevance score between query and document
	 * @param document original document
	 */
	public record RerankResponseOutputResult(@JsonProperty("index") Integer index,
			@JsonProperty("relevance_score") Double relevanceScore,
			@JsonProperty("document") Map<String, Object> document) {
	}

	/**
	 * Represents rerank response output
	 *
	 * @param results rerank output results
	 */
	public record RerankResponseOutput(@JsonProperty("results") List<RerankResponseOutputResult> results) {
	}

	/**
	 * Represents rerank response
	 *
	 * @param output rerank response output
	 * @param usage rerank token usage
	 * @param requestId rerank request id
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record RerankResponse(@JsonProperty("output") RerankResponseOutput output,
			@JsonProperty("usage") TokenUsage usage, @JsonProperty("request_id") String requestId) {

	}

	/**
	 * Creates a model response for the given chat conversation.
	 * @param chatRequest The chat completion request.
	 * @return Entity response with {@link ChatCompletion} as a body and HTTP status code
	 * and headers.
	 */
	public ResponseEntity<ChatCompletion> chatCompletionEntity(ChatCompletionRequest chatRequest) {
		return chatCompletionEntity(chatRequest, new LinkedMultiValueMap<>());
	}

	/**
	 * Creates a model response for the given chat conversation.
	 * @param chatRequest The chat completion request.
	 * @param additionalHttpHeader Optional, additional HTTP headers to be added to the
	 * request.
	 * @return Entity response with {@link ChatCompletion} as a body and HTTP status code
	 * and headers.
	 */
	public ResponseEntity<ChatCompletion> chatCompletionEntity(ChatCompletionRequest chatRequest,
			MultiValueMap<String, String> additionalHttpHeader) {

		Assert.notNull(chatRequest, "The request body can not be null.");
		Assert.isTrue(!chatRequest.stream(), "Request must set the stream property to false.");
		Assert.notNull(additionalHttpHeader, "The additional HTTP headers can not be null.");

		var chatCompletionUri = this.completionsPath;
		if (chatRequest.multiModel()) {
			chatCompletionUri = "/api/v1/services/aigc/multimodal-generation/generation";
		}

		// @formatter:off
		return this.restClient.post()
				.uri(chatCompletionUri)
				.headers(headers -> {
					headers.addAll(additionalHttpHeader);
					addDefaultHeadersIfMissing(headers);
				})
				.body(chatRequest)
				.retrieve()
				.toEntity(ChatCompletion.class);
		// @formatter:on
	}

	private void addDefaultHeadersIfMissing(HttpHeaders headers) {

		if (!headers.containsKey(HttpHeaders.AUTHORIZATION) && !(this.apiKey instanceof NoopApiKey)) {
			headers.setBearerAuth(this.apiKey.getValue());
		}
	}

	/**
	 * Creates a streaming chat response for the given chat conversation.
	 * @param chatRequest The chat completion request. Must have the stream property set
	 * to true.
	 * @return Returns a {@link Flux} stream from chat completion chunks.
	 */
	public Flux<ChatCompletionChunk> chatCompletionStream(ChatCompletionRequest chatRequest) {

		return this.chatCompletionStream(chatRequest, null);
	}

	/**
	 * Creates a streaming chat response for the given chat conversation.
	 * @param chatRequest The chat completion request. Must have the stream property set
	 * to true.
	 * @param additionalHttpHeader Optional, additional HTTP headers to be added to the
	 * request.
	 * @return Returns a {@link Flux} stream from chat completion chunks.
	 */
	public Flux<ChatCompletionChunk> chatCompletionStream(ChatCompletionRequest chatRequest,
			MultiValueMap<String, String> additionalHttpHeader) {

		Assert.notNull(chatRequest, "The request body can not be null.");
		Assert.isTrue(chatRequest.stream(), "Request must set the stream property to true.");

		AtomicBoolean isInsideTool = new AtomicBoolean(false);
		boolean incrementalOutput = chatRequest.parameters() != null
				&& chatRequest.parameters().incrementalOutput != null && chatRequest.parameters().incrementalOutput;
		DashScopeAiStreamFunctionCallingHelper chunkMerger = new DashScopeAiStreamFunctionCallingHelper(
				incrementalOutput);

		var chatCompletionUri = this.completionsPath;
		if (chatRequest.multiModel()) {
			chatCompletionUri = "/api/v1/services/aigc/multimodal-generation/generation";
		}

		return this.webClient.post().uri(chatCompletionUri).headers(headers -> {
			headers.addAll(additionalHttpHeader);
			// For Dashscope stream
			headers.add("X-DashScope-SSE", "enable");
			addDefaultHeadersIfMissing(headers);
		})
			.body(Mono.just(chatRequest), ChatCompletionRequest.class)
			.retrieve()
			.bodyToFlux(String.class)
			.takeUntil(SSE_DONE_PREDICATE)
			.filter(SSE_DONE_PREDICATE.negate())
			.map(content -> ModelOptionsUtils.jsonToObject(content, ChatCompletionChunk.class))
			.map(chunk -> {
				if (chunkMerger.isStreamingToolFunctionCall(chunk)) {
					isInsideTool.set(true);
				}
				return chunk;
			})
			.windowUntil(chunk -> {
				if (isInsideTool.get() && chunkMerger.isStreamingToolFunctionCallFinish(chunk)) {
					isInsideTool.set(false);
					return true;
				}
				return !isInsideTool.get();
			})
			.concatMapIterable(window -> {
				Mono<ChatCompletionChunk> monoChunk = window.reduce(new ChatCompletionChunk(null, null, null),
						chunkMerger::merge);
				return List.of(monoChunk);
			})
			.flatMap(mono -> mono);
	}

	/**
	 * Creates rerank request for dashscope rerank model.
	 * @param rerankRequest The chat completion request.
	 * @return Entity response with {@link ChatCompletion} as a body and HTTP status code
	 * and headers.
	 */
	public ResponseEntity<RerankResponse> rerankEntity(RerankRequest rerankRequest) {
		Assert.notNull(rerankRequest, "The request body can not be null.");

		return this.restClient.post()
			.uri("/api/v1/services/rerank/text-rerank/text-rerank")
			.body(rerankRequest)
			.retrieve()
			.toEntity(RerankResponse.class);
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record SearchOptions(@JsonProperty("enable_source") Boolean enableSource,
			@JsonProperty("enable_citation") Boolean enableCitation,
			@JsonProperty("citation_format") String citationFormat, @JsonProperty("forced_search") Boolean forcedSearch,
			@JsonProperty("search_strategy") String searchStrategy) {

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {

			private Boolean enableSource;

			private Boolean enableCitation;

			private String citationFormat;

			private Boolean forcedSearch;

			private String searchStrategy;

			public Builder enableSource(Boolean enableSource) {
				this.enableSource = enableSource;
				return this;
			}

			public Builder enableCitation(Boolean enableCitation) {
				this.enableCitation = enableCitation;
				return this;
			}

			public Builder citationFormat(String citationFormat) {
				this.citationFormat = citationFormat;
				return this;
			}

			public Builder forcedSearch(Boolean forcedSearch) {
				this.forcedSearch = forcedSearch;
				return this;
			}

			public Builder searchStrategy(String searchStrategy) {
				this.searchStrategy = searchStrategy;
				return this;
			}

			public SearchOptions build() {
				return new SearchOptions(enableSource, enableCitation, citationFormat, forcedSearch, searchStrategy);
			}

		}
	}

	String getBaseUrl() {
		return this.baseUrl;
	}

	ApiKey getApiKey() {
		return this.apiKey;
	}

	MultiValueMap<String, String> getHeaders() {
		return this.headers;
	}

	ResponseErrorHandler getResponseErrorHandler() {
		return this.responseErrorHandler;
	}

	public static class Builder {

		public Builder() {
		}

		// Copy constructor for mutate()
		public Builder(DashScopeApi api) {
			this.baseUrl = api.getBaseUrl();
			this.apiKey = api.getApiKey();
			this.headers = new LinkedMultiValueMap<>(api.getHeaders());
			this.restClientBuilder = api.restClient != null ? api.restClient.mutate() : RestClient.builder();
			this.webClientBuilder = api.webClient != null ? api.webClient.mutate() : WebClient.builder();
			this.responseErrorHandler = api.getResponseErrorHandler();
		}

		private String baseUrl = DashScopeApiConstants.DEFAULT_BASE_URL;

		private ApiKey apiKey;

		private String workSpaceId;

		private MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();

		private String completionsPath = "/api/v1/services/aigc/text-generation/generation";

		private String embeddingsPath = "api/v1/services/embeddings/text-embedding/text-embedding";

		private RestClient.Builder restClientBuilder = RestClient.builder();

		private WebClient.Builder webClientBuilder = WebClient.builder();

		private ResponseErrorHandler responseErrorHandler = RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER;

		public Builder baseUrl(String baseUrl) {

			Assert.notNull(baseUrl, "Base URL cannot be null");
			this.baseUrl = baseUrl;
			return this;
		}

		public Builder workSpaceId(String workSpaceId) {
			// Workspace ID is optional, but if provided, it must not be null.
			if (StringUtils.hasText(workSpaceId)) {
				Assert.notNull(workSpaceId, "Workspace ID cannot be null");
			}
			this.workSpaceId = workSpaceId;
			return this;
		}

		public Builder apiKey(String simpleApiKey) {
			Assert.notNull(simpleApiKey, "Simple api key cannot be null");
			this.apiKey = new SimpleApiKey(simpleApiKey);
			return this;
		}

		public Builder headers(MultiValueMap<String, String> headers) {
			Assert.notNull(headers, "Headers cannot be null");
			this.headers = headers;
			return this;
		}

		public Builder restClientBuilder(RestClient.Builder restClientBuilder) {
			Assert.notNull(restClientBuilder, "Rest client builder cannot be null");
			this.restClientBuilder = restClientBuilder;
			return this;
		}

		public Builder completionsPath(String completionsPath) {
			Assert.notNull(completionsPath, "Completions path cannot be null");
			this.completionsPath = completionsPath;
			return this;
		}

		public Builder embeddingsPath(String embeddingsPath) {
			Assert.notNull(embeddingsPath, "Embeddings path cannot be null");
			this.embeddingsPath = embeddingsPath;
			return this;
		}

		public Builder webClientBuilder(WebClient.Builder webClientBuilder) {
			Assert.notNull(webClientBuilder, "Web client builder cannot be null");
			this.webClientBuilder = webClientBuilder;
			return this;
		}

		public Builder responseErrorHandler(ResponseErrorHandler responseErrorHandler) {
			Assert.notNull(responseErrorHandler, "Response error handler cannot be null");
			this.responseErrorHandler = responseErrorHandler;
			return this;
		}

		public DashScopeApi build() {

			Assert.notNull(apiKey, "API key cannot be null");

			return new DashScopeApi(this.baseUrl, this.apiKey, this.headers, this.completionsPath, this.embeddingsPath,
					// Add request header.
					this.workSpaceId, this.restClientBuilder, this.webClientBuilder, this.responseErrorHandler);
		}

	}

}
