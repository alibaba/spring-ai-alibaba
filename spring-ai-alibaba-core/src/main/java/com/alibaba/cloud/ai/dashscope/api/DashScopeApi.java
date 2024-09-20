package com.alibaba.cloud.ai.dashscope.api;

import com.alibaba.cloud.ai.dashscope.common.DashScopeException;
import com.alibaba.cloud.ai.dashscope.common.ErrorCodeEnum;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentTransformerOptions;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeStoreOptions;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.ChatModelDescription;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.DEFAULT_BASE_URL;

/**
 * @author nuocheng.lxm
 * @author yuluo
 *
 * @date 2024/7/31 14:15
 */
public class DashScopeApi {

	private static final Predicate<String> SSE_DONE_PREDICATE = "[DONE]"::equals;

	/** Default chat model */
	public static final String DEFAULT_CHAT_MODEL = ChatModel.QWEN_PLUS.getModel();

	public static final String DEFAULT_EMBEDDING_MODEL = EmbeddingModel.EMBEDDING_V2.getValue();

	public static final String DEFAULT_EMBEDDING_TEXT_TYPE = EmbeddingTextType.DOCUMENT.getValue();

	public static final String DEFAULT_PARSER_NAME = "DASHSCOPE_DOCMIND";

	private final RestClient restClient;

	private final WebClient webClient;

	public DashScopeApi(String apiKey) {
		this(DEFAULT_BASE_URL, apiKey, RestClient.builder(), WebClient.builder(),
				RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
	}

	public DashScopeApi(String apiKey, String workSpaceId) {
		this(DEFAULT_BASE_URL, apiKey, workSpaceId, RestClient.builder(), WebClient.builder(),
				RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
	}

	public DashScopeApi(String baseUrl, String apiKey, String workSpaceId) {
		this(baseUrl, apiKey, workSpaceId, RestClient.builder(), WebClient.builder(),
				RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
	}

	public DashScopeApi(String baseUrl, String apiKey, RestClient.Builder restClientBuilder,
			WebClient.Builder webClientBuilder, ResponseErrorHandler responseErrorHandler) {
		this.restClient = restClientBuilder.baseUrl(baseUrl)
			.defaultHeaders(ApiUtils.getJsonContentHeaders(apiKey))
			.defaultStatusHandler(responseErrorHandler)
			.build();

		this.webClient = webClientBuilder.baseUrl(baseUrl)
			.defaultHeaders(ApiUtils.getJsonContentHeaders(apiKey))
			.build();
	}

	public DashScopeApi(String baseUrl, String apiKey, String workSpaceId, RestClient.Builder restClientBuilder,
			WebClient.Builder webClientBuilder, ResponseErrorHandler responseErrorHandler) {
		this.restClient = restClientBuilder.baseUrl(baseUrl)
			.defaultHeaders(ApiUtils.getJsonContentHeaders(apiKey, workSpaceId))
			.defaultStatusHandler(responseErrorHandler)
			.build();

		this.webClient = webClientBuilder.baseUrl(baseUrl)
			.defaultHeaders(ApiUtils.getJsonContentHeaders(apiKey, workSpaceId))
			.build();
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record CommonResponse<T>(@JsonProperty("code") String code, @JsonProperty("message") String message,
			@JsonProperty("data") T data) {
	}

	/*
	 * Dashscope Chat Completion Models: <a
	 * href="https://help.aliyun.com/zh/dashscope/developer-reference/api-details">
	 * Dashscope Chat API</a>
	 */
	public enum ChatModel {

		/** 模型支持8k tokens上下文，为了保证正常的使用和输出，API限定用户输入为6k tokens。 */
		QWEN_PLUS("qwen-plus"),

		/** 模型支持32k tokens上下文，为了保证正常的使用和输出，API限定用户输入为30k tokens。 */
		QWEN_TURBO("qwen-turbo"),

		/** 模型支持8k tokens上下文，为了保证正常的使用和输出，API限定用户输入为6k tokens。 */
		QWEN_MAX("qwen-max"),

		/** 模型支持30k tokens上下文，为了保证正常的使用和输出，API限定用户输入为28k tokens。 */
		QWEN_MAX_LONGCONTEXT("qwen-max-longcontext");

		private final String model;

		ChatModel(String model) {
			this.model = model;
		}

		public String getModel() {
			return this.model;
		}

	}

	/*******************************************
	 * Embedding相关
	 **********************************************/

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
		 * DIMENSION: 1024/768/512
		 */
		EMBEDDING_V3("text-embedding-v3");

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
		public Long getPromptTokens() {
			return null;
		}

		@Override
		public Long getGenerationTokens() {
			return null;
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Embedding(@JsonProperty("text_index") Integer textIndex,
			@JsonProperty("embedding") float[] embedding) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record EmbeddingList(@JsonProperty("request_id") String requestId, @JsonProperty("code") String code,
			@JsonProperty("message") String message, @JsonProperty("output") Embeddings output,
			@JsonProperty("usage") EmbeddingUsage usage) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Embeddings(@JsonProperty("embeddings") List<Embedding> embeddings) {

	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record EmbeddingRequestInput(@JsonProperty("texts") List<String> texts) {

	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record EmbeddingRequestInputParameters(@JsonProperty("text_type") String textType) {

	}

	/**
	 * Creates an embedding vector representing the input text.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record EmbeddingRequest(@JsonProperty("model") String model,
			@JsonProperty("input") EmbeddingRequestInput input,
			@JsonProperty("parameters") EmbeddingRequestInputParameters parameters) {
		public EmbeddingRequest(String text) {
			this(DEFAULT_EMBEDDING_MODEL, new EmbeddingRequestInput(List.of(text)),
					new EmbeddingRequestInputParameters(DEFAULT_EMBEDDING_TEXT_TYPE));
		}

		public EmbeddingRequest(String text, String model) {
			this(model, new EmbeddingRequestInput(List.of(text)),
					new EmbeddingRequestInputParameters(DEFAULT_EMBEDDING_TEXT_TYPE));
		}

		public EmbeddingRequest(String text, String model, String textType) {
			this(model, new EmbeddingRequestInput(List.of(text)), new EmbeddingRequestInputParameters(textType));
		}

		public EmbeddingRequest(List<String> texts) {
			this(DEFAULT_EMBEDDING_MODEL, new EmbeddingRequestInput(texts),
					new EmbeddingRequestInputParameters(DEFAULT_EMBEDDING_TEXT_TYPE));
		}

		public EmbeddingRequest(List<String> texts, String model) {
			this(model, new EmbeddingRequestInput(texts),
					new EmbeddingRequestInputParameters(DEFAULT_EMBEDDING_TEXT_TYPE));
		}

		public EmbeddingRequest(List<String> texts, String model, String textType) {
			this(model, new EmbeddingRequestInput(texts), new EmbeddingRequestInputParameters(textType));
		}
	}

	public ResponseEntity<EmbeddingList> embeddings(EmbeddingRequest embeddingRequest) {
		Assert.notNull(embeddingRequest, "The request body can not be null.");
		Assert.notNull(embeddingRequest.input(), "The input can not be null.");
		Assert.isTrue(!CollectionUtils.isEmpty(embeddingRequest.input().texts()), "The input texts can not be empty.");
		Assert.isTrue(embeddingRequest.input().texts().size() <= 25, "The input texts limit 25.");
		return this.restClient.post()
			.uri("/api/v1/services/embeddings/text-embedding/text-embedding")
			.body(embeddingRequest)
			.retrieve()
			.toEntity(EmbeddingList.class);
	}

	/*******************************************
	 * 数据中心相关
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
		// 申请上传
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
					DEFAULT_PARSER_NAME);
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
			RestTemplate restTemplate = new RestTemplate();
			HttpHeaders headers = new HttpHeaders();
			String contentType = uploadParam.header.remove("Content-Type");
			headers.setContentType(MediaType.parseMediaType(contentType));
			for (String key : uploadParam.header.keySet()) {
				headers.set(key, uploadParam.header.get(key));
			}
			InputStreamResource resource = new InputStreamResource(new FileInputStream(file)) {
				@Override
				public long contentLength() {
					return file.length();
				}

				@Override
				public String getFilename() {
					return file.getName();
				}
			};
			HttpEntity<InputStreamResource> requestEntity = new HttpEntity<>(resource, headers);
			restTemplate.exchange(new URI(uploadParam.url), HttpMethod.PUT, requestEntity, Void.class);
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
		DocumentSplitRequest request = new DocumentSplitRequest(document.getContent(), options.getChunkSize(),
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
					@JsonProperty("rerank_top_n") int rerankTopN) {

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
			@JsonProperty("rerank_min_score") float rerankMinScore, @JsonProperty("rerank_top_n") int rerankTopN) {
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
						retrieverOptions.getRerankMinScore(), retrieverOptions.getRerankTopN()));
		List<String> documentIdList = documents.stream()
			.map(Document::getId)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
		UpsertPipelineRequest upsertPipelineRequest = new UpsertPipelineRequest(storeOptions.getIndexName(),
				"MANAGED_SHARED", null, "unstructured", "recommend",
				Arrays.asList(embeddingConfig, parserConfig, retrieverConfig),
				Arrays.asList(new UpsertPipelineRequest.DataSourcesConfig("DATA_CENTER_FILE",
						new UpsertPipelineRequest.DataSourcesConfig.DataSourcesComponent(documentIdList))),
				Arrays.asList(new UpsertPipelineRequest.DataSinksConfig("ES", null))

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
		if (startPipelineResponse.getBody() == null
				|| !"SUCCESS".equalsIgnoreCase(startPipelineResponse.getBody().code)
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
				Arrays.asList(new DocumentRetrieveRequest.DocumentRetrieveModelConfig(
						searchOption.getRewriteModelName(), "DashScopeTextRewrite")),
				searchOption.isEnableReranking(),
				Arrays.asList(new DocumentRetrieveRequest.DocumentRetrieveModelConfig(searchOption.getRerankModelName(),
						null)),
				searchOption.getRerankMinScore(), searchOption.getRerankTopN());
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
		@ConstructorBinding
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
			@ConstructorBinding
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
	 *
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ChatCompletionRequest(@JsonProperty("model") String model,
			@JsonProperty("input") ChatCompletionRequestInput input,
			@JsonProperty("parameters") ChatCompletionRequestParameter parameters,
			@JsonProperty("stream") Boolean stream) {

		/**
		 * Shortcut constructor for a chat completion request with the given messages and
		 * model.
		 * @param model ID of the model to use.
		 * @param input request input of chat.
		 */
		public ChatCompletionRequest(String model, ChatCompletionRequestInput input, Boolean stream) {
			this(model, input, null, stream);
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
	 *
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ChatCompletionRequestParameter(@JsonProperty("result_format") String resultFormat,
			@JsonProperty("seed") Integer seed, @JsonProperty("max_tokens") Integer maxTokens,
			@JsonProperty("top_p") Float topP, @JsonProperty("top_k") Integer topK,
			@JsonProperty("repetition_penalty") Float repetitionPenalty,
			@JsonProperty("presence_penalty") Float presencePenalty, @JsonProperty("temperature") Float temperature,
			@JsonProperty("stop") List<Object> stop, @JsonProperty("enable_search") Boolean enableSearch,
			@JsonProperty("incremental_output") Boolean incrementalOutput,
			@JsonProperty("tools") List<FunctionTool> tools, @JsonProperty("tool_choice") Object toolChoice) {

		/**
		 * shortcut constructor for chat request parameter
		 */
		public ChatCompletionRequestParameter() {
			this(null, null, null, null, null, null, null, null, null, null, null, null, null);
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

		/**
		 * An object specifying the format that the model must output.
		 *
		 * @param type Must be one of 'text' or 'json_object'.
		 */
		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record ResponseFormat(@JsonProperty("type") String type) {
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
	 * Applicable only for {@link Role#ASSISTANT} role and null otherwise.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ChatCompletionMessage(@JsonProperty("content") Object rawContent, @JsonProperty("role") Role role,
			@JsonProperty("name") String name, @JsonProperty("tool_call_id") String toolCallId,
			@JsonProperty("tool_calls") List<ToolCall> toolCalls) {

		/**
		 * Get message content as String.
		 */
		public String content() {
			if (this.rawContent == null) {
				return null;
			}
			if (this.rawContent instanceof String text) {
				return text;
			}
			throw new IllegalStateException("The content is not a string!");
		}

		/**
		 * Create a chat completion message with the given content and role. All other
		 * fields are null.
		 * @param content The contents of the message.
		 * @param role The role of the author of this message.
		 */
		public ChatCompletionMessage(Object content, Role role) {
			this(content, role, null, null, null);
		}

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
		 * @param type Content type, each can be of type text or image_url.
		 * @param text The text content of the message.
		 * @param imageUrl The image content of the message. You can pass multiple images
		 * by adding multiple image_url content parts. Image input is only supported when
		 * using the glm-4v model.
		 */
		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record MediaContent(@JsonProperty("type") String type, @JsonProperty("text") String text,
				@JsonProperty("image_url") ImageUrl imageUrl) {

			/**
			 * @param url Either a URL of the image or the base64 encoded image data. The
			 * base64 encoded image data must have a special prefix in the following
			 * format: "data:{mimetype};base64,{base64-encoded-image-data}".
			 * @param detail Specifies the detail level of the image.
			 */
			@JsonInclude(JsonInclude.Include.NON_NULL)
			public record ImageUrl(@JsonProperty("url") String url, @JsonProperty("detail") String detail) {

				public ImageUrl(String url) {
					this(url, null);
				}
			}

			/**
			 * Shortcut constructor for a text content.
			 * @param text The text content of the message.
			 */
			public MediaContent(String text) {
				this("text", text, null);
			}

			/**
			 * Shortcut constructor for an image content.
			 * @param imageUrl The image content of the message.
			 */
			public MediaContent(ImageUrl imageUrl) {
				this("image_url", null, imageUrl);
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
	 * Creates a model response for the given chat conversation.
	 * @param chatRequest The chat completion request.
	 * @return Entity response with {@link ChatCompletion} as a body and HTTP status code
	 * and headers.
	 */
	public ResponseEntity<ChatCompletion> chatCompletionEntity(ChatCompletionRequest chatRequest) {

		Assert.notNull(chatRequest, "The request body can not be null.");
		Assert.isTrue(!chatRequest.stream(), "Request must set the stream property to false.");

		return this.restClient.post()
			.uri("/api/v1/services/aigc/text-generation/generation")
			.body(chatRequest)
			.retrieve()
			.toEntity(ChatCompletion.class);
	}

	private final DashScopeAiStreamFunctionCallingHelper chunkMerger = new DashScopeAiStreamFunctionCallingHelper();

	/**
	 * Creates a streaming chat response for the given chat conversation.
	 * @param chatRequest The chat completion request. Must have the stream property set
	 * to true.
	 * @return Returns a {@link Flux} stream from chat completion chunks.
	 */
	public Flux<ChatCompletionChunk> chatCompletionStream(ChatCompletionRequest chatRequest) {

		Assert.notNull(chatRequest, "The request body can not be null.");
		Assert.isTrue(chatRequest.stream(), "Request must set the stream property to true.");

		AtomicBoolean isInsideTool = new AtomicBoolean(false);

		return this.webClient.post()
			.uri("/api/v1/services/aigc/text-generation/generation")
			.header("X-DashScope-SSE", "enable")
			.body(Mono.just(chatRequest), ChatCompletionRequest.class)
			.retrieve()
			.bodyToFlux(String.class)
			.takeUntil(SSE_DONE_PREDICATE)
			.filter(SSE_DONE_PREDICATE.negate())
			.map(content -> ModelOptionsUtils.jsonToObject(content, ChatCompletionChunk.class))
			.map(chunk -> {
				if (this.chunkMerger.isStreamingToolFunctionCall(chunk)) {
					isInsideTool.set(true);
				}
				return chunk;
			})
			.windowUntil(chunk -> {
				if (isInsideTool.get() && this.chunkMerger.isStreamingToolFunctionCallFinish(chunk)) {
					isInsideTool.set(false);
					return true;
				}
				return !isInsideTool.get();
			})
			.concatMapIterable(window -> {
				Mono<ChatCompletionChunk> monoChunk = window.reduce(new ChatCompletionChunk(null, null, null),
						this.chunkMerger::merge);
				return List.of(monoChunk);
			})
			.flatMap(mono -> mono);
	}

}
