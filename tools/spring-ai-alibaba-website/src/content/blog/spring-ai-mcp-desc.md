---
title: Spring AI 源码解析：MCP链路调用流程及示例
keywords: [Spring AI, Spring AI Alibaba, MCP]
description: java版本MCP源码梳理，Client-Server交互流程及示例
author: 影子
date: "2025-04-01"
category: article
---

MCP官方文档：https://modelcontextprotocol.io/introduction

java版的MCP源码：https://github.com/modelcontextprotocol/java-sdk

- 本版源码解析，取自[mcp/java-sdk（20250322）](https://github.com/modelcontextprotocol/java-sdk)，等正式发版后会再度更新

## 理论部分

![](/img/blog/mcp-desc/overall.PNG)

### MCP调用链路（核心）

以client-webflux、server-webflux为例

初始化连接链路...

![](/img/blog/mcp-desc/initialize.png)

客户端咨询问题，调用服务端工具链路

![](/img/blog/mcp-desc/interactive.png)

### McpClient（客户端）

用于创建MCP客户端的工厂类，提供了同步、异步客户端的方法，并支持多种配置，提供如下核心功能

- 配置选项：运行设置请求超时、客户端能力、客户端信息、根URI等
- 工具和资源管理：支持工具发现、资源访问、提示模版处理等
- 实时更新：通过变更消费者接收工具、资源和提示的实时更新
- 日志记录：支持结构化日志记录，提供多种日志级别和日志消费者配置

内部类

- AsyncSpec：配置异步MCP服务器的构建器类
- SyncSpec：配置同步MCP服务器的构建器类

```Java
public interface McpClient {

    
    static SyncSpec sync(McpClientTransport transport) {
       return new SyncSpec(transport);
    }

    
    static AsyncSpec async(McpClientTransport transport) {
       return new AsyncSpec(transport);
    }

    
    class SyncSpec {

       private final McpClientTransport transport;

       private Duration requestTimeout = Duration.ofSeconds(20); // Default timeout

       private Duration initializationTimeout = Duration.ofSeconds(20);

       private ClientCapabilities capabilities;

       private Implementation clientInfo = new Implementation("Java SDK MCP Client", "1.0.0");

       private final Map<String, Root> roots = new HashMap<>();

       private final List<Consumer<List<McpSchema.Tool>>> toolsChangeConsumers = new ArrayList<>();

       private final List<Consumer<List<McpSchema.Resource>>> resourcesChangeConsumers = new ArrayList<>();

       private final List<Consumer<List<McpSchema.Prompt>>> promptsChangeConsumers = new ArrayList<>();

       private final List<Consumer<McpSchema.LoggingMessageNotification>> loggingConsumers = new ArrayList<>();

       private Function<CreateMessageRequest, CreateMessageResult> samplingHandler;

       private SyncSpec(McpClientTransport transport) {
          Assert.notNull(transport, "Transport must not be null");
          this.transport = transport;
       }

       
       public SyncSpec requestTimeout(Duration requestTimeout) {
          Assert.notNull(requestTimeout, "Request timeout must not be null");
          this.requestTimeout = requestTimeout;
          return this;
       }

       
       public SyncSpec initializationTimeout(Duration initializationTimeout) {
          Assert.notNull(initializationTimeout, "Initialization timeout must not be null");
          this.initializationTimeout = initializationTimeout;
          return this;
       }

       
       public SyncSpec capabilities(ClientCapabilities capabilities) {
          Assert.notNull(capabilities, "Capabilities must not be null");
          this.capabilities = capabilities;
          return this;
       }

       
       public SyncSpec clientInfo(Implementation clientInfo) {
          Assert.notNull(clientInfo, "Client info must not be null");
          this.clientInfo = clientInfo;
          return this;
       }

       
       public SyncSpec roots(List<Root> roots) {
          Assert.notNull(roots, "Roots must not be null");
          for (Root root : roots) {
             this.roots.put(root.uri(), root);
          }
          return this;
       }

       
       public SyncSpec roots(Root... roots) {
          Assert.notNull(roots, "Roots must not be null");
          for (Root root : roots) {
             this.roots.put(root.uri(), root);
          }
          return this;
       }

       
       public SyncSpec sampling(Function<CreateMessageRequest, CreateMessageResult> samplingHandler) {
          Assert.notNull(samplingHandler, "Sampling handler must not be null");
          this.samplingHandler = samplingHandler;
          return this;
       }

       
       public SyncSpec toolsChangeConsumer(Consumer<List<McpSchema.Tool>> toolsChangeConsumer) {
          Assert.notNull(toolsChangeConsumer, "Tools change consumer must not be null");
          this.toolsChangeConsumers.add(toolsChangeConsumer);
          return this;
       }

       
       public SyncSpec resourcesChangeConsumer(Consumer<List<McpSchema.Resource>> resourcesChangeConsumer) {
          Assert.notNull(resourcesChangeConsumer, "Resources change consumer must not be null");
          this.resourcesChangeConsumers.add(resourcesChangeConsumer);
          return this;
       }

       
       public SyncSpec promptsChangeConsumer(Consumer<List<McpSchema.Prompt>> promptsChangeConsumer) {
          Assert.notNull(promptsChangeConsumer, "Prompts change consumer must not be null");
          this.promptsChangeConsumers.add(promptsChangeConsumer);
          return this;
       }

       
       public SyncSpec loggingConsumer(Consumer<McpSchema.LoggingMessageNotification> loggingConsumer) {
          Assert.notNull(loggingConsumer, "Logging consumer must not be null");
          this.loggingConsumers.add(loggingConsumer);
          return this;
       }

       
       public SyncSpec loggingConsumers(List<Consumer<McpSchema.LoggingMessageNotification>> loggingConsumers) {
          Assert.notNull(loggingConsumers, "Logging consumers must not be null");
          this.loggingConsumers.addAll(loggingConsumers);
          return this;
       }

       
       public McpSyncClient build() {
          McpClientFeatures.Sync syncFeatures = new McpClientFeatures.Sync(this.clientInfo, this.capabilities,
                this.roots, this.toolsChangeConsumers, this.resourcesChangeConsumers, this.promptsChangeConsumers,
                this.loggingConsumers, this.samplingHandler);

          McpClientFeatures.Async asyncFeatures = McpClientFeatures.Async.fromSync(syncFeatures);

          return new McpSyncClient(
                new McpAsyncClient(transport, this.requestTimeout, this.initializationTimeout, asyncFeatures));
       }

    }

    
    class AsyncSpec {

       private final McpClientTransport transport;

       private Duration requestTimeout = Duration.ofSeconds(20); // Default timeout

       private Duration initializationTimeout = Duration.ofSeconds(20);

       private ClientCapabilities capabilities;

       private Implementation clientInfo = new Implementation("Spring AI MCP Client", "0.3.1");

       private final Map<String, Root> roots = new HashMap<>();

       private final List<Function<List<McpSchema.Tool>, Mono<Void>>> toolsChangeConsumers = new ArrayList<>();

       private final List<Function<List<McpSchema.Resource>, Mono<Void>>> resourcesChangeConsumers = new ArrayList<>();

       private final List<Function<List<McpSchema.Prompt>, Mono<Void>>> promptsChangeConsumers = new ArrayList<>();

       private final List<Function<McpSchema.LoggingMessageNotification, Mono<Void>>> loggingConsumers = new ArrayList<>();

       private Function<CreateMessageRequest, Mono<CreateMessageResult>> samplingHandler;

       private AsyncSpec(McpClientTransport transport) {
          Assert.notNull(transport, "Transport must not be null");
          this.transport = transport;
       }

       
       public AsyncSpec requestTimeout(Duration requestTimeout) {
          Assert.notNull(requestTimeout, "Request timeout must not be null");
          this.requestTimeout = requestTimeout;
          return this;
       }

       
       public AsyncSpec initializationTimeout(Duration initializationTimeout) {
          Assert.notNull(initializationTimeout, "Initialization timeout must not be null");
          this.initializationTimeout = initializationTimeout;
          return this;
       }

       
       public AsyncSpec capabilities(ClientCapabilities capabilities) {
          Assert.notNull(capabilities, "Capabilities must not be null");
          this.capabilities = capabilities;
          return this;
       }

       
       public AsyncSpec clientInfo(Implementation clientInfo) {
          Assert.notNull(clientInfo, "Client info must not be null");
          this.clientInfo = clientInfo;
          return this;
       }

       
       public AsyncSpec roots(List<Root> roots) {
          Assert.notNull(roots, "Roots must not be null");
          for (Root root : roots) {
             this.roots.put(root.uri(), root);
          }
          return this;
       }

       
       public AsyncSpec roots(Root... roots) {
          Assert.notNull(roots, "Roots must not be null");
          for (Root root : roots) {
             this.roots.put(root.uri(), root);
          }
          return this;
       }

       
       public AsyncSpec sampling(Function<CreateMessageRequest, Mono<CreateMessageResult>> samplingHandler) {
          Assert.notNull(samplingHandler, "Sampling handler must not be null");
          this.samplingHandler = samplingHandler;
          return this;
       }

       
       public AsyncSpec toolsChangeConsumer(Function<List<McpSchema.Tool>, Mono<Void>> toolsChangeConsumer) {
          Assert.notNull(toolsChangeConsumer, "Tools change consumer must not be null");
          this.toolsChangeConsumers.add(toolsChangeConsumer);
          return this;
       }

       
       public AsyncSpec resourcesChangeConsumer(
             Function<List<McpSchema.Resource>, Mono<Void>> resourcesChangeConsumer) {
          Assert.notNull(resourcesChangeConsumer, "Resources change consumer must not be null");
          this.resourcesChangeConsumers.add(resourcesChangeConsumer);
          return this;
       }

       
       public AsyncSpec promptsChangeConsumer(Function<List<McpSchema.Prompt>, Mono<Void>> promptsChangeConsumer) {
          Assert.notNull(promptsChangeConsumer, "Prompts change consumer must not be null");
          this.promptsChangeConsumers.add(promptsChangeConsumer);
          return this;
       }

       
       public AsyncSpec loggingConsumer(Function<McpSchema.LoggingMessageNotification, Mono<Void>> loggingConsumer) {
          Assert.notNull(loggingConsumer, "Logging consumer must not be null");
          this.loggingConsumers.add(loggingConsumer);
          return this;
       }

       
       public AsyncSpec loggingConsumers(
             List<Function<McpSchema.LoggingMessageNotification, Mono<Void>>> loggingConsumers) {
          Assert.notNull(loggingConsumers, "Logging consumers must not be null");
          this.loggingConsumers.addAll(loggingConsumers);
          return this;
       }

       
       public McpAsyncClient build() {
          return new McpAsyncClient(this.transport, this.requestTimeout, this.initializationTimeout,
                new McpClientFeatures.Async(this.clientInfo, this.capabilities, this.roots,
                      this.toolsChangeConsumers, this.resourcesChangeConsumers, this.promptsChangeConsumers,
                      this.loggingConsumers, this.samplingHandler));
       }

    }

}
```

#### McpAsyncClient

异步客户端实现，基于Project Reactor的Mono与Flux类型，支持非阻塞操作

初始化与关闭

- initialize：初始化客户端与服务端的连接，协商协议版本、交换能力并共享实现信息
- close：立即关闭客户端连接
- closeGracefully：优雅关闭客户端连接

工具操作

- callTool：调用服务器提供的工具
- listTools：获取服务器提供的工具列表

服务器与客户端信息

- getServerInfo：获取服务器的实现信息
- getServerCapabilities：获取服务器支持的功能和能力
- getClientInfo：获取客户端的实现信息
- getClientCapabilities：获取客户端支持的功能和能力

资源操作

- listResources：获取服务器提供的资源列表
- readResource：获取特定资源的内容
- listResourceTemplates：获取服务器提供的资源模版列表
- subscribeResource：资源订阅变更通知
- unsubscribeResource：取消资源变更订阅

提示操作

- listPrompts：获取服务器提供的提示列表
- getPrompt：获取特定提示的详细信息

日志操作

- setLoggingLevel：设置日志级别

其他工具方法：

- ping：向服务器发送ping请求
- addRoot：添加根路径
- removeRoot：移除根路径
- rootsListChangedNotification：手动发送根路径变更通知

```Java
public class McpAsyncClient {

    private static final Logger logger = LoggerFactory.getLogger(McpAsyncClient.class);

    private static TypeReference<Void> VOID_TYPE_REFERENCE = new TypeReference<>() {
    };

    protected final Sinks.One<McpSchema.InitializeResult> initializedSink = Sinks.one();

    private AtomicBoolean initialized = new AtomicBoolean(false);

    
    private final Duration initializationTimeout;

    
    private final McpClientSession mcpSession;

    
    private final McpSchema.ClientCapabilities clientCapabilities;

    
    private final McpSchema.Implementation clientInfo;

    
    private McpSchema.ServerCapabilities serverCapabilities;

    
    private McpSchema.Implementation serverInfo;

    
    private final ConcurrentHashMap<String, Root> roots;

    
    private Function<CreateMessageRequest, Mono<CreateMessageResult>> samplingHandler;

    
    private final McpTransport transport;

    
    private List<String> protocolVersions = List.of(McpSchema.LATEST_PROTOCOL_VERSION);

    
    McpAsyncClient(McpClientTransport transport, Duration requestTimeout, Duration initializationTimeout,
          McpClientFeatures.Async features) {

       Assert.notNull(transport, "Transport must not be null");
       Assert.notNull(requestTimeout, "Request timeout must not be null");
       Assert.notNull(initializationTimeout, "Initialization timeout must not be null");

       this.clientInfo = features.clientInfo();
       this.clientCapabilities = features.clientCapabilities();
       this.transport = transport;
       this.roots = new ConcurrentHashMap<>(features.roots());
       this.initializationTimeout = initializationTimeout;

       // Request Handlers
       Map<String, RequestHandler<?>> requestHandlers = new HashMap<>();

       // Roots List Request Handler
       if (this.clientCapabilities.roots() != null) {
          requestHandlers.put(McpSchema.METHOD_ROOTS_LIST, rootsListRequestHandler());
       }

       // Sampling Handler
       if (this.clientCapabilities.sampling() != null) {
          if (features.samplingHandler() == null) {
             throw new McpError("Sampling handler must not be null when client capabilities include sampling");
          }
          this.samplingHandler = features.samplingHandler();
          requestHandlers.put(McpSchema.METHOD_SAMPLING_CREATE_MESSAGE, samplingCreateMessageHandler());
       }

       // Notification Handlers
       Map<String, NotificationHandler> notificationHandlers = new HashMap<>();

       // Tools Change Notification
       List<Function<List<McpSchema.Tool>, Mono<Void>>> toolsChangeConsumersFinal = new ArrayList<>();
       toolsChangeConsumersFinal
          .add((notification) -> Mono.fromRunnable(() -> logger.debug("Tools changed: {}", notification)));

       if (!Utils.isEmpty(features.toolsChangeConsumers())) {
          toolsChangeConsumersFinal.addAll(features.toolsChangeConsumers());
       }
       notificationHandlers.put(McpSchema.METHOD_NOTIFICATION_TOOLS_LIST_CHANGED,
             asyncToolsChangeNotificationHandler(toolsChangeConsumersFinal));

       // Resources Change Notification
       List<Function<List<McpSchema.Resource>, Mono<Void>>> resourcesChangeConsumersFinal = new ArrayList<>();
       resourcesChangeConsumersFinal
          .add((notification) -> Mono.fromRunnable(() -> logger.debug("Resources changed: {}", notification)));

       if (!Utils.isEmpty(features.resourcesChangeConsumers())) {
          resourcesChangeConsumersFinal.addAll(features.resourcesChangeConsumers());
       }

       notificationHandlers.put(McpSchema.METHOD_NOTIFICATION_RESOURCES_LIST_CHANGED,
             asyncResourcesChangeNotificationHandler(resourcesChangeConsumersFinal));

       // Prompts Change Notification
       List<Function<List<McpSchema.Prompt>, Mono<Void>>> promptsChangeConsumersFinal = new ArrayList<>();
       promptsChangeConsumersFinal
          .add((notification) -> Mono.fromRunnable(() -> logger.debug("Prompts changed: {}", notification)));
       if (!Utils.isEmpty(features.promptsChangeConsumers())) {
          promptsChangeConsumersFinal.addAll(features.promptsChangeConsumers());
       }
       notificationHandlers.put(McpSchema.METHOD_NOTIFICATION_PROMPTS_LIST_CHANGED,
             asyncPromptsChangeNotificationHandler(promptsChangeConsumersFinal));

       // Utility Logging Notification
       List<Function<LoggingMessageNotification, Mono<Void>>> loggingConsumersFinal = new ArrayList<>();
       loggingConsumersFinal.add((notification) -> Mono.fromRunnable(() -> logger.debug("Logging: {}", notification)));
       if (!Utils.isEmpty(features.loggingConsumers())) {
          loggingConsumersFinal.addAll(features.loggingConsumers());
       }
       notificationHandlers.put(McpSchema.METHOD_NOTIFICATION_MESSAGE,
             asyncLoggingNotificationHandler(loggingConsumersFinal));

       this.mcpSession = new McpClientSession(requestTimeout, transport, requestHandlers, notificationHandlers);

    }

    
    public McpSchema.ServerCapabilities getServerCapabilities() {
       return this.serverCapabilities;
    }

    
    public McpSchema.Implementation getServerInfo() {
       return this.serverInfo;
    }

    
    public boolean isInitialized() {
       return this.initialized.get();
    }

    
    public ClientCapabilities getClientCapabilities() {
       return this.clientCapabilities;
    }

    
    public McpSchema.Implementation getClientInfo() {
       return this.clientInfo;
    }

    
    public void close() {
       this.mcpSession.close();
    }

    
    public Mono<Void> closeGracefully() {
       return this.mcpSession.closeGracefully();
    }

    // --------------------------
    // Initialization
    // --------------------------
    
    public Mono<McpSchema.InitializeResult> initialize() {

       String latestVersion = this.protocolVersions.get(this.protocolVersions.size() - 1);

       McpSchema.InitializeRequest initializeRequest = new McpSchema.InitializeRequest(// @formatter:off
             latestVersion,
             this.clientCapabilities,
             this.clientInfo); // @formatter:on

       Mono<McpSchema.InitializeResult> result = this.mcpSession.sendRequest(McpSchema.METHOD_INITIALIZE,
             initializeRequest, new TypeReference<McpSchema.InitializeResult>() {
             });

       return result.flatMap(initializeResult -> {

          this.serverCapabilities = initializeResult.capabilities();
          this.serverInfo = initializeResult.serverInfo();

          logger.info("Server response with Protocol: {}, Capabilities: {}, Info: {} and Instructions {}",
                initializeResult.protocolVersion(), initializeResult.capabilities(), initializeResult.serverInfo(),
                initializeResult.instructions());

          if (!this.protocolVersions.contains(initializeResult.protocolVersion())) {
             return Mono.error(new McpError(
                   "Unsupported protocol version from the server: " + initializeResult.protocolVersion()));
          }

          return this.mcpSession.sendNotification(McpSchema.METHOD_NOTIFICATION_INITIALIZED, null).doOnSuccess(v -> {
             this.initialized.set(true);
             this.initializedSink.tryEmitValue(initializeResult);
          }).thenReturn(initializeResult);
       });
    }

    
    private <T> Mono<T> withInitializationCheck(String actionName,
          Function<McpSchema.InitializeResult, Mono<T>> operation) {
       return this.initializedSink.asMono()
          .timeout(this.initializationTimeout)
          .onErrorResume(TimeoutException.class,
                ex -> Mono.error(new McpError("Client must be initialized before " + actionName)))
          .flatMap(operation);
    }

    // --------------------------
    // Basic Utilites
    // --------------------------

    
    public Mono<Object> ping() {
       return this.withInitializationCheck("pinging the server", initializedResult -> this.mcpSession
          .sendRequest(McpSchema.METHOD_PING, null, new TypeReference<Object>() {
          }));
    }

    // --------------------------
    // Roots
    // --------------------------
    
    public Mono<Void> addRoot(Root root) {

       if (root == null) {
          return Mono.error(new McpError("Root must not be null"));
       }

       if (this.clientCapabilities.roots() == null) {
          return Mono.error(new McpError("Client must be configured with roots capabilities"));
       }

       if (this.roots.containsKey(root.uri())) {
          return Mono.error(new McpError("Root with uri '" + root.uri() + "' already exists"));
       }

       this.roots.put(root.uri(), root);

       logger.debug("Added root: {}", root);

       if (this.clientCapabilities.roots().listChanged()) {
          if (this.isInitialized()) {
             return this.rootsListChangedNotification();
          }
          else {
             logger.warn("Client is not initialized, ignore sending a roots list changed notification");
          }
       }
       return Mono.empty();
    }

    
    public Mono<Void> removeRoot(String rootUri) {

       if (rootUri == null) {
          return Mono.error(new McpError("Root uri must not be null"));
       }

       if (this.clientCapabilities.roots() == null) {
          return Mono.error(new McpError("Client must be configured with roots capabilities"));
       }

       Root removed = this.roots.remove(rootUri);

       if (removed != null) {
          logger.debug("Removed Root: {}", rootUri);
          if (this.clientCapabilities.roots().listChanged()) {
             if (this.isInitialized()) {
                return this.rootsListChangedNotification();
             }
             else {
                logger.warn("Client is not initialized, ignore sending a roots list changed notification");
             }

          }
          return Mono.empty();
       }
       return Mono.error(new McpError("Root with uri '" + rootUri + "' not found"));
    }

    
    public Mono<Void> rootsListChangedNotification() {
       return this.withInitializationCheck("sending roots list changed notification",
             initResult -> this.mcpSession.sendNotification(McpSchema.METHOD_NOTIFICATION_ROOTS_LIST_CHANGED));
    }

    private RequestHandler<McpSchema.ListRootsResult> rootsListRequestHandler() {
       return params -> {
          @SuppressWarnings("unused")
          McpSchema.PaginatedRequest request = transport.unmarshalFrom(params,
                new TypeReference<McpSchema.PaginatedRequest>() {
                });

          List<Root> roots = this.roots.values().stream().toList();

          return Mono.just(new McpSchema.ListRootsResult(roots));
       };
    }

    // --------------------------
    // Sampling
    // --------------------------
    private RequestHandler<CreateMessageResult> samplingCreateMessageHandler() {
       return params -> {
          McpSchema.CreateMessageRequest request = transport.unmarshalFrom(params,
                new TypeReference<McpSchema.CreateMessageRequest>() {
                });

          return this.samplingHandler.apply(request);
       };
    }

    // --------------------------
    // Tools
    // --------------------------
    private static final TypeReference<McpSchema.CallToolResult> CALL_TOOL_RESULT_TYPE_REF = new TypeReference<>() {
    };

    private static final TypeReference<McpSchema.ListToolsResult> LIST_TOOLS_RESULT_TYPE_REF = new TypeReference<>() {
    };

    
    public Mono<McpSchema.CallToolResult> callTool(McpSchema.CallToolRequest callToolRequest) {
       return this.withInitializationCheck("calling tools", initializedResult -> {
          if (this.serverCapabilities.tools() == null) {
             return Mono.error(new McpError("Server does not provide tools capability"));
          }
          return this.mcpSession.sendRequest(McpSchema.METHOD_TOOLS_CALL, callToolRequest, CALL_TOOL_RESULT_TYPE_REF);
       });
    }

    
    public Mono<McpSchema.ListToolsResult> listTools() {
       return this.listTools(null);
    }

    
    public Mono<McpSchema.ListToolsResult> listTools(String cursor) {
       return this.withInitializationCheck("listing tools", initializedResult -> {
          if (this.serverCapabilities.tools() == null) {
             return Mono.error(new McpError("Server does not provide tools capability"));
          }
          return this.mcpSession.sendRequest(McpSchema.METHOD_TOOLS_LIST, new McpSchema.PaginatedRequest(cursor),
                LIST_TOOLS_RESULT_TYPE_REF);
       });
    }

    private NotificationHandler asyncToolsChangeNotificationHandler(
          List<Function<List<McpSchema.Tool>, Mono<Void>>> toolsChangeConsumers) {
       // TODO: params are not used yet
       return params -> this.listTools()
          .flatMap(listToolsResult -> Flux.fromIterable(toolsChangeConsumers)
             .flatMap(consumer -> consumer.apply(listToolsResult.tools()))
             .onErrorResume(error -> {
                logger.error("Error handling tools list change notification", error);
                return Mono.empty();
             })
             .then());
    }

    // --------------------------
    // Resources
    // --------------------------

    private static final TypeReference<McpSchema.ListResourcesResult> LIST_RESOURCES_RESULT_TYPE_REF = new TypeReference<>() {
    };

    private static final TypeReference<McpSchema.ReadResourceResult> READ_RESOURCE_RESULT_TYPE_REF = new TypeReference<>() {
    };

    private static final TypeReference<McpSchema.ListResourceTemplatesResult> LIST_RESOURCE_TEMPLATES_RESULT_TYPE_REF = new TypeReference<>() {
    };

    
    public Mono<McpSchema.ListResourcesResult> listResources() {
       return this.listResources(null);
    }

    
    public Mono<McpSchema.ListResourcesResult> listResources(String cursor) {
       return this.withInitializationCheck("listing resources", initializedResult -> {
          if (this.serverCapabilities.resources() == null) {
             return Mono.error(new McpError("Server does not provide the resources capability"));
          }
          return this.mcpSession.sendRequest(McpSchema.METHOD_RESOURCES_LIST, new McpSchema.PaginatedRequest(cursor),
                LIST_RESOURCES_RESULT_TYPE_REF);
       });
    }

    
    public Mono<McpSchema.ReadResourceResult> readResource(McpSchema.Resource resource) {
       return this.readResource(new McpSchema.ReadResourceRequest(resource.uri()));
    }

    
    public Mono<McpSchema.ReadResourceResult> readResource(McpSchema.ReadResourceRequest readResourceRequest) {
       return this.withInitializationCheck("reading resources", initializedResult -> {
          if (this.serverCapabilities.resources() == null) {
             return Mono.error(new McpError("Server does not provide the resources capability"));
          }
          return this.mcpSession.sendRequest(McpSchema.METHOD_RESOURCES_READ, readResourceRequest,
                READ_RESOURCE_RESULT_TYPE_REF);
       });
    }

    
    public Mono<McpSchema.ListResourceTemplatesResult> listResourceTemplates() {
       return this.listResourceTemplates(null);
    }

    
    public Mono<McpSchema.ListResourceTemplatesResult> listResourceTemplates(String cursor) {
       return this.withInitializationCheck("listing resource templates", initializedResult -> {
          if (this.serverCapabilities.resources() == null) {
             return Mono.error(new McpError("Server does not provide the resources capability"));
          }
          return this.mcpSession.sendRequest(McpSchema.METHOD_RESOURCES_TEMPLATES_LIST,
                new McpSchema.PaginatedRequest(cursor), LIST_RESOURCE_TEMPLATES_RESULT_TYPE_REF);
       });
    }

    
    public Mono<Void> subscribeResource(McpSchema.SubscribeRequest subscribeRequest) {
       return this.withInitializationCheck("subscribing to resources", initializedResult -> this.mcpSession
          .sendRequest(McpSchema.METHOD_RESOURCES_SUBSCRIBE, subscribeRequest, VOID_TYPE_REFERENCE));
    }

    
    public Mono<Void> unsubscribeResource(McpSchema.UnsubscribeRequest unsubscribeRequest) {
       return this.withInitializationCheck("unsubscribing from resources", initializedResult -> this.mcpSession
          .sendRequest(McpSchema.METHOD_RESOURCES_UNSUBSCRIBE, unsubscribeRequest, VOID_TYPE_REFERENCE));
    }

    private NotificationHandler asyncResourcesChangeNotificationHandler(
          List<Function<List<McpSchema.Resource>, Mono<Void>>> resourcesChangeConsumers) {
       return params -> listResources().flatMap(listResourcesResult -> Flux.fromIterable(resourcesChangeConsumers)
          .flatMap(consumer -> consumer.apply(listResourcesResult.resources()))
          .onErrorResume(error -> {
             logger.error("Error handling resources list change notification", error);
             return Mono.empty();
          })
          .then());
    }

    // --------------------------
    // Prompts
    // --------------------------
    private static final TypeReference<McpSchema.ListPromptsResult> LIST_PROMPTS_RESULT_TYPE_REF = new TypeReference<>() {
    };

    private static final TypeReference<McpSchema.GetPromptResult> GET_PROMPT_RESULT_TYPE_REF = new TypeReference<>() {
    };

    
    public Mono<ListPromptsResult> listPrompts() {
       return this.listPrompts(null);
    }

    
    public Mono<ListPromptsResult> listPrompts(String cursor) {
       return this.withInitializationCheck("listing prompts", initializedResult -> this.mcpSession
          .sendRequest(McpSchema.METHOD_PROMPT_LIST, new PaginatedRequest(cursor), LIST_PROMPTS_RESULT_TYPE_REF));
    }

    
    public Mono<GetPromptResult> getPrompt(GetPromptRequest getPromptRequest) {
       return this.withInitializationCheck("getting prompts", initializedResult -> this.mcpSession
          .sendRequest(McpSchema.METHOD_PROMPT_GET, getPromptRequest, GET_PROMPT_RESULT_TYPE_REF));
    }

    private NotificationHandler asyncPromptsChangeNotificationHandler(
          List<Function<List<McpSchema.Prompt>, Mono<Void>>> promptsChangeConsumers) {
       return params -> listPrompts().flatMap(listPromptsResult -> Flux.fromIterable(promptsChangeConsumers)
          .flatMap(consumer -> consumer.apply(listPromptsResult.prompts()))
          .onErrorResume(error -> {
             logger.error("Error handling prompts list change notification", error);
             return Mono.empty();
          })
          .then());
    }

    // --------------------------
    // Logging
    // --------------------------
    private NotificationHandler asyncLoggingNotificationHandler(
          List<Function<LoggingMessageNotification, Mono<Void>>> loggingConsumers) {

       return params -> {
          McpSchema.LoggingMessageNotification loggingMessageNotification = transport.unmarshalFrom(params,
                new TypeReference<McpSchema.LoggingMessageNotification>() {
                });

          return Flux.fromIterable(loggingConsumers)
             .flatMap(consumer -> consumer.apply(loggingMessageNotification))
             .then();
       };
    }

    
    public Mono<Void> setLoggingLevel(LoggingLevel loggingLevel) {
       if (loggingLevel == null) {
          return Mono.error(new McpError("Logging level must not be null"));
       }

       return this.withInitializationCheck("setting logging level", initializedResult -> {
          String levelName = this.transport.unmarshalFrom(loggingLevel, new TypeReference<String>() {
          });
          Map<String, Object> params = Map.of("level", levelName);
          return this.mcpSession.sendNotification(McpSchema.METHOD_LOGGING_SET_LEVEL, params);
       });
    }

    
    void setProtocolVersions(List<String> protocolVersions) {
       this.protocolVersions = protocolVersions;
    }

}
```

#### McpSyncClient

同步客户端实现，封装了McpAsyncClient以提供阻塞操作，其余功能方法和McpAsyncClient保持一致

```Java
public class McpSyncClient implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(McpSyncClient.class);

    // TODO: Consider providing a client config to set this properly
    // this is currently a concern only because AutoCloseable is used - perhaps it
    // is not a requirement?
    private static final long DEFAULT_CLOSE_TIMEOUT_MS = 10_000L;

    private final McpAsyncClient delegate;

    
    McpSyncClient(McpAsyncClient delegate) {
       Assert.notNull(delegate, "The delegate can not be null");
       this.delegate = delegate;
    }

    
    public McpSchema.ServerCapabilities getServerCapabilities() {
       return this.delegate.getServerCapabilities();
    }

    
    public McpSchema.Implementation getServerInfo() {
       return this.delegate.getServerInfo();
    }

    
    public ClientCapabilities getClientCapabilities() {
       return this.delegate.getClientCapabilities();
    }

    
    public McpSchema.Implementation getClientInfo() {
       return this.delegate.getClientInfo();
    }

    @Override
    public void close() {
       this.delegate.close();
    }

    public boolean closeGracefully() {
       try {
          this.delegate.closeGracefully().block(Duration.ofMillis(DEFAULT_CLOSE_TIMEOUT_MS));
       }
       catch (RuntimeException e) {
          logger.warn("Client didn't close within timeout of {} ms.", DEFAULT_CLOSE_TIMEOUT_MS, e);
          return false;
       }
       return true;
    }

    
    public McpSchema.InitializeResult initialize() {
       // TODO: block takes no argument here as we assume the async client is
       // configured with a requestTimeout at all times
       return this.delegate.initialize().block();
    }

    
    public void rootsListChangedNotification() {
       this.delegate.rootsListChangedNotification().block();
    }

    
    public void addRoot(McpSchema.Root root) {
       this.delegate.addRoot(root).block();
    }

    
    public void removeRoot(String rootUri) {
       this.delegate.removeRoot(rootUri).block();
    }

    
    public Object ping() {
       return this.delegate.ping().block();
    }

    // --------------------------
    // Tools
    // --------------------------
    
    public McpSchema.CallToolResult callTool(McpSchema.CallToolRequest callToolRequest) {
       return this.delegate.callTool(callToolRequest).block();
    }

    
    public McpSchema.ListToolsResult listTools() {
       return this.delegate.listTools().block();
    }

    
    public McpSchema.ListToolsResult listTools(String cursor) {
       return this.delegate.listTools(cursor).block();
    }

    // --------------------------
    // Resources
    // --------------------------

    
    public McpSchema.ListResourcesResult listResources(String cursor) {
       return this.delegate.listResources(cursor).block();
    }

    
    public McpSchema.ListResourcesResult listResources() {
       return this.delegate.listResources().block();
    }

    
    public McpSchema.ReadResourceResult readResource(McpSchema.Resource resource) {
       return this.delegate.readResource(resource).block();
    }

    
    public McpSchema.ReadResourceResult readResource(McpSchema.ReadResourceRequest readResourceRequest) {
       return this.delegate.readResource(readResourceRequest).block();
    }

    
    public McpSchema.ListResourceTemplatesResult listResourceTemplates(String cursor) {
       return this.delegate.listResourceTemplates(cursor).block();
    }

    
    public McpSchema.ListResourceTemplatesResult listResourceTemplates() {
       return this.delegate.listResourceTemplates().block();
    }

    
    public void subscribeResource(McpSchema.SubscribeRequest subscribeRequest) {
       this.delegate.subscribeResource(subscribeRequest).block();
    }

    
    public void unsubscribeResource(McpSchema.UnsubscribeRequest unsubscribeRequest) {
       this.delegate.unsubscribeResource(unsubscribeRequest).block();
    }

    // --------------------------
    // Prompts
    // --------------------------
    public ListPromptsResult listPrompts(String cursor) {
       return this.delegate.listPrompts(cursor).block();
    }

    public ListPromptsResult listPrompts() {
       return this.delegate.listPrompts().block();
    }

    public GetPromptResult getPrompt(GetPromptRequest getPromptRequest) {
       return this.delegate.getPrompt(getPromptRequest).block();
    }

    
    public void setLoggingLevel(McpSchema.LoggingLevel loggingLevel) {
       this.delegate.setLoggingLevel(loggingLevel).block();
    }

}
```

### McpServer（服务端）

用于创建MCP服务端的工厂类，提供了同步、异步服务端的方法，提供如下核心功能

- 暴露工具：允许AI模型调用服务器提供的工具来执行特定操作
- 提供资源访问：为AI模型提供上下文数据，如文件、数据库等
- 管理提示模版：提供结构化的提示模版，用于与AI模型的交互
- 处理客户端连接和请求：管理客户端的连接，并处理其请求

内部类

- AsyncSpecification：配置异步MCP服务器的构建器类
- SyncSpecification：配置同步MCP服务器的构建器类

```Java
public interface McpServer {

    
    static SyncSpecification sync(McpServerTransportProvider transportProvider) {
       return new SyncSpecification(transportProvider);
    }

    
    static AsyncSpecification async(McpServerTransportProvider transportProvider) {
       return new AsyncSpecification(transportProvider);
    }

    
    class AsyncSpecification {

       private static final McpSchema.Implementation DEFAULT_SERVER_INFO = new McpSchema.Implementation("mcp-server",
             "1.0.0");

       private final McpServerTransportProvider transportProvider;

       private ObjectMapper objectMapper;

       private McpSchema.Implementation serverInfo = DEFAULT_SERVER_INFO;

       private McpSchema.ServerCapabilities serverCapabilities;

       
       private final List<McpServerFeatures.AsyncToolSpecification> tools = new ArrayList<>();

       
       private final Map<String, McpServerFeatures.AsyncResourceSpecification> resources = new HashMap<>();

       private final List<ResourceTemplate> resourceTemplates = new ArrayList<>();

       
       private final Map<String, McpServerFeatures.AsyncPromptSpecification> prompts = new HashMap<>();

       private final List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootsChangeHandlers = new ArrayList<>();

       private AsyncSpecification(McpServerTransportProvider transportProvider) {
          Assert.notNull(transportProvider, "Transport provider must not be null");
          this.transportProvider = transportProvider;
       }

       
       public AsyncSpecification serverInfo(McpSchema.Implementation serverInfo) {
          Assert.notNull(serverInfo, "Server info must not be null");
          this.serverInfo = serverInfo;
          return this;
       }

       
       public AsyncSpecification serverInfo(String name, String version) {
          Assert.hasText(name, "Name must not be null or empty");
          Assert.hasText(version, "Version must not be null or empty");
          this.serverInfo = new McpSchema.Implementation(name, version);
          return this;
       }

       
       public AsyncSpecification capabilities(McpSchema.ServerCapabilities serverCapabilities) {
          Assert.notNull(serverCapabilities, "Server capabilities must not be null");
          this.serverCapabilities = serverCapabilities;
          return this;
       }

       
       public AsyncSpecification tool(McpSchema.Tool tool,
             BiFunction<McpAsyncServerExchange, Map<String, Object>, Mono<CallToolResult>> handler) {
          Assert.notNull(tool, "Tool must not be null");
          Assert.notNull(handler, "Handler must not be null");

          this.tools.add(new McpServerFeatures.AsyncToolSpecification(tool, handler));

          return this;
       }

       
       public AsyncSpecification tools(List<McpServerFeatures.AsyncToolSpecification> toolSpecifications) {
          Assert.notNull(toolSpecifications, "Tool handlers list must not be null");
          this.tools.addAll(toolSpecifications);
          return this;
       }

       
       public AsyncSpecification tools(McpServerFeatures.AsyncToolSpecification... toolSpecifications) {
          Assert.notNull(toolSpecifications, "Tool handlers list must not be null");
          for (McpServerFeatures.AsyncToolSpecification tool : toolSpecifications) {
             this.tools.add(tool);
          }
          return this;
       }

       
       public AsyncSpecification resources(
             Map<String, McpServerFeatures.AsyncResourceSpecification> resourceSpecifications) {
          Assert.notNull(resourceSpecifications, "Resource handlers map must not be null");
          this.resources.putAll(resourceSpecifications);
          return this;
       }

       
       public AsyncSpecification resources(List<McpServerFeatures.AsyncResourceSpecification> resourceSpecifications) {
          Assert.notNull(resourceSpecifications, "Resource handlers list must not be null");
          for (McpServerFeatures.AsyncResourceSpecification resource : resourceSpecifications) {
             this.resources.put(resource.resource().uri(), resource);
          }
          return this;
       }

       
       public AsyncSpecification resources(McpServerFeatures.AsyncResourceSpecification... resourceSpecifications) {
          Assert.notNull(resourceSpecifications, "Resource handlers list must not be null");
          for (McpServerFeatures.AsyncResourceSpecification resource : resourceSpecifications) {
             this.resources.put(resource.resource().uri(), resource);
          }
          return this;
       }

       
       public AsyncSpecification resourceTemplates(List<ResourceTemplate> resourceTemplates) {
          Assert.notNull(resourceTemplates, "Resource templates must not be null");
          this.resourceTemplates.addAll(resourceTemplates);
          return this;
       }

       
       public AsyncSpecification resourceTemplates(ResourceTemplate... resourceTemplates) {
          Assert.notNull(resourceTemplates, "Resource templates must not be null");
          for (ResourceTemplate resourceTemplate : resourceTemplates) {
             this.resourceTemplates.add(resourceTemplate);
          }
          return this;
       }

       
       public AsyncSpecification prompts(Map<String, McpServerFeatures.AsyncPromptSpecification> prompts) {
          Assert.notNull(prompts, "Prompts map must not be null");
          this.prompts.putAll(prompts);
          return this;
       }

       
       public AsyncSpecification prompts(List<McpServerFeatures.AsyncPromptSpecification> prompts) {
          Assert.notNull(prompts, "Prompts list must not be null");
          for (McpServerFeatures.AsyncPromptSpecification prompt : prompts) {
             this.prompts.put(prompt.prompt().name(), prompt);
          }
          return this;
       }

       
       public AsyncSpecification prompts(McpServerFeatures.AsyncPromptSpecification... prompts) {
          Assert.notNull(prompts, "Prompts list must not be null");
          for (McpServerFeatures.AsyncPromptSpecification prompt : prompts) {
             this.prompts.put(prompt.prompt().name(), prompt);
          }
          return this;
       }

       
       public AsyncSpecification rootsChangeHandler(
             BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>> handler) {
          Assert.notNull(handler, "Consumer must not be null");
          this.rootsChangeHandlers.add(handler);
          return this;
       }

       
       public AsyncSpecification rootsChangeHandlers(
             List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> handlers) {
          Assert.notNull(handlers, "Handlers list must not be null");
          this.rootsChangeHandlers.addAll(handlers);
          return this;
       }

       
       public AsyncSpecification rootsChangeHandlers(
             @SuppressWarnings("unchecked") BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>... handlers) {
          Assert.notNull(handlers, "Handlers list must not be null");
          return this.rootsChangeHandlers(Arrays.asList(handlers));
       }

       
       public AsyncSpecification objectMapper(ObjectMapper objectMapper) {
          Assert.notNull(objectMapper, "ObjectMapper must not be null");
          this.objectMapper = objectMapper;
          return this;
       }

       
       public McpAsyncServer build() {
          var features = new McpServerFeatures.Async(this.serverInfo, this.serverCapabilities, this.tools,
                this.resources, this.resourceTemplates, this.prompts, this.rootsChangeHandlers);
          var mapper = this.objectMapper != null ? this.objectMapper : new ObjectMapper();
          return new McpAsyncServer(this.transportProvider, mapper, features);
       }

    }

    
    class SyncSpecification {

       private static final McpSchema.Implementation DEFAULT_SERVER_INFO = new McpSchema.Implementation("mcp-server",
             "1.0.0");

       private final McpServerTransportProvider transportProvider;

       private ObjectMapper objectMapper;

       private McpSchema.Implementation serverInfo = DEFAULT_SERVER_INFO;

       private McpSchema.ServerCapabilities serverCapabilities;

       
       private final List<McpServerFeatures.SyncToolSpecification> tools = new ArrayList<>();

       
       private final Map<String, McpServerFeatures.SyncResourceSpecification> resources = new HashMap<>();

       private final List<ResourceTemplate> resourceTemplates = new ArrayList<>();

       
       private final Map<String, McpServerFeatures.SyncPromptSpecification> prompts = new HashMap<>();

       private final List<BiConsumer<McpSyncServerExchange, List<McpSchema.Root>>> rootsChangeHandlers = new ArrayList<>();

       private SyncSpecification(McpServerTransportProvider transportProvider) {
          Assert.notNull(transportProvider, "Transport provider must not be null");
          this.transportProvider = transportProvider;
       }

       
       public SyncSpecification serverInfo(McpSchema.Implementation serverInfo) {
          Assert.notNull(serverInfo, "Server info must not be null");
          this.serverInfo = serverInfo;
          return this;
       }

       
       public SyncSpecification serverInfo(String name, String version) {
          Assert.hasText(name, "Name must not be null or empty");
          Assert.hasText(version, "Version must not be null or empty");
          this.serverInfo = new McpSchema.Implementation(name, version);
          return this;
       }

       
       public SyncSpecification capabilities(McpSchema.ServerCapabilities serverCapabilities) {
          Assert.notNull(serverCapabilities, "Server capabilities must not be null");
          this.serverCapabilities = serverCapabilities;
          return this;
       }

       
       public SyncSpecification tool(McpSchema.Tool tool,
             BiFunction<McpSyncServerExchange, Map<String, Object>, McpSchema.CallToolResult> handler) {
          Assert.notNull(tool, "Tool must not be null");
          Assert.notNull(handler, "Handler must not be null");

          this.tools.add(new McpServerFeatures.SyncToolSpecification(tool, handler));

          return this;
       }

       
       public SyncSpecification tools(List<McpServerFeatures.SyncToolSpecification> toolSpecifications) {
          Assert.notNull(toolSpecifications, "Tool handlers list must not be null");
          this.tools.addAll(toolSpecifications);
          return this;
       }

       
       public SyncSpecification tools(McpServerFeatures.SyncToolSpecification... toolSpecifications) {
          Assert.notNull(toolSpecifications, "Tool handlers list must not be null");
          for (McpServerFeatures.SyncToolSpecification tool : toolSpecifications) {
             this.tools.add(tool);
          }
          return this;
       }

       
       public SyncSpecification resources(
             Map<String, McpServerFeatures.SyncResourceSpecification> resourceSpecifications) {
          Assert.notNull(resourceSpecifications, "Resource handlers map must not be null");
          this.resources.putAll(resourceSpecifications);
          return this;
       }

       
       public SyncSpecification resources(List<McpServerFeatures.SyncResourceSpecification> resourceSpecifications) {
          Assert.notNull(resourceSpecifications, "Resource handlers list must not be null");
          for (McpServerFeatures.SyncResourceSpecification resource : resourceSpecifications) {
             this.resources.put(resource.resource().uri(), resource);
          }
          return this;
       }

       
       public SyncSpecification resources(McpServerFeatures.SyncResourceSpecification... resourceSpecifications) {
          Assert.notNull(resourceSpecifications, "Resource handlers list must not be null");
          for (McpServerFeatures.SyncResourceSpecification resource : resourceSpecifications) {
             this.resources.put(resource.resource().uri(), resource);
          }
          return this;
       }

       
       public SyncSpecification resourceTemplates(List<ResourceTemplate> resourceTemplates) {
          Assert.notNull(resourceTemplates, "Resource templates must not be null");
          this.resourceTemplates.addAll(resourceTemplates);
          return this;
       }

       
       public SyncSpecification resourceTemplates(ResourceTemplate... resourceTemplates) {
          Assert.notNull(resourceTemplates, "Resource templates must not be null");
          for (ResourceTemplate resourceTemplate : resourceTemplates) {
             this.resourceTemplates.add(resourceTemplate);
          }
          return this;
       }

       
       public SyncSpecification prompts(Map<String, McpServerFeatures.SyncPromptSpecification> prompts) {
          Assert.notNull(prompts, "Prompts map must not be null");
          this.prompts.putAll(prompts);
          return this;
       }

       
       public SyncSpecification prompts(List<McpServerFeatures.SyncPromptSpecification> prompts) {
          Assert.notNull(prompts, "Prompts list must not be null");
          for (McpServerFeatures.SyncPromptSpecification prompt : prompts) {
             this.prompts.put(prompt.prompt().name(), prompt);
          }
          return this;
       }

       
       public SyncSpecification prompts(McpServerFeatures.SyncPromptSpecification... prompts) {
          Assert.notNull(prompts, "Prompts list must not be null");
          for (McpServerFeatures.SyncPromptSpecification prompt : prompts) {
             this.prompts.put(prompt.prompt().name(), prompt);
          }
          return this;
       }

       
       public SyncSpecification rootsChangeHandler(BiConsumer<McpSyncServerExchange, List<McpSchema.Root>> handler) {
          Assert.notNull(handler, "Consumer must not be null");
          this.rootsChangeHandlers.add(handler);
          return this;
       }

       
       public SyncSpecification rootsChangeHandlers(
             List<BiConsumer<McpSyncServerExchange, List<McpSchema.Root>>> handlers) {
          Assert.notNull(handlers, "Handlers list must not be null");
          this.rootsChangeHandlers.addAll(handlers);
          return this;
       }

       
       public SyncSpecification rootsChangeHandlers(
             BiConsumer<McpSyncServerExchange, List<McpSchema.Root>>... handlers) {
          Assert.notNull(handlers, "Handlers list must not be null");
          return this.rootsChangeHandlers(List.of(handlers));
       }

       
       public SyncSpecification objectMapper(ObjectMapper objectMapper) {
          Assert.notNull(objectMapper, "ObjectMapper must not be null");
          this.objectMapper = objectMapper;
          return this;
       }

       
       public McpSyncServer build() {
          McpServerFeatures.Sync syncFeatures = new McpServerFeatures.Sync(this.serverInfo, this.serverCapabilities,
                this.tools, this.resources, this.resourceTemplates, this.prompts, this.rootsChangeHandlers);
          McpServerFeatures.Async asyncFeatures = McpServerFeatures.Async.fromSync(syncFeatures);
          var mapper = this.objectMapper != null ? this.objectMapper : new ObjectMapper();
          var asyncServer = new McpAsyncServer(this.transportProvider, mapper, asyncFeatures);

          return new McpSyncServer(asyncServer);
       }

    }

}
```

#### McpAsyncServer

异步服务端的实现，基于Project Reactor的Mono与Flux类型，支持非阻塞操作

关闭

- close：立即关闭服务端连接
- closeGracefully：优雅关闭服务端连接

工具操作

- addTool：动态添加工具
- removeTool：动态移除指定名称的工具
- notifyToolsListChanged：通知客户端工具列表已发生变化

服务器信息获取

- getServerCapabilities：获取服务器的能力配置
- getServerInfo：获取服务器的实现信息

资源管理

- addResource：动态添加资源
- removeResource：动态移除指定URL的资源
- notifyResourcesListChanged：通知客户端资源列表已发生变化

提示管理

- addPrompt：动态添加提示
- removePrompt：动态移除指定名称的提示
- notifyPromptsListChanged：通知客户端提示列表已发生变化

日志管理

- loggingNotification：向所有连接的客户端发送日志通知

```Java
public class McpAsyncServer {

    private static final Logger logger = LoggerFactory.getLogger(McpAsyncServer.class);

    private final McpAsyncServer delegate;

    McpAsyncServer() {
       this.delegate = null;
    }

    
    McpAsyncServer(McpServerTransportProvider mcpTransportProvider, ObjectMapper objectMapper,
          McpServerFeatures.Async features) {
       this.delegate = new AsyncServerImpl(mcpTransportProvider, objectMapper, features);
    }

    
    public McpSchema.ServerCapabilities getServerCapabilities() {
       return this.delegate.getServerCapabilities();
    }

    
    public McpSchema.Implementation getServerInfo() {
       return this.delegate.getServerInfo();
    }

    
    public Mono<Void> closeGracefully() {
       return this.delegate.closeGracefully();
    }

    
    public void close() {
       this.delegate.close();
    }

    // ---------------------------------------
    // Tool Management
    // ---------------------------------------
    
    public Mono<Void> addTool(McpServerFeatures.AsyncToolSpecification toolSpecification) {
       return this.delegate.addTool(toolSpecification);
    }

    
    public Mono<Void> removeTool(String toolName) {
       return this.delegate.removeTool(toolName);
    }

    
    public Mono<Void> notifyToolsListChanged() {
       return this.delegate.notifyToolsListChanged();
    }

    // ---------------------------------------
    // Resource Management
    // ---------------------------------------
    
    public Mono<Void> addResource(McpServerFeatures.AsyncResourceSpecification resourceHandler) {
       return this.delegate.addResource(resourceHandler);
    }

    
    public Mono<Void> removeResource(String resourceUri) {
       return this.delegate.removeResource(resourceUri);
    }

    
    public Mono<Void> notifyResourcesListChanged() {
       return this.delegate.notifyResourcesListChanged();
    }

    // ---------------------------------------
    // Prompt Management
    // ---------------------------------------
    
    public Mono<Void> addPrompt(McpServerFeatures.AsyncPromptSpecification promptSpecification) {
       return this.delegate.addPrompt(promptSpecification);
    }

    
    public Mono<Void> removePrompt(String promptName) {
       return this.delegate.removePrompt(promptName);
    }

    
    public Mono<Void> notifyPromptsListChanged() {
       return this.delegate.notifyPromptsListChanged();
    }

    // ---------------------------------------
    // Logging Management
    // ---------------------------------------

    
    public Mono<Void> loggingNotification(LoggingMessageNotification loggingMessageNotification) {
       return this.delegate.loggingNotification(loggingMessageNotification);
    }

    // ---------------------------------------
    // Sampling
    // ---------------------------------------
    
    void setProtocolVersions(List<String> protocolVersions) {
       this.delegate.setProtocolVersions(protocolVersions);
    }

    private static class AsyncServerImpl extends McpAsyncServer {

       private final McpServerTransportProvider mcpTransportProvider;

       private final ObjectMapper objectMapper;

       private final McpSchema.ServerCapabilities serverCapabilities;

       private final McpSchema.Implementation serverInfo;

       private final CopyOnWriteArrayList<McpServerFeatures.AsyncToolSpecification> tools = new CopyOnWriteArrayList<>();

       private final CopyOnWriteArrayList<McpSchema.ResourceTemplate> resourceTemplates = new CopyOnWriteArrayList<>();

       private final ConcurrentHashMap<String, McpServerFeatures.AsyncResourceSpecification> resources = new ConcurrentHashMap<>();

       private final ConcurrentHashMap<String, McpServerFeatures.AsyncPromptSpecification> prompts = new ConcurrentHashMap<>();

       private LoggingLevel minLoggingLevel = LoggingLevel.DEBUG;

       private List<String> protocolVersions = List.of(McpSchema.LATEST_PROTOCOL_VERSION);

       AsyncServerImpl(McpServerTransportProvider mcpTransportProvider, ObjectMapper objectMapper,
             McpServerFeatures.Async features) {
          this.mcpTransportProvider = mcpTransportProvider;
          this.objectMapper = objectMapper;
          this.serverInfo = features.serverInfo();
          this.serverCapabilities = features.serverCapabilities();
          this.tools.addAll(features.tools());
          this.resources.putAll(features.resources());
          this.resourceTemplates.addAll(features.resourceTemplates());
          this.prompts.putAll(features.prompts());

          Map<String, McpServerSession.RequestHandler<?>> requestHandlers = new HashMap<>();

          // Initialize request handlers for standard MCP methods

          // Ping MUST respond with an empty data, but not NULL response.
          requestHandlers.put(McpSchema.METHOD_PING, (exchange, params) -> Mono.just(Map.of()));

          // Add tools API handlers if the tool capability is enabled
          if (this.serverCapabilities.tools() != null) {
             requestHandlers.put(McpSchema.METHOD_TOOLS_LIST, toolsListRequestHandler());
             requestHandlers.put(McpSchema.METHOD_TOOLS_CALL, toolsCallRequestHandler());
          }

          // Add resources API handlers if provided
          if (this.serverCapabilities.resources() != null) {
             requestHandlers.put(McpSchema.METHOD_RESOURCES_LIST, resourcesListRequestHandler());
             requestHandlers.put(McpSchema.METHOD_RESOURCES_READ, resourcesReadRequestHandler());
             requestHandlers.put(McpSchema.METHOD_RESOURCES_TEMPLATES_LIST, resourceTemplateListRequestHandler());
          }

          // Add prompts API handlers if provider exists
          if (this.serverCapabilities.prompts() != null) {
             requestHandlers.put(McpSchema.METHOD_PROMPT_LIST, promptsListRequestHandler());
             requestHandlers.put(McpSchema.METHOD_PROMPT_GET, promptsGetRequestHandler());
          }

          // Add logging API handlers if the logging capability is enabled
          if (this.serverCapabilities.logging() != null) {
             requestHandlers.put(McpSchema.METHOD_LOGGING_SET_LEVEL, setLoggerRequestHandler());
          }

          Map<String, McpServerSession.NotificationHandler> notificationHandlers = new HashMap<>();

          notificationHandlers.put(McpSchema.METHOD_NOTIFICATION_INITIALIZED, (exchange, params) -> Mono.empty());

          List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootsChangeConsumers = features
             .rootsChangeConsumers();

          if (Utils.isEmpty(rootsChangeConsumers)) {
             rootsChangeConsumers = List.of((exchange,
                   roots) -> Mono.fromRunnable(() -> logger.warn(
                         "Roots list changed notification, but no consumers provided. Roots list changed: {}",
                         roots)));
          }

          notificationHandlers.put(McpSchema.METHOD_NOTIFICATION_ROOTS_LIST_CHANGED,
                asyncRootsListChangedNotificationHandler(rootsChangeConsumers));

          mcpTransportProvider
             .setSessionFactory(transport -> new McpServerSession(UUID.randomUUID().toString(), transport,
                   this::asyncInitializeRequestHandler, Mono::empty, requestHandlers, notificationHandlers));
       }

       // ---------------------------------------
       // Lifecycle Management
       // ---------------------------------------
       private Mono<McpSchema.InitializeResult> asyncInitializeRequestHandler(
             McpSchema.InitializeRequest initializeRequest) {
          return Mono.defer(() -> {
             logger.info("Client initialize request - Protocol: {}, Capabilities: {}, Info: {}",
                   initializeRequest.protocolVersion(), initializeRequest.capabilities(),
                   initializeRequest.clientInfo());

             // The server MUST respond with the highest protocol version it supports
             // if
             // it does not support the requested (e.g. Client) version.
             String serverProtocolVersion = this.protocolVersions.get(this.protocolVersions.size() - 1);

             if (this.protocolVersions.contains(initializeRequest.protocolVersion())) {
                // If the server supports the requested protocol version, it MUST
                // respond
                // with the same version.
                serverProtocolVersion = initializeRequest.protocolVersion();
             }
             else {
                logger.warn(
                      "Client requested unsupported protocol version: {}, so the server will sugggest the {} version instead",
                      initializeRequest.protocolVersion(), serverProtocolVersion);
             }

             return Mono.just(new McpSchema.InitializeResult(serverProtocolVersion, this.serverCapabilities,
                   this.serverInfo, null));
          });
       }

       public McpSchema.ServerCapabilities getServerCapabilities() {
          return this.serverCapabilities;
       }

       public McpSchema.Implementation getServerInfo() {
          return this.serverInfo;
       }

       @Override
       public Mono<Void> closeGracefully() {
          return this.mcpTransportProvider.closeGracefully();
       }

       @Override
       public void close() {
          this.mcpTransportProvider.close();
       }

       private McpServerSession.NotificationHandler asyncRootsListChangedNotificationHandler(
             List<BiFunction<McpAsyncServerExchange, List<McpSchema.Root>, Mono<Void>>> rootsChangeConsumers) {
          return (exchange, params) -> exchange.listRoots()
             .flatMap(listRootsResult -> Flux.fromIterable(rootsChangeConsumers)
                .flatMap(consumer -> consumer.apply(exchange, listRootsResult.roots()))
                .onErrorResume(error -> {
                   logger.error("Error handling roots list change notification", error);
                   return Mono.empty();
                })
                .then());
       }

       // ---------------------------------------
       // Tool Management
       // ---------------------------------------

       @Override
       public Mono<Void> addTool(McpServerFeatures.AsyncToolSpecification toolSpecification) {
          if (toolSpecification == null) {
             return Mono.error(new McpError("Tool specification must not be null"));
          }
          if (toolSpecification.tool() == null) {
             return Mono.error(new McpError("Tool must not be null"));
          }
          if (toolSpecification.call() == null) {
             return Mono.error(new McpError("Tool call handler must not be null"));
          }
          if (this.serverCapabilities.tools() == null) {
             return Mono.error(new McpError("Server must be configured with tool capabilities"));
          }

          return Mono.defer(() -> {
             // Check for duplicate tool names
             if (this.tools.stream().anyMatch(th -> th.tool().name().equals(toolSpecification.tool().name()))) {
                return Mono
                   .error(new McpError("Tool with name '" + toolSpecification.tool().name() + "' already exists"));
             }

             this.tools.add(toolSpecification);
             logger.debug("Added tool handler: {}", toolSpecification.tool().name());

             if (this.serverCapabilities.tools().listChanged()) {
                return notifyToolsListChanged();
             }
             return Mono.empty();
          });
       }

       @Override
       public Mono<Void> removeTool(String toolName) {
          if (toolName == null) {
             return Mono.error(new McpError("Tool name must not be null"));
          }
          if (this.serverCapabilities.tools() == null) {
             return Mono.error(new McpError("Server must be configured with tool capabilities"));
          }

          return Mono.defer(() -> {
             boolean removed = this.tools
                .removeIf(toolSpecification -> toolSpecification.tool().name().equals(toolName));
             if (removed) {
                logger.debug("Removed tool handler: {}", toolName);
                if (this.serverCapabilities.tools().listChanged()) {
                   return notifyToolsListChanged();
                }
                return Mono.empty();
             }
             return Mono.error(new McpError("Tool with name '" + toolName + "' not found"));
          });
       }

       @Override
       public Mono<Void> notifyToolsListChanged() {
          return this.mcpTransportProvider.notifyClients(McpSchema.METHOD_NOTIFICATION_TOOLS_LIST_CHANGED, null);
       }

       private McpServerSession.RequestHandler<McpSchema.ListToolsResult> toolsListRequestHandler() {
          return (exchange, params) -> {
             List<Tool> tools = this.tools.stream().map(McpServerFeatures.AsyncToolSpecification::tool).toList();

             return Mono.just(new McpSchema.ListToolsResult(tools, null));
          };
       }

       private McpServerSession.RequestHandler<CallToolResult> toolsCallRequestHandler() {
          return (exchange, params) -> {
             McpSchema.CallToolRequest callToolRequest = objectMapper.convertValue(params,
                   new TypeReference<McpSchema.CallToolRequest>() {
                   });

             Optional<McpServerFeatures.AsyncToolSpecification> toolSpecification = this.tools.stream()
                .filter(tr -> callToolRequest.name().equals(tr.tool().name()))
                .findAny();

             if (toolSpecification.isEmpty()) {
                return Mono.error(new McpError("Tool not found: " + callToolRequest.name()));
             }

             return toolSpecification.map(tool -> tool.call().apply(exchange, callToolRequest.arguments()))
                .orElse(Mono.error(new McpError("Tool not found: " + callToolRequest.name())));
          };
       }

       // ---------------------------------------
       // Resource Management
       // ---------------------------------------

       @Override
       public Mono<Void> addResource(McpServerFeatures.AsyncResourceSpecification resourceSpecification) {
          if (resourceSpecification == null || resourceSpecification.resource() == null) {
             return Mono.error(new McpError("Resource must not be null"));
          }

          if (this.serverCapabilities.resources() == null) {
             return Mono.error(new McpError("Server must be configured with resource capabilities"));
          }

          return Mono.defer(() -> {
             if (this.resources.putIfAbsent(resourceSpecification.resource().uri(), resourceSpecification) != null) {
                return Mono.error(new McpError(
                      "Resource with URI '" + resourceSpecification.resource().uri() + "' already exists"));
             }
             logger.debug("Added resource handler: {}", resourceSpecification.resource().uri());
             if (this.serverCapabilities.resources().listChanged()) {
                return notifyResourcesListChanged();
             }
             return Mono.empty();
          });
       }

       @Override
       public Mono<Void> removeResource(String resourceUri) {
          if (resourceUri == null) {
             return Mono.error(new McpError("Resource URI must not be null"));
          }
          if (this.serverCapabilities.resources() == null) {
             return Mono.error(new McpError("Server must be configured with resource capabilities"));
          }

          return Mono.defer(() -> {
             McpServerFeatures.AsyncResourceSpecification removed = this.resources.remove(resourceUri);
             if (removed != null) {
                logger.debug("Removed resource handler: {}", resourceUri);
                if (this.serverCapabilities.resources().listChanged()) {
                   return notifyResourcesListChanged();
                }
                return Mono.empty();
             }
             return Mono.error(new McpError("Resource with URI '" + resourceUri + "' not found"));
          });
       }

       @Override
       public Mono<Void> notifyResourcesListChanged() {
          return this.mcpTransportProvider.notifyClients(McpSchema.METHOD_NOTIFICATION_RESOURCES_LIST_CHANGED, null);
       }

       private McpServerSession.RequestHandler<McpSchema.ListResourcesResult> resourcesListRequestHandler() {
          return (exchange, params) -> {
             var resourceList = this.resources.values()
                .stream()
                .map(McpServerFeatures.AsyncResourceSpecification::resource)
                .toList();
             return Mono.just(new McpSchema.ListResourcesResult(resourceList, null));
          };
       }

       private McpServerSession.RequestHandler<McpSchema.ListResourceTemplatesResult> resourceTemplateListRequestHandler() {
          return (exchange, params) -> Mono
             .just(new McpSchema.ListResourceTemplatesResult(this.resourceTemplates, null));

       }

       private McpServerSession.RequestHandler<McpSchema.ReadResourceResult> resourcesReadRequestHandler() {
          return (exchange, params) -> {
             McpSchema.ReadResourceRequest resourceRequest = objectMapper.convertValue(params,
                   new TypeReference<McpSchema.ReadResourceRequest>() {
                   });
             var resourceUri = resourceRequest.uri();
             McpServerFeatures.AsyncResourceSpecification specification = this.resources.get(resourceUri);
             if (specification != null) {
                return specification.readHandler().apply(exchange, resourceRequest);
             }
             return Mono.error(new McpError("Resource not found: " + resourceUri));
          };
       }

       // ---------------------------------------
       // Prompt Management
       // ---------------------------------------

       @Override
       public Mono<Void> addPrompt(McpServerFeatures.AsyncPromptSpecification promptSpecification) {
          if (promptSpecification == null) {
             return Mono.error(new McpError("Prompt specification must not be null"));
          }
          if (this.serverCapabilities.prompts() == null) {
             return Mono.error(new McpError("Server must be configured with prompt capabilities"));
          }

          return Mono.defer(() -> {
             McpServerFeatures.AsyncPromptSpecification specification = this.prompts
                .putIfAbsent(promptSpecification.prompt().name(), promptSpecification);
             if (specification != null) {
                return Mono.error(new McpError(
                      "Prompt with name '" + promptSpecification.prompt().name() + "' already exists"));
             }

             logger.debug("Added prompt handler: {}", promptSpecification.prompt().name());

             // Servers that declared the listChanged capability SHOULD send a
             // notification,
             // when the list of available prompts changes
             if (this.serverCapabilities.prompts().listChanged()) {
                return notifyPromptsListChanged();
             }
             return Mono.empty();
          });
       }

       @Override
       public Mono<Void> removePrompt(String promptName) {
          if (promptName == null) {
             return Mono.error(new McpError("Prompt name must not be null"));
          }
          if (this.serverCapabilities.prompts() == null) {
             return Mono.error(new McpError("Server must be configured with prompt capabilities"));
          }

          return Mono.defer(() -> {
             McpServerFeatures.AsyncPromptSpecification removed = this.prompts.remove(promptName);

             if (removed != null) {
                logger.debug("Removed prompt handler: {}", promptName);
                // Servers that declared the listChanged capability SHOULD send a
                // notification, when the list of available prompts changes
                if (this.serverCapabilities.prompts().listChanged()) {
                   return this.notifyPromptsListChanged();
                }
                return Mono.empty();
             }
             return Mono.error(new McpError("Prompt with name '" + promptName + "' not found"));
          });
       }

       @Override
       public Mono<Void> notifyPromptsListChanged() {
          return this.mcpTransportProvider.notifyClients(McpSchema.METHOD_NOTIFICATION_PROMPTS_LIST_CHANGED, null);
       }

       private McpServerSession.RequestHandler<McpSchema.ListPromptsResult> promptsListRequestHandler() {
          return (exchange, params) -> {
             // TODO: Implement pagination
             // McpSchema.PaginatedRequest request = objectMapper.convertValue(params,
             // new TypeReference<McpSchema.PaginatedRequest>() {
             // });

             var promptList = this.prompts.values()
                .stream()
                .map(McpServerFeatures.AsyncPromptSpecification::prompt)
                .toList();

             return Mono.just(new McpSchema.ListPromptsResult(promptList, null));
          };
       }

       private McpServerSession.RequestHandler<McpSchema.GetPromptResult> promptsGetRequestHandler() {
          return (exchange, params) -> {
             McpSchema.GetPromptRequest promptRequest = objectMapper.convertValue(params,
                   new TypeReference<McpSchema.GetPromptRequest>() {
                   });

             // Implement prompt retrieval logic here
             McpServerFeatures.AsyncPromptSpecification specification = this.prompts.get(promptRequest.name());
             if (specification == null) {
                return Mono.error(new McpError("Prompt not found: " + promptRequest.name()));
             }

             return specification.promptHandler().apply(exchange, promptRequest);
          };
       }

       // ---------------------------------------
       // Logging Management
       // ---------------------------------------

       @Override
       public Mono<Void> loggingNotification(LoggingMessageNotification loggingMessageNotification) {

          if (loggingMessageNotification == null) {
             return Mono.error(new McpError("Logging message must not be null"));
          }

          Map<String, Object> params = this.objectMapper.convertValue(loggingMessageNotification,
                new TypeReference<Map<String, Object>>() {
                });

          if (loggingMessageNotification.level().level() < minLoggingLevel.level()) {
             return Mono.empty();
          }

          return this.mcpTransportProvider.notifyClients(McpSchema.METHOD_NOTIFICATION_MESSAGE, params);
       }

       private McpServerSession.RequestHandler<Void> setLoggerRequestHandler() {
          return (exchange, params) -> {
             this.minLoggingLevel = objectMapper.convertValue(params, new TypeReference<LoggingLevel>() {
             });

             return Mono.empty();
          };
       }

       // ---------------------------------------
       // Sampling
       // ---------------------------------------

       @Override
       void setProtocolVersions(List<String> protocolVersions) {
          this.protocolVersions = protocolVersions;
       }

    }

}
```

#### McpSyncServer

同步服务端的实现，封装了McpSyncServer以提供阻塞操作，其余功能方法和McpAsyncServer保持一致

```Java
public class McpSyncServer {

    
    private final McpAsyncServer asyncServer;

    
    public McpSyncServer(McpAsyncServer asyncServer) {
       Assert.notNull(asyncServer, "Async server must not be null");
       this.asyncServer = asyncServer;
    }

    
    public void addTool(McpServerFeatures.SyncToolSpecification toolHandler) {
       this.asyncServer.addTool(McpServerFeatures.AsyncToolSpecification.fromSync(toolHandler)).block();
    }

    
    public void removeTool(String toolName) {
       this.asyncServer.removeTool(toolName).block();
    }

    
    public void addResource(McpServerFeatures.SyncResourceSpecification resourceHandler) {
       this.asyncServer.addResource(McpServerFeatures.AsyncResourceSpecification.fromSync(resourceHandler)).block();
    }

    
    public void removeResource(String resourceUri) {
       this.asyncServer.removeResource(resourceUri).block();
    }

    
    public void addPrompt(McpServerFeatures.SyncPromptSpecification promptSpecification) {
       this.asyncServer.addPrompt(McpServerFeatures.AsyncPromptSpecification.fromSync(promptSpecification)).block();
    }

    
    public void removePrompt(String promptName) {
       this.asyncServer.removePrompt(promptName).block();
    }

    
    public void notifyToolsListChanged() {
       this.asyncServer.notifyToolsListChanged().block();
    }

    
    public McpSchema.ServerCapabilities getServerCapabilities() {
       return this.asyncServer.getServerCapabilities();
    }

    
    public McpSchema.Implementation getServerInfo() {
       return this.asyncServer.getServerInfo();
    }

    
    public void notifyResourcesListChanged() {
       this.asyncServer.notifyResourcesListChanged().block();
    }

    
    public void notifyPromptsListChanged() {
       this.asyncServer.notifyPromptsListChanged().block();
    }

    
    public void loggingNotification(LoggingMessageNotification loggingMessageNotification) {
       this.asyncServer.loggingNotification(loggingMessageNotification).block();
    }

    
    public void closeGracefully() {
       this.asyncServer.closeGracefully().block();
    }

    
    public void close() {
       this.asyncServer.close();
    }

    
    public McpAsyncServer getAsyncServer() {
       return this.asyncServer;
    }

}
```

### McpTransport（传输层接口）

MCP中定义异步传输层的核心接口，负责管理客户端和服务器端之间的双向通信，提供了自定义传输机制的基础，具体功能如下：

- 管理传输连接的生命周期：包括连接到建立、关闭和资源释放
- 处理来自服务器的消息和错误：
- 将客户端生成的消息发送到服务器

```java
public interface McpTransport {
    
    // 关闭传输连接并释放相关资源
    default void close() {
       this.closeGracefully().subscribe();
    }
    // 异步关闭传输连接并释放相关资源
    Mono<Void> closeGracefully();
    // 异步发送消息到服务器
    Mono<Void> sendMessage(JSONRPCMessage message);
    // 将给定数据反序列化为指定类型的对象
    <T> T unmarshalFrom(Object data, TypeReference<T> typeRef);

}
```

### McpClientTransport（客户端传输层接口）

通过connect方法，建立客户端与MCP服务器之间的连接

```java
public interface McpClientTransport extends McpTransport {

    Mono<Void> connect(Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> handler);

}
```

#### StdioClientTransport

通过标准输入输出流（stdin/stdout）与服务器进程进行通信

- `connect`：启动服务器进程并初始化消息处理流。设置进程的命令、参数和环境，然后启动输入、输出和错误处理线程
- `sendMessage`：发送JSON-RPC消息到服务器进程
- `closeGracefully`：优雅地关闭传输、销毁进程并释放调度器资源，发送TERM信号给进程并等待其退出

```java
public class StdioClientTransport implements McpClientTransport {

    private static final Logger logger = LoggerFactory.getLogger(StdioClientTransport.class);

    private final Sinks.Many<JSONRPCMessage> inboundSink;

    private final Sinks.Many<JSONRPCMessage> outboundSink;

    
    private Process process;

    private ObjectMapper objectMapper;

    
    private Scheduler inboundScheduler;

    
    private Scheduler outboundScheduler;

    
    private Scheduler errorScheduler;

    
    private final ServerParameters params;

    private final Sinks.Many<String> errorSink;

    private volatile boolean isClosing = false;

    // visible for tests
    private Consumer<String> stdErrorHandler = error -> logger.info("STDERR Message received: {}", error);

    public StdioClientTransport(ServerParameters params) {
       this(params, new ObjectMapper());
    }

    public StdioClientTransport(ServerParameters params, ObjectMapper objectMapper) {
       Assert.notNull(params, "The params can not be null");
       Assert.notNull(objectMapper, "The ObjectMapper can not be null");

       this.inboundSink = Sinks.many().unicast().onBackpressureBuffer();
       this.outboundSink = Sinks.many().unicast().onBackpressureBuffer();

       this.params = params;

       this.objectMapper = objectMapper;

       this.errorSink = Sinks.many().unicast().onBackpressureBuffer();

       // Start threads
       this.inboundScheduler = Schedulers.fromExecutorService(Executors.newSingleThreadExecutor(), "inbound");
       this.outboundScheduler = Schedulers.fromExecutorService(Executors.newSingleThreadExecutor(), "outbound");
       this.errorScheduler = Schedulers.fromExecutorService(Executors.newSingleThreadExecutor(), "error");
    }

    @Override
    public Mono<Void> connect(Function<Mono<JSONRPCMessage>, Mono<JSONRPCMessage>> handler) {
       return Mono.<Void>fromRunnable(() -> {
          handleIncomingMessages(handler);
          handleIncomingErrors();

          // Prepare command and environment
          List<String> fullCommand = new ArrayList<>();
          fullCommand.add(params.getCommand());
          fullCommand.addAll(params.getArgs());

          ProcessBuilder processBuilder = this.getProcessBuilder();
          processBuilder.command(fullCommand);
          processBuilder.environment().putAll(params.getEnv());

          // Start the process
          try {
             this.process = processBuilder.start();
          }
          catch (IOException e) {
             throw new RuntimeException("Failed to start process with command: " + fullCommand, e);
          }

          // Validate process streams
          if (this.process.getInputStream() == null || process.getOutputStream() == null) {
             this.process.destroy();
             throw new RuntimeException("Process input or output stream is null");
          }

          // Start threads
          startInboundProcessing();
          startOutboundProcessing();
          startErrorProcessing();
       }).subscribeOn(Schedulers.boundedElastic());
    }

    protected ProcessBuilder getProcessBuilder() {
       return new ProcessBuilder();
    }

    public void setStdErrorHandler(Consumer<String> errorHandler) {
       this.stdErrorHandler = errorHandler;
    }

    
    public void awaitForExit() {
       try {
          this.process.waitFor();
       }
       catch (InterruptedException e) {
          throw new RuntimeException("Process interrupted", e);
       }
    }

    private void startErrorProcessing() {
       this.errorScheduler.schedule(() -> {
          try (BufferedReader processErrorReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()))) {
             String line;
             while (!isClosing && (line = processErrorReader.readLine()) != null) {
                try {
                   if (!this.errorSink.tryEmitNext(line).isSuccess()) {
                      if (!isClosing) {
                         logger.error("Failed to emit error message");
                      }
                      break;
                   }
                }
                catch (Exception e) {
                   if (!isClosing) {
                      logger.error("Error processing error message", e);
                   }
                   break;
                }
             }
          }
          catch (IOException e) {
             if (!isClosing) {
                logger.error("Error reading from error stream", e);
             }
          }
          finally {
             isClosing = true;
             errorSink.tryEmitComplete();
          }
       });
    }

    private void handleIncomingMessages(Function<Mono<JSONRPCMessage>, Mono<JSONRPCMessage>> inboundMessageHandler) {
       this.inboundSink.asFlux()
          .flatMap(message -> Mono.just(message)
             .transform(inboundMessageHandler)
             .contextWrite(ctx -> ctx.put("observation", "myObservation")))
          .subscribe();
    }

    private void handleIncomingErrors() {
       this.errorSink.asFlux().subscribe(e -> {
          this.stdErrorHandler.accept(e);
       });
    }

    @Override
    public Mono<Void> sendMessage(JSONRPCMessage message) {
       if (this.outboundSink.tryEmitNext(message).isSuccess()) {
          // TODO: essentially we could reschedule ourselves in some time and make
          // another attempt with the already read data but pause reading until
          // success
          // In this approach we delegate the retry and the backpressure onto the
          // caller. This might be enough for most cases.
          return Mono.empty();
       }
       else {
          return Mono.error(new RuntimeException("Failed to enqueue message"));
       }
    }

    
    private void startInboundProcessing() {
       this.inboundScheduler.schedule(() -> {
          try (BufferedReader processReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
             String line;
             while (!isClosing && (line = processReader.readLine()) != null) {
                try {
                   JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(this.objectMapper, line);
                   if (!this.inboundSink.tryEmitNext(message).isSuccess()) {
                      if (!isClosing) {
                         logger.error("Failed to enqueue inbound message: {}", message);
                      }
                      break;
                   }
                }
                catch (Exception e) {
                   if (!isClosing) {
                      logger.error("Error processing inbound message for line: " + line, e);
                   }
                   break;
                }
             }
          }
          catch (IOException e) {
             if (!isClosing) {
                logger.error("Error reading from input stream", e);
             }
          }
          finally {
             isClosing = true;
             inboundSink.tryEmitComplete();
          }
       });
    }

    private void startOutboundProcessing() {
       this.handleOutbound(messages -> messages
          // this bit is important since writes come from user threads and we
          // want to ensure that the actual writing happens on a dedicated thread
          .publishOn(outboundScheduler)
          .handle((message, s) -> {
             if (message != null && !isClosing) {
                try {
                   String jsonMessage = objectMapper.writeValueAsString(message);
                   // Escape any embedded newlines in the JSON message as per spec:
                   // https://spec.modelcontextprotocol.io/specification/basic/transports/#stdio
                   // - Messages are delimited by newlines, and MUST NOT contain
                   // embedded newlines.
                   jsonMessage = jsonMessage.replace("\r\n", "\\n").replace("\n", "\\n").replace("\r", "\\n");

                   var os = this.process.getOutputStream();
                   synchronized (os) {
                      os.write(jsonMessage.getBytes(StandardCharsets.UTF_8));
                      os.write("\n".getBytes(StandardCharsets.UTF_8));
                      os.flush();
                   }
                   s.next(message);
                }
                catch (IOException e) {
                   s.error(new RuntimeException(e));
                }
             }
          }));
    }

    protected void handleOutbound(Function<Flux<JSONRPCMessage>, Flux<JSONRPCMessage>> outboundConsumer) {
       outboundConsumer.apply(outboundSink.asFlux()).doOnComplete(() -> {
          isClosing = true;
          outboundSink.tryEmitComplete();
       }).doOnError(e -> {
          if (!isClosing) {
             logger.error("Error in outbound processing", e);
             isClosing = true;
             outboundSink.tryEmitComplete();
          }
       }).subscribe();
    }

    @Override
    public Mono<Void> closeGracefully() {
       return Mono.fromRunnable(() -> {
          isClosing = true;
          logger.debug("Initiating graceful shutdown");
       }).then(Mono.defer(() -> {
          // First complete all sinks to stop accepting new messages
          inboundSink.tryEmitComplete();
          outboundSink.tryEmitComplete();
          errorSink.tryEmitComplete();

          // Give a short time for any pending messages to be processed
          return Mono.delay(Duration.ofMillis(100));
       })).then(Mono.defer(() -> {
          logger.debug("Sending TERM to process");
          if (this.process != null) {
             this.process.destroy();
             return Mono.fromFuture(process.onExit());
          }
          else {
             logger.warn("Process not started");
             return Mono.empty();
          }
       })).doOnNext(process -> {
          if (process.exitValue() != 0) {
             logger.warn("Process terminated with code " + process.exitValue());
          }
       }).then(Mono.fromRunnable(() -> {
          try {
             // The Threads are blocked on readLine so disposeGracefully would not
             // interrupt them, therefore we issue an async hard dispose.
             inboundScheduler.dispose();
             errorScheduler.dispose();
             outboundScheduler.dispose();

             logger.debug("Graceful shutdown completed");
          }
          catch (Exception e) {
             logger.error("Error during graceful shutdown", e);
          }
       })).then().subscribeOn(Schedulers.boundedElastic());
    }

    public Sinks.Many<String> getErrorSink() {
       return this.errorSink;
    }

    @Override
    public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
       return this.objectMapper.convertValue(data, typeRef);
    }

}
```

#### WebFluxSseClientTransport

基于Server-Sent Events（SSE）的MCP客户端传输实现类，用于与MCP服务器建立双向通信通道，基于Spring WebFlux的响应式编程

- `connect`：与MCP服务器建立SSE连接，并设置消息处理管道
  - 建立SSE连接
  - 等待服务器发送包含端点的endpoint事件
  - 设置处理传入JSON-RPC消息的处理器
- `sendMessage`：将JSON-RPC消息发送到服务器
- `eventStream`：初始化并启动SSE事件处理流
- `closeGracefully`：优雅地关闭传输，清理所有资源（如订阅和调度器）

```java
public class WebFluxSseClientTransport implements McpClientTransport {

    private static final Logger logger = LoggerFactory.getLogger(WebFluxSseClientTransport.class);

    private static final String MESSAGE_EVENT_TYPE = "message";

    private static final String ENDPOINT_EVENT_TYPE = "endpoint";

    private static final String SSE_ENDPOINT = "/sse";

    private static final ParameterizedTypeReference<ServerSentEvent<String>> SSE_TYPE = new ParameterizedTypeReference<>() {
    };

    private final WebClient webClient;
    
    protected ObjectMapper objectMapper;

    private Disposable inboundSubscription;

    private volatile boolean isClosing = false;

    protected final Sinks.One<String> messageEndpointSink = Sinks.one();

    public WebFluxSseClientTransport(WebClient.Builder webClientBuilder) {
       this(webClientBuilder, new ObjectMapper());
    }

    public WebFluxSseClientTransport(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
       Assert.notNull(objectMapper, "ObjectMapper must not be null");
       Assert.notNull(webClientBuilder, "WebClient.Builder must not be null");

       this.objectMapper = objectMapper;
       this.webClient = webClientBuilder.build();
    }

    @Override
    public Mono<Void> connect(Function<Mono<JSONRPCMessage>, Mono<JSONRPCMessage>> handler) {
       Flux<ServerSentEvent<String>> events = eventStream();
       this.inboundSubscription = events.concatMap(event -> Mono.just(event).<JSONRPCMessage>handle((e, s) -> {
          if (ENDPOINT_EVENT_TYPE.equals(event.event())) {
             String messageEndpointUri = event.data();
             if (messageEndpointSink.tryEmitValue(messageEndpointUri).isSuccess()) {
                s.complete();
             }
             else {
                // TODO: clarify with the spec if multiple events can be
                // received
                s.error(new McpError("Failed to handle SSE endpoint event"));
             }
          }
          else if (MESSAGE_EVENT_TYPE.equals(event.event())) {
             try {
                JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(this.objectMapper, event.data());
                s.next(message);
             }
             catch (IOException ioException) {
                s.error(ioException);
             }
          }
          else {
             s.error(new McpError("Received unrecognized SSE event type: " + event.event()));
          }
       }).transform(handler)).subscribe();

       // The connection is established once the server sends the endpoint event
       return messageEndpointSink.asMono().then();
    }
    
    @Override
    public Mono<Void> sendMessage(JSONRPCMessage message) {
       // The messageEndpoint is the endpoint URI to send the messages
       // It is provided by the server as part of the endpoint event
       return messageEndpointSink.asMono().flatMap(messageEndpointUri -> {
          if (isClosing) {
             return Mono.empty();
          }
          try {
             String jsonText = this.objectMapper.writeValueAsString(message);
             return webClient.post()
                .uri(messageEndpointUri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(jsonText)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(response -> {
                   logger.debug("Message sent successfully");
                })
                .doOnError(error -> {
                   if (!isClosing) {
                      logger.error("Error sending message: {}", error.getMessage());
                   }
                });
          }
          catch (IOException e) {
             if (!isClosing) {
                return Mono.error(new RuntimeException("Failed to serialize message", e));
             }
             return Mono.empty();
          }
       }).then(); // TODO: Consider non-200-ok response
    }

    // visible for tests
    protected Flux<ServerSentEvent<String>> eventStream() {// @formatter:off
       return this.webClient
          .get()
          .uri(SSE_ENDPOINT)
          .accept(MediaType.TEXT_EVENT_STREAM)
          .retrieve()
          .bodyToFlux(SSE_TYPE)
          .retryWhen(Retry.from(retrySignal -> retrySignal.handle(inboundRetryHandler)));
    } // @formatter:on

    private BiConsumer<RetrySignal, SynchronousSink<Object>> inboundRetryHandler = (retrySpec, sink) -> {
       if (isClosing) {
          logger.debug("SSE connection closed during shutdown");
          sink.error(retrySpec.failure());
          return;
       }
       if (retrySpec.failure() instanceof IOException) {
          logger.debug("Retrying SSE connection after IO error");
          sink.next(retrySpec);
          return;
       }
       logger.error("Fatal SSE error, not retrying: {}", retrySpec.failure().getMessage());
       sink.error(retrySpec.failure());
    };

    @Override
    public Mono<Void> closeGracefully() { // @formatter:off
       return Mono.fromRunnable(() -> {
          isClosing = true;
          
          if (inboundSubscription != null) {
             inboundSubscription.dispose();
          }

       })
       .then()
       .subscribeOn(Schedulers.boundedElastic());
    } // @formatter:on

    @Override
    public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
       return this.objectMapper.convertValue(data, typeRef);
    }

}
```

#### HttpClientSseClientTransport

基于Server-Sent Events（SSE）的MCP客户端传输实现类，用于与MCP服务器建立双向通信通道。使用Java的HttpClient进行通信

- `connect`：与MCP服务器建立SSE连接，并设置消息处理管道
  - 建立SSE连接（订阅SSE事件 + 处理SSE事件）
  - 等待服务器发送包含端点的endpoint事件
  - 设置处理传入JSON-RPC消息的处理器
- `sendMessage`：将JSON-RPC消息发送到服务器
- `closeGracefully`：优雅地关闭传输，清理所有资源（如订阅和调度器）

```java
public class HttpClientSseClientTransport implements McpClientTransport {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientSseClientTransport.class);

    
    private static final String MESSAGE_EVENT_TYPE = "message";

    
    private static final String ENDPOINT_EVENT_TYPE = "endpoint";

    
    private static final String SSE_ENDPOINT = "/sse";

    
    private final String baseUri;

    
    private final FlowSseClient sseClient;

    
    private final HttpClient httpClient;

    
    protected ObjectMapper objectMapper;

    
    private volatile boolean isClosing = false;

    
    private final CountDownLatch closeLatch = new CountDownLatch(1);

    
    private final AtomicReference<String> messageEndpoint = new AtomicReference<>();

    
    private final AtomicReference<CompletableFuture<Void>> connectionFuture = new AtomicReference<>();

    
    public HttpClientSseClientTransport(String baseUri) {
       this(HttpClient.newBuilder(), baseUri, new ObjectMapper());
    }

    public HttpClientSseClientTransport(HttpClient.Builder clientBuilder, String baseUri, ObjectMapper objectMapper) {
       Assert.notNull(objectMapper, "ObjectMapper must not be null");
       Assert.hasText(baseUri, "baseUri must not be empty");
       Assert.notNull(clientBuilder, "clientBuilder must not be null");
       this.baseUri = baseUri;
       this.objectMapper = objectMapper;
       this.httpClient = clientBuilder.connectTimeout(Duration.ofSeconds(10)).build();
       this.sseClient = new FlowSseClient(this.httpClient);
    }
    
    @Override
    public Mono<Void> connect(Function<Mono<JSONRPCMessage>, Mono<JSONRPCMessage>> handler) {
       CompletableFuture<Void> future = new CompletableFuture<>();
       connectionFuture.set(future);

       sseClient.subscribe(this.baseUri + SSE_ENDPOINT, new FlowSseClient.SseEventHandler() {
          @Override
          public void onEvent(SseEvent event) {
             if (isClosing) {
                return;
             }

             try {
                if (ENDPOINT_EVENT_TYPE.equals(event.type())) {
                   String endpoint = event.data();
                   messageEndpoint.set(endpoint);
                   closeLatch.countDown();
                   future.complete(null);
                }
                else if (MESSAGE_EVENT_TYPE.equals(event.type())) {
                   JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper, event.data());
                   handler.apply(Mono.just(message)).subscribe();
                }
                else {
                   logger.error("Received unrecognized SSE event type: {}", event.type());
                }
             }
             catch (IOException e) {
                logger.error("Error processing SSE event", e);
                future.completeExceptionally(e);
             }
          }

          @Override
          public void onError(Throwable error) {
             if (!isClosing) {
                logger.error("SSE connection error", error);
                future.completeExceptionally(error);
             }
          }
       });

       return Mono.fromFuture(future);
    }

    @Override
    public Mono<Void> sendMessage(JSONRPCMessage message) {
       if (isClosing) {
          return Mono.empty();
       }

       try {
          if (!closeLatch.await(10, TimeUnit.SECONDS)) {
             return Mono.error(new McpError("Failed to wait for the message endpoint"));
          }
       }
       catch (InterruptedException e) {
          return Mono.error(new McpError("Failed to wait for the message endpoint"));
       }

       String endpoint = messageEndpoint.get();
       if (endpoint == null) {
          return Mono.error(new McpError("No message endpoint available"));
       }

       try {
          String jsonText = this.objectMapper.writeValueAsString(message);
          HttpRequest request = HttpRequest.newBuilder()
             .uri(URI.create(this.baseUri + endpoint))
             .header("Content-Type", "application/json")
             .POST(HttpRequest.BodyPublishers.ofString(jsonText))
             .build();

          return Mono.fromFuture(
                httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding()).thenAccept(response -> {
                   if (response.statusCode() != 200 && response.statusCode() != 201 && response.statusCode() != 202
                         && response.statusCode() != 206) {
                      logger.error("Error sending message: {}", response.statusCode());
                   }
                }));
       }
       catch (IOException e) {
          if (!isClosing) {
             return Mono.error(new RuntimeException("Failed to serialize message", e));
          }
          return Mono.empty();
       }
    }
    
    @Override
    public Mono<Void> closeGracefully() {
       return Mono.fromRunnable(() -> {
          isClosing = true;
          CompletableFuture<Void> future = connectionFuture.get();
          if (future != null && !future.isDone()) {
             future.cancel(true);
          }
       });
    }

    @Override
    public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
       return this.objectMapper.convertValue(data, typeRef);
    }

}
```

#### ClientTransport对比总结

| 特性         | StdioClientTransport                     | WebFluxSseClientTransport                    | HttpClientSseClientTransport                   |
| :----------- | :--------------------------------------- | :------------------------------------------- | :--------------------------------------------- |
| 底层技术     | 基于标准的输入输出流                     | 基于Spring WebFlux的响应式编程               | 基于Java的HttpClient的自定义SSE客户端          |
| 通信协议     | 通过标准输入输出流传                     | HTTP + SSE                                   | HTTP + SSE                                     |
| 消息传输方式 | 双向通过标准输入输出流传输               | 服务器到客户端：SSE客户端到服务器：HTTP POST | 服务器到客户端：SSE客户端到服务器：HTTP POST   |
| 错误处理     | 通过标准错误流和日志处理错误             | 通过WebFlux到错误处理机制处理连接和消息错误  | 通过自定义SSE客户端和HttpClient处理错误        |
| 资源管理     | 手动管理管理进程和线程资源，支持优雅关闭 | 使用WebFlux到资源管理机制，支持自动释放资源  | 使用HttpClient的资源管理机制，支持手动释放资源 |
| 扩展性       | 低，主要用于本地进程通信                 | 高，支持高并发和低延迟的响应式处理           | 中，可通过自定义HttpClient和SSE逻辑扩展        |
| 适用场景     | 本地进程间的通信或命令行工具集成         | 微服务架构或需要高并发处理的分布式系统       | 轻量级HTTP通信的场景                           |

在消息传输方式方面，为什么要这样设计？

- SSE的特性：SSE单向，服务器可主动向客户端推送消息，但客户端无法通过SSE向服务器发送消息
- 双向通信需求：为了实现客户端到服务器的消息传输，需额外的机制（HTTP POST）
- 分离关注点：SSE负责服务器到客户端端实时消息推送，HTTP POST负责客户端到服务器的消息发送，职责清晰

### McpServerTransport（服务端传输层接口）

```java
public interface McpServerTransport extends McpTransport {

}
```

#### StdioMcpSessionTransport

StdioServerTransportProvider的内部类，

```bash
private class StdioMcpSessionTransport implements McpServerTransport {

    private final Sinks.Many<JSONRPCMessage> inboundSink;

    private final Sinks.Many<JSONRPCMessage> outboundSink;

    private final AtomicBoolean isStarted = new AtomicBoolean(false);

    
    private Scheduler inboundScheduler;

    
    private Scheduler outboundScheduler;

    private final Sinks.One<Void> outboundReady = Sinks.one();

    public StdioMcpSessionTransport() {

       this.inboundSink = Sinks.many().unicast().onBackpressureBuffer();
       this.outboundSink = Sinks.many().unicast().onBackpressureBuffer();

       // Use bounded schedulers for better resource management
       this.inboundScheduler = Schedulers.fromExecutorService(Executors.newSingleThreadExecutor(),
             "stdio-inbound");
       this.outboundScheduler = Schedulers.fromExecutorService(Executors.newSingleThreadExecutor(),
             "stdio-outbound");
    }

    @Override
    public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {

       return Mono.zip(inboundReady.asMono(), outboundReady.asMono()).then(Mono.defer(() -> {
          if (outboundSink.tryEmitNext(message).isSuccess()) {
             return Mono.empty();
          }
          else {
             return Mono.error(new RuntimeException("Failed to enqueue message"));
          }
       }));
    }

    @Override
    public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
       return objectMapper.convertValue(data, typeRef);
    }

    @Override
    public Mono<Void> closeGracefully() {
       return Mono.fromRunnable(() -> {
          isClosing.set(true);
          logger.debug("Session transport closing gracefully");
          inboundSink.tryEmitComplete();
       });
    }

    @Override
    public void close() {
       isClosing.set(true);
       logger.debug("Session transport closed");
    }

    private void initProcessing() {
       handleIncomingMessages();
       startInboundProcessing();
       startOutboundProcessing();
    }

    private void handleIncomingMessages() {
       this.inboundSink.asFlux().flatMap(message -> session.handle(message)).doOnTerminate(() -> {
          // The outbound processing will dispose its scheduler upon completion
          this.outboundSink.tryEmitComplete();
          this.inboundScheduler.dispose();
       }).subscribe();
    }

    
    private void startInboundProcessing() {
       if (isStarted.compareAndSet(false, true)) {
          this.inboundScheduler.schedule(() -> {
             inboundReady.tryEmitValue(null);
             BufferedReader reader = null;
             try {
                reader = new BufferedReader(new InputStreamReader(inputStream));
                while (!isClosing.get()) {
                   try {
                      String line = reader.readLine();
                      if (line == null || isClosing.get()) {
                         break;
                      }

                      logger.debug("Received JSON message: {}", line);

                      try {
                         McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper,
                               line);
                         if (!this.inboundSink.tryEmitNext(message).isSuccess()) {
                            // logIfNotClosing("Failed to enqueue message");
                            break;
                         }

                      }
                      catch (Exception e) {
                         logIfNotClosing("Error processing inbound message", e);
                         break;
                      }
                   }
                   catch (IOException e) {
                      logIfNotClosing("Error reading from stdin", e);
                      break;
                   }
                }
             }
             catch (Exception e) {
                logIfNotClosing("Error in inbound processing", e);
             }
             finally {
                isClosing.set(true);
                if (session != null) {
                   session.close();
                }
                inboundSink.tryEmitComplete();
             }
          });
       }
    }

    
    private void startOutboundProcessing() {
       Function<Flux<JSONRPCMessage>, Flux<JSONRPCMessage>> outboundConsumer = messages -> messages // @formatter:off
           .doOnSubscribe(subscription -> outboundReady.tryEmitValue(null))
           .publishOn(outboundScheduler)
           .handle((message, sink) -> {
              if (message != null && !isClosing.get()) {
                 try {
                    String jsonMessage = objectMapper.writeValueAsString(message);
                    // Escape any embedded newlines in the JSON message as per spec
                    jsonMessage = jsonMessage.replace("\r\n", "\\n").replace("\n", "\\n").replace("\r", "\\n");

                    synchronized (outputStream) {
                       outputStream.write(jsonMessage.getBytes(StandardCharsets.UTF_8));
                       outputStream.write("\n".getBytes(StandardCharsets.UTF_8));
                       outputStream.flush();
                    }
                    sink.next(message);
                 }
                 catch (IOException e) {
                    if (!isClosing.get()) {
                       logger.error("Error writing message", e);
                       sink.error(new RuntimeException(e));
                    }
                    else {
                       logger.debug("Stream closed during shutdown", e);
                    }
                 }
              }
              else if (isClosing.get()) {
                 sink.complete();
              }
           })
           .doOnComplete(() -> {
              isClosing.set(true);
              outboundScheduler.dispose();
           })
           .doOnError(e -> {
              if (!isClosing.get()) {
                 logger.error("Error in outbound processing", e);
                 isClosing.set(true);
                 outboundScheduler.dispose();
              }
           })
           .map(msg -> (JSONRPCMessage) msg);

           outboundConsumer.apply(outboundSink.asFlux()).subscribe();
     } // @formatter:on

    private void logIfNotClosing(String message, Exception e) {
       if (!isClosing.get()) {
          logger.error(message, e);
       }
    }

}
```

#### WebFluxMcpSessionTransport

WebFluxSseServerTransportProvider的内部类

```Java
private class WebFluxMcpSessionTransport implements McpServerTransport {

    private final FluxSink<ServerSentEvent<?>> sink;

    public WebFluxMcpSessionTransport(FluxSink<ServerSentEvent<?>> sink) {
       this.sink = sink;
    }

    @Override
    public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
       return Mono.fromSupplier(() -> {
          try {
             return objectMapper.writeValueAsString(message);
          }
          catch (IOException e) {
             throw Exceptions.propagate(e);
          }
       }).doOnNext(jsonText -> {
          ServerSentEvent<Object> event = ServerSentEvent.builder()
             .event(MESSAGE_EVENT_TYPE)
             .data(jsonText)
             .build();
          sink.next(event);
       }).doOnError(e -> {
          // TODO log with sessionid
          Throwable exception = Exceptions.unwrap(e);
          sink.error(exception);
       }).then();
    }

    @Override
    public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
       return objectMapper.convertValue(data, typeRef);
    }

    @Override
    public Mono<Void> closeGracefully() {
       return Mono.fromRunnable(sink::complete);
    }

    @Override
    public void close() {
       sink.complete();
    }

}
```

#### HttpServletMcpSessionTransport

HttpServletSseServerTransportProvider的内部类

```Java
private class HttpServletMcpSessionTransport implements McpServerTransport {

    private final String sessionId;

    private final AsyncContext asyncContext;

    private final PrintWriter writer;


    HttpServletMcpSessionTransport(String sessionId, AsyncContext asyncContext, PrintWriter writer) {
       this.sessionId = sessionId;
       this.asyncContext = asyncContext;
       this.writer = writer;
       logger.debug("Session transport {} initialized with SSE writer", sessionId);
    }

    @Override
    public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
       return Mono.fromRunnable(() -> {
          try {
             String jsonText = objectMapper.writeValueAsString(message);
             sendEvent(writer, MESSAGE_EVENT_TYPE, jsonText);
             logger.debug("Message sent to session {}", sessionId);
          }
          catch (Exception e) {
             logger.error("Failed to send message to session {}: {}", sessionId, e.getMessage());
             sessions.remove(sessionId);
             asyncContext.complete();
          }
       });
    }

    @Override
    public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
       return objectMapper.convertValue(data, typeRef);
    }

    @Override
    public Mono<Void> closeGracefully() {
       return Mono.fromRunnable(() -> {
          logger.debug("Closing session transport: {}", sessionId);
          try {
             sessions.remove(sessionId);
             asyncContext.complete();
             logger.debug("Successfully completed async context for session {}", sessionId);
          }
          catch (Exception e) {
             logger.warn("Failed to complete async context for session {}: {}", sessionId, e.getMessage());
          }
       });
    }

    @Override
    public void close() {
       try {
          sessions.remove(sessionId);
          asyncContext.complete();
          logger.debug("Successfully completed async context for session {}", sessionId);
       }
       catch (Exception e) {
          logger.warn("Failed to complete async context for session {}: {}", sessionId, e.getMessage());
       }
    }

}
```

#### WebMvcMcpSessionTransport

WebMvcSseServerTransportProvider的内部类

```Java
private class WebMvcMcpSessionTransport implements McpServerTransport {

    private final String sessionId;

    private final SseBuilder sseBuilder;

    
    WebMvcMcpSessionTransport(String sessionId, SseBuilder sseBuilder) {
       this.sessionId = sessionId;
       this.sseBuilder = sseBuilder;
       logger.debug("Session transport {} initialized with SSE builder", sessionId);
    }

    
    @Override
    public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
       return Mono.fromRunnable(() -> {
          try {
             String jsonText = objectMapper.writeValueAsString(message);
             sseBuilder.id(sessionId).event(MESSAGE_EVENT_TYPE).data(jsonText);
             logger.debug("Message sent to session {}", sessionId);
          }
          catch (Exception e) {
             logger.error("Failed to send message to session {}: {}", sessionId, e.getMessage());
             sseBuilder.error(e);
          }
       });
    }

    
    @Override
    public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
       return objectMapper.convertValue(data, typeRef);
    }

    
    @Override
    public Mono<Void> closeGracefully() {
       return Mono.fromRunnable(() -> {
          logger.debug("Closing session transport: {}", sessionId);
          try {
             sseBuilder.complete();
             logger.debug("Successfully completed SSE builder for session {}", sessionId);
          }
          catch (Exception e) {
             logger.warn("Failed to complete SSE builder for session {}: {}", sessionId, e.getMessage());
          }
       });
    }

    
    @Override
    public void close() {
       try {
          sseBuilder.complete();
          logger.debug("Successfully completed SSE builder for session {}", sessionId);
       }
       catch (Exception e) {
          logger.warn("Failed to complete SSE builder for session {}: {}", sessionId, e.getMessage());
       }
    }

}
```

### McpServerTransportProvider（创建、管理McpServerTransport实例）

创建和管理McpServerTransport实例

```java
public interface McpServerTransportProvider {

    // 设置会话工厂，用于创建新的客户端会话
    void setSessionFactory(McpServerSession.Factory sessionFactory);

    // 面向所有连接的客户端发送通知
    Mono<Void> notifyClients(String method, Map<String, Object> params);

    // 立即关闭所有传输连接并释放资源
    default void close() {
       this.closeGracefully().subscribe();
    }

    // 异步地优雅关闭所有传输连接并释放资源
    Mono<Void> closeGracefully();

}
```

#### StdioServerTransportProvider

创建StdioMcpSessionTransport实例类，通过标准输入输出流进行通信

```Java
public class StdioServerTransportProvider implements McpServerTransportProvider {

    private static final Logger logger = LoggerFactory.getLogger(StdioServerTransportProvider.class);

    private final ObjectMapper objectMapper;

    private final InputStream inputStream;

    private final OutputStream outputStream;

    private McpServerSession session;

    private final AtomicBoolean isClosing = new AtomicBoolean(false);

    private final Sinks.One<Void> inboundReady = Sinks.one();

    public StdioServerTransportProvider() {
       this(new ObjectMapper());
    }

    public StdioServerTransportProvider(ObjectMapper objectMapper) {
       this(objectMapper, System.in, System.out);
    }

    public StdioServerTransportProvider(ObjectMapper objectMapper, InputStream inputStream, OutputStream outputStream) {
       Assert.notNull(objectMapper, "The ObjectMapper can not be null");
       Assert.notNull(inputStream, "The InputStream can not be null");
       Assert.notNull(outputStream, "The OutputStream can not be null");

       this.objectMapper = objectMapper;
       this.inputStream = inputStream;
       this.outputStream = outputStream;
    }

    @Override
    public void setSessionFactory(McpServerSession.Factory sessionFactory) {
       // Create a single session for the stdio connection
       var transport = new StdioMcpSessionTransport();
       this.session = sessionFactory.create(transport);
       transport.initProcessing();
    }

    @Override
    public Mono<Void> notifyClients(String method, Map<String, Object> params) {
       if (this.session == null) {
          return Mono.error(new McpError("No session to close"));
       }
       return this.session.sendNotification(method, params)
          .doOnError(e -> logger.error("Failed to send notification: {}", e.getMessage()));
    }

    @Override
    public Mono<Void> closeGracefully() {
       if (this.session == null) {
          return Mono.empty();
       }
       return this.session.closeGracefully();
    }
}
```

#### WebFluxSseServerTransportProvider

创建WebFluxMcpSessionTransport实例，提供基于WebFlux的传输层实现。

- getRouterFunction：返回WebFlux的路由函数，定义了传输层的HTTP端点。
  - GET/see：用于建立SSE连接
  - POST/message：用于接收客户端消息

```Java
public class WebFluxSseServerTransportProvider implements McpServerTransportProvider {

    private static final Logger logger = LoggerFactory.getLogger(WebFluxSseServerTransportProvider.class);

    
    public static final String MESSAGE_EVENT_TYPE = "message";

    
    public static final String ENDPOINT_EVENT_TYPE = "endpoint";

    
    public static final String DEFAULT_SSE_ENDPOINT = "/sse";

    private final ObjectMapper objectMapper;

    private final String messageEndpoint;

    private final String sseEndpoint;

    private final RouterFunction<?> routerFunction;

    private McpServerSession.Factory sessionFactory;

    
    private final ConcurrentHashMap<String, McpServerSession> sessions = new ConcurrentHashMap<>();

    
    private volatile boolean isClosing = false;

    public WebFluxSseServerTransportProvider(ObjectMapper objectMapper, String messageEndpoint, String sseEndpoint) {
       Assert.notNull(objectMapper, "ObjectMapper must not be null");
       Assert.notNull(messageEndpoint, "Message endpoint must not be null");
       Assert.notNull(sseEndpoint, "SSE endpoint must not be null");

       this.objectMapper = objectMapper;
       this.messageEndpoint = messageEndpoint;
       this.sseEndpoint = sseEndpoint;
       this.routerFunction = RouterFunctions.route()
          .GET(this.sseEndpoint, this::handleSseConnection)
          .POST(this.messageEndpoint, this::handleMessage)
          .build();
    }

    public WebFluxSseServerTransportProvider(ObjectMapper objectMapper, String messageEndpoint) {
       this(objectMapper, messageEndpoint, DEFAULT_SSE_ENDPOINT);
    }

    @Override
    public void setSessionFactory(McpServerSession.Factory sessionFactory) {
       this.sessionFactory = sessionFactory;
    }
    
    @Override
    public Mono<Void> notifyClients(String method, Map<String, Object> params) {
       if (sessions.isEmpty()) {
          logger.debug("No active sessions to broadcast message to");
          return Mono.empty();
       }

       logger.debug("Attempting to broadcast message to {} active sessions", sessions.size());

       return Flux.fromStream(sessions.values().stream())
          .flatMap(session -> session.sendNotification(method, params)
             .doOnError(e -> logger.error("Failed to " + "send message to session " + "{}: {}", session.getId(),
                   e.getMessage()))
             .onErrorComplete())
          .then();
    }

    @Override
    public Mono<Void> closeGracefully() {
       return Flux.fromIterable(sessions.values())
          .doFirst(() -> logger.debug("Initiating graceful shutdown with {} active sessions", sessions.size()))
          .flatMap(McpServerSession::closeGracefully)
          .then();
    }

    public RouterFunction<?> getRouterFunction() {
       return this.routerFunction;
    }


    private Mono<ServerResponse> handleSseConnection(ServerRequest request) {
       if (isClosing) {
          return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).bodyValue("Server is shutting down");
       }

       return ServerResponse.ok()
          .contentType(MediaType.TEXT_EVENT_STREAM)
          .body(Flux.<ServerSentEvent<?>>create(sink -> {
             WebFluxMcpSessionTransport sessionTransport = new WebFluxMcpSessionTransport(sink);

             McpServerSession session = sessionFactory.create(sessionTransport);
             String sessionId = session.getId();

             logger.debug("Created new SSE connection for session: {}", sessionId);
             sessions.put(sessionId, session);

             // Send initial endpoint event
             logger.debug("Sending initial endpoint event to session: {}", sessionId);
             sink.next(ServerSentEvent.builder()
                .event(ENDPOINT_EVENT_TYPE)
                .data(messageEndpoint + "?sessionId=" + sessionId)
                .build());
             sink.onCancel(() -> {
                logger.debug("Session {} cancelled", sessionId);
                sessions.remove(sessionId);
             });
          }), ServerSentEvent.class);
    }
    
    private Mono<ServerResponse> handleMessage(ServerRequest request) {
       if (isClosing) {
          return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).bodyValue("Server is shutting down");
       }

       if (request.queryParam("sessionId").isEmpty()) {
          return ServerResponse.badRequest().bodyValue(new McpError("Session ID missing in message endpoint"));
       }

       McpServerSession session = sessions.get(request.queryParam("sessionId").get());

       return request.bodyToMono(String.class).flatMap(body -> {
          try {
             McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper, body);
             return session.handle(message).flatMap(response -> ServerResponse.ok().build()).onErrorResume(error -> {
                logger.error("Error processing  message: {}", error.getMessage());
                // TODO: instead of signalling the error, just respond with 200 OK
                // - the error is signalled on the SSE connection
                // return ServerResponse.ok().build();
                return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .bodyValue(new McpError(error.getMessage()));
             });
          }
          catch (IllegalArgumentException | IOException e) {
             logger.error("Failed to deserialize message: {}", e.getMessage());
             return ServerResponse.badRequest().bodyValue(new McpError("Invalid message format"));
          }
       });
    }
}
```

#### HttpServletSseServerTransportProvider

创建HttpServletMcpSessionTransport实例，基于Servlet API的HTTP服务器传输提供者

- doGet：处理客户端的GET请求，建立SSE连接
- doPost：处理客户端的POST请求，接收和处理客户端发送的消息

```Java
@WebServlet(asyncSupported = true)
public class HttpServletSseServerTransportProvider extends HttpServlet implements McpServerTransportProvider {

    
    private static final Logger logger = LoggerFactory.getLogger(HttpServletSseServerTransportProvider.class);

    public static final String UTF_8 = "UTF-8";

    public static final String APPLICATION_JSON = "application/json";

    public static final String FAILED_TO_SEND_ERROR_RESPONSE = "Failed to send error response: {}";

    
    public static final String DEFAULT_SSE_ENDPOINT = "/sse";

    
    public static final String MESSAGE_EVENT_TYPE = "message";

    
    public static final String ENDPOINT_EVENT_TYPE = "endpoint";

    
    private final ObjectMapper objectMapper;

    
    private final String messageEndpoint;

    
    private final String sseEndpoint;

    
    private final Map<String, McpServerSession> sessions = new ConcurrentHashMap<>();

    
    private final AtomicBoolean isClosing = new AtomicBoolean(false);

    
    private McpServerSession.Factory sessionFactory;

    public HttpServletSseServerTransportProvider(ObjectMapper objectMapper, String messageEndpoint,
          String sseEndpoint) {
       this.objectMapper = objectMapper;
       this.messageEndpoint = messageEndpoint;
       this.sseEndpoint = sseEndpoint;
    }

    public HttpServletSseServerTransportProvider(ObjectMapper objectMapper, String messageEndpoint) {
       this(objectMapper, messageEndpoint, DEFAULT_SSE_ENDPOINT);
    }

    @Override
    public void setSessionFactory(McpServerSession.Factory sessionFactory) {
       this.sessionFactory = sessionFactory;
    }

    @Override
    public Mono<Void> notifyClients(String method, Map<String, Object> params) {
       if (sessions.isEmpty()) {
          logger.debug("No active sessions to broadcast message to");
          return Mono.empty();
       }

       logger.debug("Attempting to broadcast message to {} active sessions", sessions.size());

       return Flux.fromIterable(sessions.values())
          .flatMap(session -> session.sendNotification(method, params)
             .doOnError(
                   e -> logger.error("Failed to send message to session {}: {}", session.getId(), e.getMessage()))
             .onErrorComplete())
          .then();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {

       String requestURI = request.getRequestURI();
       if (!requestURI.endsWith(sseEndpoint)) {
          response.sendError(HttpServletResponse.SC_NOT_FOUND);
          return;
       }

       if (isClosing.get()) {
          response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Server is shutting down");
          return;
       }

       response.setContentType("text/event-stream");
       response.setCharacterEncoding(UTF_8);
       response.setHeader("Cache-Control", "no-cache");
       response.setHeader("Connection", "keep-alive");
       response.setHeader("Access-Control-Allow-Origin", "*");

       String sessionId = UUID.randomUUID().toString();
       AsyncContext asyncContext = request.startAsync();
       asyncContext.setTimeout(0);

       PrintWriter writer = response.getWriter();

       // Create a new session transport
       HttpServletMcpSessionTransport sessionTransport = new HttpServletMcpSessionTransport(sessionId, asyncContext,
             writer);

       // Create a new session using the session factory
       McpServerSession session = sessionFactory.create(sessionTransport);
       this.sessions.put(sessionId, session);

       // Send initial endpoint event
       this.sendEvent(writer, ENDPOINT_EVENT_TYPE, messageEndpoint + "?sessionId=" + sessionId);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
          throws ServletException, IOException {

       if (isClosing.get()) {
          response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Server is shutting down");
          return;
       }

       String requestURI = request.getRequestURI();
       if (!requestURI.endsWith(messageEndpoint)) {
          response.sendError(HttpServletResponse.SC_NOT_FOUND);
          return;
       }

       // Get the session ID from the request parameter
       String sessionId = request.getParameter("sessionId");
       if (sessionId == null) {
          response.setContentType(APPLICATION_JSON);
          response.setCharacterEncoding(UTF_8);
          response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
          String jsonError = objectMapper.writeValueAsString(new McpError("Session ID missing in message endpoint"));
          PrintWriter writer = response.getWriter();
          writer.write(jsonError);
          writer.flush();
          return;
       }

       // Get the session from the sessions map
       McpServerSession session = sessions.get(sessionId);
       if (session == null) {
          response.setContentType(APPLICATION_JSON);
          response.setCharacterEncoding(UTF_8);
          response.setStatus(HttpServletResponse.SC_NOT_FOUND);
          String jsonError = objectMapper.writeValueAsString(new McpError("Session not found: " + sessionId));
          PrintWriter writer = response.getWriter();
          writer.write(jsonError);
          writer.flush();
          return;
       }

       try {
          BufferedReader reader = request.getReader();
          StringBuilder body = new StringBuilder();
          String line;
          while ((line = reader.readLine()) != null) {
             body.append(line);
          }

          McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper, body.toString());

          // Process the message through the session's handle method
          session.handle(message).block(); // Block for Servlet compatibility

          response.setStatus(HttpServletResponse.SC_OK);
       }
       catch (Exception e) {
          logger.error("Error processing message: {}", e.getMessage());
          try {
             McpError mcpError = new McpError(e.getMessage());
             response.setContentType(APPLICATION_JSON);
             response.setCharacterEncoding(UTF_8);
             response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
             String jsonError = objectMapper.writeValueAsString(mcpError);
             PrintWriter writer = response.getWriter();
             writer.write(jsonError);
             writer.flush();
          }
          catch (IOException ex) {
             logger.error(FAILED_TO_SEND_ERROR_RESPONSE, ex.getMessage());
             response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing message");
          }
       }
    }
    
    @Override
    public Mono<Void> closeGracefully() {
       isClosing.set(true);
       logger.debug("Initiating graceful shutdown with {} active sessions", sessions.size());

       return Flux.fromIterable(sessions.values()).flatMap(McpServerSession::closeGracefully).then();
    }

    private void sendEvent(PrintWriter writer, String eventType, String data) throws IOException {
       writer.write("event: " + eventType + "\n");
       writer.write("data: " + data + "\n\n");
       writer.flush();

       if (writer.checkError()) {
          throw new IOException("Client disconnected");
       }
    }

    @Override
    public void destroy() {
       closeGracefully().block();
       super.destroy();
    }
 }
```

#### WebMvcSseServerTransportProvider

创建WebMvcMcpSessionTransport实例，基于Spring WebMVC的服务端传输提供者，支持通过SSE实现服务端与客户端之间的双向通信

- getRouterFunction：返回Web MVC的路由函数，定义了传输层的HTTP端点。
  - GET/see：用于建立SSE连接
  - POST/message：用于接收客户端消息

```Java
public class WebMvcSseServerTransportProvider implements McpServerTransportProvider {

    private static final Logger logger = LoggerFactory.getLogger(WebMvcSseServerTransportProvider.class);

    
    public static final String MESSAGE_EVENT_TYPE = "message";

    
    public static final String ENDPOINT_EVENT_TYPE = "endpoint";

    
    public static final String DEFAULT_SSE_ENDPOINT = "/sse";

    private final ObjectMapper objectMapper;

    private final String messageEndpoint;

    private final String sseEndpoint;

    private final RouterFunction<ServerResponse> routerFunction;

    private McpServerSession.Factory sessionFactory;

    
    private final ConcurrentHashMap<String, McpServerSession> sessions = new ConcurrentHashMap<>();

    
    private volatile boolean isClosing = false;

    public WebMvcSseServerTransportProvider(ObjectMapper objectMapper, String messageEndpoint, String sseEndpoint) {
       Assert.notNull(objectMapper, "ObjectMapper must not be null");
       Assert.notNull(messageEndpoint, "Message endpoint must not be null");
       Assert.notNull(sseEndpoint, "SSE endpoint must not be null");

       this.objectMapper = objectMapper;
       this.messageEndpoint = messageEndpoint;
       this.sseEndpoint = sseEndpoint;
       this.routerFunction = RouterFunctions.route()
          .GET(this.sseEndpoint, this::handleSseConnection)
          .POST(this.messageEndpoint, this::handleMessage)
          .build();
    }

    public WebMvcSseServerTransportProvider(ObjectMapper objectMapper, String messageEndpoint) {
       this(objectMapper, messageEndpoint, DEFAULT_SSE_ENDPOINT);
    }

    @Override
    public void setSessionFactory(McpServerSession.Factory sessionFactory) {
       this.sessionFactory = sessionFactory;
    }

    @Override
    public Mono<Void> notifyClients(String method, Map<String, Object> params) {
       if (sessions.isEmpty()) {
          logger.debug("No active sessions to broadcast message to");
          return Mono.empty();
       }

       logger.debug("Attempting to broadcast message to {} active sessions", sessions.size());

       return Flux.fromIterable(sessions.values())
          .flatMap(session -> session.sendNotification(method, params)
             .doOnError(
                   e -> logger.error("Failed to send message to session {}: {}", session.getId(), e.getMessage()))
             .onErrorComplete())
          .then();
    }

    @Override
    public Mono<Void> closeGracefully() {
       return Flux.fromIterable(sessions.values()).doFirst(() -> {
          this.isClosing = true;
          logger.debug("Initiating graceful shutdown with {} active sessions", sessions.size());
       })
          .flatMap(McpServerSession::closeGracefully)
          .then()
          .doOnSuccess(v -> logger.debug("Graceful shutdown completed"));
    }

    public RouterFunction<ServerResponse> getRouterFunction() {
       return this.routerFunction;
    }

    private ServerResponse handleSseConnection(ServerRequest request) {
       if (this.isClosing) {
          return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Server is shutting down");
       }

       String sessionId = UUID.randomUUID().toString();
       logger.debug("Creating new SSE connection for session: {}", sessionId);

       // Send initial endpoint event
       try {
          return ServerResponse.sse(sseBuilder -> {
             sseBuilder.onComplete(() -> {
                logger.debug("SSE connection completed for session: {}", sessionId);
                sessions.remove(sessionId);
             });
             sseBuilder.onTimeout(() -> {
                logger.debug("SSE connection timed out for session: {}", sessionId);
                sessions.remove(sessionId);
             });

             WebMvcMcpSessionTransport sessionTransport = new WebMvcMcpSessionTransport(sessionId, sseBuilder);
             McpServerSession session = sessionFactory.create(sessionTransport);
             this.sessions.put(sessionId, session);

             try {
                sseBuilder.id(sessionId)
                   .event(ENDPOINT_EVENT_TYPE)
                   .data(messageEndpoint + "?sessionId=" + sessionId);
             }
             catch (Exception e) {
                logger.error("Failed to send initial endpoint event: {}", e.getMessage());
                sseBuilder.error(e);
             }
          }, Duration.ZERO);
       }
       catch (Exception e) {
          logger.error("Failed to send initial endpoint event to session {}: {}", sessionId, e.getMessage());
          sessions.remove(sessionId);
          return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
       }
    }

    private ServerResponse handleMessage(ServerRequest request) {
       if (this.isClosing) {
          return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).body("Server is shutting down");
       }

       if (!request.param("sessionId").isPresent()) {
          return ServerResponse.badRequest().body(new McpError("Session ID missing in message endpoint"));
       }

       String sessionId = request.param("sessionId").get();
       McpServerSession session = sessions.get(sessionId);

       if (session == null) {
          return ServerResponse.status(HttpStatus.NOT_FOUND).body(new McpError("Session not found: " + sessionId));
       }

       try {
          String body = request.body(String.class);
          McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper, body);

          // Process the message through the session's handle method
          session.handle(message).block(); // Block for WebMVC compatibility

          return ServerResponse.ok().build();
       }
       catch (IllegalArgumentException | IOException e) {
          logger.error("Failed to deserialize message: {}", e.getMessage());
          return ServerResponse.badRequest().body(new McpError("Invalid message format"));
       }
       catch (Exception e) {
          logger.error("Error handling message: {}", e.getMessage());
          return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new McpError(e.getMessage()));
       }
    }
}
```

### MCPSeesion（会话层）

处理客户端与服务器之间的通信，支持请求-响应、通知等两种模式，支持同步、异步的会话管理

- sendRequest：向模型端发送请求，返回指定类型的响应
- sendNotification：向客户端或服务端发送通知，适用于不需要响应的通知模式
- closeGracefully：异步关闭会话并释放资源
- close：同步关闭会话并释放相关资源

```java
public interface McpSession {

    <T> Mono<T> sendRequest(String method, Object requestParams, TypeReference<T> typeRef);

    default Mono<Void> sendNotification(String method) {
       return sendNotification(method, null);
    }

    Mono<Void> sendNotification(String method, Map<String, Object> params);
    
    Mono<Void> closeGracefully();

    void close();

}
```

#### McpClientSession

客户端会话实现类，负责管理与服务端之间的双向JSON-RPC通信，提供了如下核心功能

- 管理通信：处理客户端与服务器之间的请求，响应和通知
- 异步操作：基于Project Reactor的Mono实现非阻塞通信
- 会话管理：支持会话的创建、关闭（优雅关闭、立即关闭）
- 消息处理：管理请求的响应超时、消息的路由和错误处理

内部类的作用：

- RequestHandler内部接口类：处理传入的JSON-RPC请求
- NotificationHandler内部接口类：处理传入的JSON-RPC通知

构造器部分的作用

1. 初始化成员变量：RequestHandler、NotificationHandler的实例对象复制到内部的ConcurrentHashMap中
2. 建立传输层连接：通过mono.doOnNext方法注册消息处理器
3. 消息处理逻辑：根据接收到的消息类型，调用不同的处理器
4. 订阅连接：通过subscribe方法订阅连接，启动消息的接收和处理流程

```java
public class McpClientSession implements McpSession {

    
    private static final Logger logger = LoggerFactory.getLogger(McpClientSession.class);

    
    private final Duration requestTimeout;

    
    private final McpClientTransport transport;

    
    private final ConcurrentHashMap<Object, MonoSink<McpSchema.JSONRPCResponse>> pendingResponses = new ConcurrentHashMap<>();

    
    private final ConcurrentHashMap<String, RequestHandler<?>> requestHandlers = new ConcurrentHashMap<>();

    
    private final ConcurrentHashMap<String, NotificationHandler> notificationHandlers = new ConcurrentHashMap<>();

    
    private final String sessionPrefix = UUID.randomUUID().toString().substring(0, 8);

    
    private final AtomicLong requestCounter = new AtomicLong(0);

    private final Disposable connection;

    @FunctionalInterface
    public interface RequestHandler<T> {

       Mono<T> handle(Object params);

    }

    @FunctionalInterface
    public interface NotificationHandler {

       Mono<Void> handle(Object params);

    }

    public McpClientSession(Duration requestTimeout, McpClientTransport transport,
          Map<String, RequestHandler<?>> requestHandlers, Map<String, NotificationHandler> notificationHandlers) {

       Assert.notNull(requestTimeout, "The requstTimeout can not be null");
       Assert.notNull(transport, "The transport can not be null");
       Assert.notNull(requestHandlers, "The requestHandlers can not be null");
       Assert.notNull(notificationHandlers, "The notificationHandlers can not be null");

       this.requestTimeout = requestTimeout;
       this.transport = transport;
       this.requestHandlers.putAll(requestHandlers);
       this.notificationHandlers.putAll(notificationHandlers);

       this.connection = this.transport.connect(mono -> mono.doOnNext(message -> {
          if (message instanceof McpSchema.JSONRPCResponse response) {
             logger.debug("Received Response: {}", response);
             var sink = pendingResponses.remove(response.id());
             if (sink == null) {
                logger.warn("Unexpected response for unkown id {}", response.id());
             }
             else {
                sink.success(response);
             }
          }
          else if (message instanceof McpSchema.JSONRPCRequest request) {
             logger.debug("Received request: {}", request);
             handleIncomingRequest(request).subscribe(response -> transport.sendMessage(response).subscribe(),
                   error -> {
                      var errorResponse = new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.id(),
                            null, new McpSchema.JSONRPCResponse.JSONRPCError(
                                  McpSchema.ErrorCodes.INTERNAL_ERROR, error.getMessage(), null));
                      transport.sendMessage(errorResponse).subscribe();
                   });
          }
          else if (message instanceof McpSchema.JSONRPCNotification notification) {
             logger.debug("Received notification: {}", notification);
             handleIncomingNotification(notification).subscribe(null,
                   error -> logger.error("Error handling notification: {}", error.getMessage()));
          }
       })).subscribe();
    }

    private Mono<McpSchema.JSONRPCResponse> handleIncomingRequest(McpSchema.JSONRPCRequest request) {
       return Mono.defer(() -> {
          var handler = this.requestHandlers.get(request.method());
          if (handler == null) {
             MethodNotFoundError error = getMethodNotFoundError(request.method());
             return Mono.just(new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.id(), null,
                   new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.METHOD_NOT_FOUND,
                         error.message(), error.data())));
          }

          return handler.handle(request.params())
             .map(result -> new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.id(), result, null))
             .onErrorResume(error -> Mono.just(new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.id(),
                   null, new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INTERNAL_ERROR,
                         error.getMessage(), null)))); // TODO: add error message
                                                 // through the data field
       });
    }

    record MethodNotFoundError(String method, String message, Object data) {
    }

    public static MethodNotFoundError getMethodNotFoundError(String method) {
       switch (method) {
          case McpSchema.METHOD_ROOTS_LIST:
             return new MethodNotFoundError(method, "Roots not supported",
                   Map.of("reason", "Client does not have roots capability"));
          default:
             return new MethodNotFoundError(method, "Method not found: " + method, null);
       }
    }

    private Mono<Void> handleIncomingNotification(McpSchema.JSONRPCNotification notification) {
       return Mono.defer(() -> {
          var handler = notificationHandlers.get(notification.method());
          if (handler == null) {
             logger.error("No handler registered for notification method: {}", notification.method());
             return Mono.empty();
          }
          return handler.handle(notification.params());
       });
    }

    private String generateRequestId() {
       return this.sessionPrefix + "-" + this.requestCounter.getAndIncrement();
    }
    
    @Override
    public <T> Mono<T> sendRequest(String method, Object requestParams, TypeReference<T> typeRef) {
       String requestId = this.generateRequestId();

       return Mono.<McpSchema.JSONRPCResponse>create(sink -> {
          this.pendingResponses.put(requestId, sink);
          McpSchema.JSONRPCRequest jsonrpcRequest = new McpSchema.JSONRPCRequest(McpSchema.JSONRPC_VERSION, method,
                requestId, requestParams);
          this.transport.sendMessage(jsonrpcRequest)
             // TODO: It's most efficient to create a dedicated Subscriber here
             .subscribe(v -> {
             }, error -> {
                this.pendingResponses.remove(requestId);
                sink.error(error);
             });
       }).timeout(this.requestTimeout).handle((jsonRpcResponse, sink) -> {
          if (jsonRpcResponse.error() != null) {
             sink.error(new McpError(jsonRpcResponse.error()));
          }
          else {
             if (typeRef.getType().equals(Void.class)) {
                sink.complete();
             }
             else {
                sink.next(this.transport.unmarshalFrom(jsonRpcResponse.result(), typeRef));
             }
          }
       });
    }

    @Override
    public Mono<Void> sendNotification(String method, Map<String, Object> params) {
       McpSchema.JSONRPCNotification jsonrpcNotification = new McpSchema.JSONRPCNotification(McpSchema.JSONRPC_VERSION,
             method, params);
       return this.transport.sendMessage(jsonrpcNotification);
    }

    @Override
    public Mono<Void> closeGracefully() {
       return Mono.defer(() -> {
          this.connection.dispose();
          return transport.closeGracefully();
       });
    }

    @Override
    public void close() {
       this.connection.dispose();
       transport.close();
    }

}
```

#### McpServerSession

服务端会话管理类，负责管理与客户端的双向JSON-RPC通信，提供了如下核心功能

- 管理会话周期：会话的初始化、消息处理、关闭等
- 处理JSON-RPC消息：请求、响应和通知的处理
- 维护会话状态：通过状态机管理会话的初始化状态（未初始化0、初始化中1、已初始化2）
- 与传输层交互：通过McpServerServerTransport与客户端进行消息的发送和接收

内部类的作用：

- InitRequestHandler：初始化客户端的请求处理
- InitNotificationHandler：初始化客户端的通知处理
- RequestHandler：客户端请求的处理
- Factory：创建McpServerSession实例

对外暴露的方法，用于与客户端进行交互和管理会话

- McpServerSession构造器：初始化会话。接收会话id、服务端传输层、初始化处理器、请求处理器、通知处理器
- getId：当前会话唯一标识符
- init：在客户端和服务器成功初始化后调用，设置客户端的能力和信息
- handle：处理来自客户端的JSON-RPC消息，根据消息类型调用相应的处理器

```java
public class McpServerSession implements McpSession {

    private static final Logger logger = LoggerFactory.getLogger(McpServerSession.class);

    private final ConcurrentHashMap<Object, MonoSink<McpSchema.JSONRPCResponse>> pendingResponses = new ConcurrentHashMap<>();

    private final String id;

    private final AtomicLong requestCounter = new AtomicLong(0);

    private final InitRequestHandler initRequestHandler;

    private final InitNotificationHandler initNotificationHandler;

    private final Map<String, RequestHandler<?>> requestHandlers;

    private final Map<String, NotificationHandler> notificationHandlers;

    private final McpServerTransport transport;

    private final Sinks.One<McpAsyncServerExchange> exchangeSink = Sinks.one();

    private final AtomicReference<McpSchema.ClientCapabilities> clientCapabilities = new AtomicReference<>();

    private final AtomicReference<McpSchema.Implementation> clientInfo = new AtomicReference<>();

    private static final int STATE_UNINITIALIZED = 0;

    private static final int STATE_INITIALIZING = 1;

    private static final int STATE_INITIALIZED = 2;

    private final AtomicInteger state = new AtomicInteger(STATE_UNINITIALIZED);

    public McpServerSession(String id, McpServerTransport transport, InitRequestHandler initHandler,
          InitNotificationHandler initNotificationHandler, Map<String, RequestHandler<?>> requestHandlers,
          Map<String, NotificationHandler> notificationHandlers) {
       this.id = id;
       this.transport = transport;
       this.initRequestHandler = initHandler;
       this.initNotificationHandler = initNotificationHandler;
       this.requestHandlers = requestHandlers;
       this.notificationHandlers = notificationHandlers;
    }

    public String getId() {
       return this.id;
    }

    public void init(McpSchema.ClientCapabilities clientCapabilities, McpSchema.Implementation clientInfo) {
       this.clientCapabilities.lazySet(clientCapabilities);
       this.clientInfo.lazySet(clientInfo);
    }

    private String generateRequestId() {
       return this.id + "-" + this.requestCounter.getAndIncrement();
    }

    @Override
    public <T> Mono<T> sendRequest(String method, Object requestParams, TypeReference<T> typeRef) {
       String requestId = this.generateRequestId();

       return Mono.<McpSchema.JSONRPCResponse>create(sink -> {
          this.pendingResponses.put(requestId, sink);
          McpSchema.JSONRPCRequest jsonrpcRequest = new McpSchema.JSONRPCRequest(McpSchema.JSONRPC_VERSION, method,
                requestId, requestParams);
          this.transport.sendMessage(jsonrpcRequest).subscribe(v -> {
          }, error -> {
             this.pendingResponses.remove(requestId);
             sink.error(error);
          });
       }).timeout(Duration.ofSeconds(10)).handle((jsonRpcResponse, sink) -> {
          if (jsonRpcResponse.error() != null) {
             sink.error(new McpError(jsonRpcResponse.error()));
          }
          else {
             if (typeRef.getType().equals(Void.class)) {
                sink.complete();
             }
             else {
                sink.next(this.transport.unmarshalFrom(jsonRpcResponse.result(), typeRef));
             }
          }
       });
    }

    @Override
    public Mono<Void> sendNotification(String method, Map<String, Object> params) {
       McpSchema.JSONRPCNotification jsonrpcNotification = new McpSchema.JSONRPCNotification(McpSchema.JSONRPC_VERSION,
             method, params);
       return this.transport.sendMessage(jsonrpcNotification);
    }
    
    public Mono<Void> handle(McpSchema.JSONRPCMessage message) {
       return Mono.defer(() -> {
          // TODO handle errors for communication to without initialization happening
          // first
          if (message instanceof McpSchema.JSONRPCResponse response) {
             logger.debug("Received Response: {}", response);
             var sink = pendingResponses.remove(response.id());
             if (sink == null) {
                logger.warn("Unexpected response for unknown id {}", response.id());
             }
             else {
                sink.success(response);
             }
             return Mono.empty();
          }
          else if (message instanceof McpSchema.JSONRPCRequest request) {
             logger.debug("Received request: {}", request);
             return handleIncomingRequest(request).onErrorResume(error -> {
                var errorResponse = new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.id(), null,
                      new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INTERNAL_ERROR,
                            error.getMessage(), null));
                // TODO: Should the error go to SSE or back as POST return?
                return this.transport.sendMessage(errorResponse).then(Mono.empty());
             }).flatMap(this.transport::sendMessage);
          }
          else if (message instanceof McpSchema.JSONRPCNotification notification) {
             // TODO handle errors for communication to without initialization
             // happening first
             logger.debug("Received notification: {}", notification);
             // TODO: in case of error, should the POST request be signalled?
             return handleIncomingNotification(notification)
                .doOnError(error -> logger.error("Error handling notification: {}", error.getMessage()));
          }
          else {
             logger.warn("Received unknown message type: {}", message);
             return Mono.empty();
          }
       });
    }

    private Mono<McpSchema.JSONRPCResponse> handleIncomingRequest(McpSchema.JSONRPCRequest request) {
       return Mono.defer(() -> {
          Mono<?> resultMono;
          if (McpSchema.METHOD_INITIALIZE.equals(request.method())) {
             // TODO handle situation where already initialized!
             McpSchema.InitializeRequest initializeRequest = transport.unmarshalFrom(request.params(),
                   new TypeReference<McpSchema.InitializeRequest>() {
                   });

             this.state.lazySet(STATE_INITIALIZING);
             this.init(initializeRequest.capabilities(), initializeRequest.clientInfo());
             resultMono = this.initRequestHandler.handle(initializeRequest);
          }
          else {
             // TODO handle errors for communication to this session without
             // initialization happening first
             var handler = this.requestHandlers.get(request.method());
             if (handler == null) {
                MethodNotFoundError error = getMethodNotFoundError(request.method());
                return Mono.just(new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.id(), null,
                      new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.METHOD_NOT_FOUND,
                            error.message(), error.data())));
             }

             resultMono = this.exchangeSink.asMono().flatMap(exchange -> handler.handle(exchange, request.params()));
          }
          return resultMono
             .map(result -> new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.id(), result, null))
             .onErrorResume(error -> Mono.just(new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.id(),
                   null, new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INTERNAL_ERROR,
                         error.getMessage(), null)))); // TODO: add error message
                                                 // through the data field
       });
    }

    private Mono<Void> handleIncomingNotification(McpSchema.JSONRPCNotification notification) {
       return Mono.defer(() -> {
          if (McpSchema.METHOD_NOTIFICATION_INITIALIZED.equals(notification.method())) {
             this.state.lazySet(STATE_INITIALIZED);
             exchangeSink.tryEmitValue(new McpAsyncServerExchange(this, clientCapabilities.get(), clientInfo.get()));
             return this.initNotificationHandler.handle();
          }

          var handler = notificationHandlers.get(notification.method());
          if (handler == null) {
             logger.error("No handler registered for notification method: {}", notification.method());
             return Mono.empty();
          }
          return this.exchangeSink.asMono().flatMap(exchange -> handler.handle(exchange, notification.params()));
       });
    }

    record MethodNotFoundError(String method, String message, Object data) {
    }

    static MethodNotFoundError getMethodNotFoundError(String method) {
       switch (method) {
          case McpSchema.METHOD_ROOTS_LIST:
             return new MethodNotFoundError(method, "Roots not supported",
                   Map.of("reason", "Client does not have roots capability"));
          default:
             return new MethodNotFoundError(method, "Method not found: " + method, null);
       }
    }

    @Override
    public Mono<Void> closeGracefully() {
       return this.transport.closeGracefully();
    }

    @Override
    public void close() {
       this.transport.close();
    }

    public interface InitRequestHandler {

       Mono<McpSchema.InitializeResult> handle(McpSchema.InitializeRequest initializeRequest);

    }

    public interface InitNotificationHandler {

       Mono<Void> handle();

    }

    public interface NotificationHandler {

       Mono<Void> handle(McpAsyncServerExchange exchange, Object params);

    }

    public interface RequestHandler<T> {
       Mono<T> handle(McpAsyncServerExchange exchange, Object params);
    }

    @FunctionalInterface
    public interface Factory {

       McpServerSession create(McpServerTransport sessionTransport);

    }

}
```

### McpSchema（数据结构定义类）

MCP的核心数据结构定义类，它基于JSON-RPC2.0规范，并扩展MCP协议所需的特定消息类型、错误码、方法名称等，主要做了以下事情：

1. 协议版本管理：定义MCP协议、JSON-RPC的版本
2. 方法名称定义：定义了MCP协议中使用的各种方法名称，标识客户端与服务器之间的通信操作
3. 错误码定义：定义标准了JSON-RPC错误码，用于处理通信中的错误情况
4. 消息类型定义：定义MCP协议中使用的消息类型，包括请求（JSONRPCRequest）、通知（JSONRPCNotification）、响应（JSONRPCResponse）
5. 功能模块数据结构：定义MCP协议中各个功能模块的数据结构，如资源管理、提示词管理、工具管理、采样管理等

```java
public final class McpSchema {

    private static final Logger logger = LoggerFactory.getLogger(McpSchema.class);

    private McpSchema() {
    }

    public static final String LATEST_PROTOCOL_VERSION = "2024-11-05";

    public static final String JSONRPC_VERSION = "2.0";

    // ---------------------------
    // Method Names
    // ---------------------------

    // Lifecycle Methods
    public static final String METHOD_INITIALIZE = "initialize";

    public static final String METHOD_NOTIFICATION_INITIALIZED = "notifications/initialized";

    public static final String METHOD_PING = "ping";

    // Tool Methods
    public static final String METHOD_TOOLS_LIST = "tools/list";

    public static final String METHOD_TOOLS_CALL = "tools/call";

    public static final String METHOD_NOTIFICATION_TOOLS_LIST_CHANGED = "notifications/tools/list_changed";

    // Resources Methods
    public static final String METHOD_RESOURCES_LIST = "resources/list";

    public static final String METHOD_RESOURCES_READ = "resources/read";

    public static final String METHOD_NOTIFICATION_RESOURCES_LIST_CHANGED = "notifications/resources/list_changed";

    public static final String METHOD_RESOURCES_TEMPLATES_LIST = "resources/templates/list";

    public static final String METHOD_RESOURCES_SUBSCRIBE = "resources/subscribe";

    public static final String METHOD_RESOURCES_UNSUBSCRIBE = "resources/unsubscribe";

    // Prompt Methods
    public static final String METHOD_PROMPT_LIST = "prompts/list";

    public static final String METHOD_PROMPT_GET = "prompts/get";

    public static final String METHOD_NOTIFICATION_PROMPTS_LIST_CHANGED = "notifications/prompts/list_changed";

    // Logging Methods
    public static final String METHOD_LOGGING_SET_LEVEL = "logging/setLevel";

    public static final String METHOD_NOTIFICATION_MESSAGE = "notifications/message";

    // Roots Methods
    public static final String METHOD_ROOTS_LIST = "roots/list";

    public static final String METHOD_NOTIFICATION_ROOTS_LIST_CHANGED = "notifications/roots/list_changed";

    // Sampling Methods
    public static final String METHOD_SAMPLING_CREATE_MESSAGE = "sampling/createMessage";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // ---------------------------
    // JSON-RPC Error Codes
    // ---------------------------
    
    public static final class ErrorCodes {

       
       public static final int PARSE_ERROR = -32700;

       
       public static final int INVALID_REQUEST = -32600;

       
       public static final int METHOD_NOT_FOUND = -32601;

       
       public static final int INVALID_PARAMS = -32602;

       
       public static final int INTERNAL_ERROR = -32603;

    }

    public sealed interface Request
          permits InitializeRequest, CallToolRequest, CreateMessageRequest, CompleteRequest, GetPromptRequest {

    }

    private static final TypeReference<HashMap<String, Object>> MAP_TYPE_REF = new TypeReference<>() {
    };

    public static JSONRPCMessage deserializeJsonRpcMessage(ObjectMapper objectMapper, String jsonText)
          throws IOException {

       logger.debug("Received JSON message: {}", jsonText);

       var map = objectMapper.readValue(jsonText, MAP_TYPE_REF);

       // Determine message type based on specific JSON structure
       if (map.containsKey("method") && map.containsKey("id")) {
          return objectMapper.convertValue(map, JSONRPCRequest.class);
       }
       else if (map.containsKey("method") && !map.containsKey("id")) {
          return objectMapper.convertValue(map, JSONRPCNotification.class);
       }
       else if (map.containsKey("result") || map.containsKey("error")) {
          return objectMapper.convertValue(map, JSONRPCResponse.class);
       }

       throw new IllegalArgumentException("Cannot deserialize JSONRPCMessage: " + jsonText);
    }

    // ---------------------------
    // JSON-RPC Message Types
    // ---------------------------
    public sealed interface JSONRPCMessage permits JSONRPCRequest, JSONRPCNotification, JSONRPCResponse {

       String jsonrpc();

    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JSONRPCRequest( // @formatter:off
          @JsonProperty("jsonrpc") String jsonrpc,
          @JsonProperty("method") String method,
          @JsonProperty("id") Object id,
          @JsonProperty("params") Object params) implements JSONRPCMessage {
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JSONRPCNotification( // @formatter:off
          @JsonProperty("jsonrpc") String jsonrpc,
          @JsonProperty("method") String method,
          @JsonProperty("params") Map<String, Object> params) implements JSONRPCMessage {
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JSONRPCResponse( // @formatter:off
          @JsonProperty("jsonrpc") String jsonrpc,
          @JsonProperty("id") Object id,
          @JsonProperty("result") Object result,
          @JsonProperty("error") JSONRPCError error) implements JSONRPCMessage {

       @JsonInclude(JsonInclude.Include.NON_ABSENT)
       @JsonIgnoreProperties(ignoreUnknown = true)
       public record JSONRPCError(
          @JsonProperty("code") int code,
          @JsonProperty("message") String message,
          @JsonProperty("data") Object data) {
       }
    }// @formatter:on

    // ---------------------------
    // Initialization
    // ---------------------------
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InitializeRequest( // @formatter:off
       @JsonProperty("protocolVersion") String protocolVersion,
       @JsonProperty("capabilities") ClientCapabilities capabilities,
       @JsonProperty("clientInfo") Implementation clientInfo) implements Request {       
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InitializeResult( // @formatter:off
       @JsonProperty("protocolVersion") String protocolVersion,
       @JsonProperty("capabilities") ServerCapabilities capabilities,
       @JsonProperty("serverInfo") Implementation serverInfo,
       @JsonProperty("instructions") String instructions) {
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ClientCapabilities( // @formatter:off
       @JsonProperty("experimental") Map<String, Object> experimental,
       @JsonProperty("roots") RootCapabilities roots,
       @JsonProperty("sampling") Sampling sampling) {

       @JsonInclude(JsonInclude.Include.NON_ABSENT)
       @JsonIgnoreProperties(ignoreUnknown = true)    
       public record RootCapabilities(
          @JsonProperty("listChanged") Boolean listChanged) {
       }

       @JsonInclude(JsonInclude.Include.NON_ABSENT)         
       public record Sampling() {
       }

       public static Builder builder() {
          return new Builder();
       }

       public static class Builder {
          private Map<String, Object> experimental;
          private RootCapabilities roots;
          private Sampling sampling;

          public Builder experimental(Map<String, Object> experimental) {
             this.experimental = experimental;
             return this;
          }

          public Builder roots(Boolean listChanged) {
             this.roots = new RootCapabilities(listChanged);
             return this;
          }

          public Builder sampling() {
             this.sampling = new Sampling();
             return this;
          }

          public ClientCapabilities build() {
             return new ClientCapabilities(experimental, roots, sampling);
          }
       }
    }// @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ServerCapabilities( // @formatter:off
       @JsonProperty("experimental") Map<String, Object> experimental,
       @JsonProperty("logging") LoggingCapabilities logging,
       @JsonProperty("prompts") PromptCapabilities prompts,
       @JsonProperty("resources") ResourceCapabilities resources,
       @JsonProperty("tools") ToolCapabilities tools) {

          
       @JsonInclude(JsonInclude.Include.NON_ABSENT)
       public record LoggingCapabilities() {
       }
    
       @JsonInclude(JsonInclude.Include.NON_ABSENT)
       public record PromptCapabilities(
          @JsonProperty("listChanged") Boolean listChanged) {
       }

       @JsonInclude(JsonInclude.Include.NON_ABSENT)
       public record ResourceCapabilities(
          @JsonProperty("subscribe") Boolean subscribe,
          @JsonProperty("listChanged") Boolean listChanged) {
       }

       @JsonInclude(JsonInclude.Include.NON_ABSENT)
       public record ToolCapabilities(
          @JsonProperty("listChanged") Boolean listChanged) {
       }

       public static Builder builder() {
          return new Builder();
       }

       public static class Builder {

          private Map<String, Object> experimental;
          private LoggingCapabilities logging = new LoggingCapabilities();
          private PromptCapabilities prompts;
          private ResourceCapabilities resources;
          private ToolCapabilities tools;

          public Builder experimental(Map<String, Object> experimental) {
             this.experimental = experimental;
             return this;
          }

          public Builder logging() {
             this.logging = new LoggingCapabilities();
             return this;
          }

          public Builder prompts(Boolean listChanged) {
             this.prompts = new PromptCapabilities(listChanged);
             return this;
          }

          public Builder resources(Boolean subscribe, Boolean listChanged) {
             this.resources = new ResourceCapabilities(subscribe, listChanged);
             return this;
          }

          public Builder tools(Boolean listChanged) {
             this.tools = new ToolCapabilities(listChanged);
             return this;
          }

          public ServerCapabilities build() {
             return new ServerCapabilities(experimental, logging, prompts, resources, tools);
          }
       }
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Implementation(// @formatter:off
       @JsonProperty("name") String name,
       @JsonProperty("version") String version) {
    } // @formatter:on

    // Existing Enums and Base Types (from previous implementation)
    public enum Role {// @formatter:off

       @JsonProperty("user") USER,
       @JsonProperty("assistant") ASSISTANT
    }// @formatter:on

    // ---------------------------
    // Resource Interfaces
    // ---------------------------
    
    public interface Annotated {

       Annotations annotations();

    }

    
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Annotations( // @formatter:off
       @JsonProperty("audience") List<Role> audience,
       @JsonProperty("priority") Double priority) {
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Resource( // @formatter:off
       @JsonProperty("uri") String uri,
       @JsonProperty("name") String name,
       @JsonProperty("description") String description,
       @JsonProperty("mimeType") String mimeType,
       @JsonProperty("annotations") Annotations annotations) implements Annotated {
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResourceTemplate( // @formatter:off
       @JsonProperty("uriTemplate") String uriTemplate,
       @JsonProperty("name") String name,
       @JsonProperty("description") String description,
       @JsonProperty("mimeType") String mimeType,
       @JsonProperty("annotations") Annotations annotations) implements Annotated {
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ListResourcesResult( // @formatter:off
       @JsonProperty("resources") List<Resource> resources,
       @JsonProperty("nextCursor") String nextCursor) {
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ListResourceTemplatesResult( // @formatter:off
       @JsonProperty("resourceTemplates") List<ResourceTemplate> resourceTemplates,
       @JsonProperty("nextCursor") String nextCursor) {
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReadResourceRequest( // @formatter:off
       @JsonProperty("uri") String uri){
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReadResourceResult( // @formatter:off
       @JsonProperty("contents") List<ResourceContents> contents){
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SubscribeRequest( // @formatter:off
       @JsonProperty("uri") String uri){
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UnsubscribeRequest( // @formatter:off
       @JsonProperty("uri") String uri){
    } // @formatter:on

    
    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION, include = As.PROPERTY)
    @JsonSubTypes({ @JsonSubTypes.Type(value = TextResourceContents.class, name = "text"),
          @JsonSubTypes.Type(value = BlobResourceContents.class, name = "blob") })
    public sealed interface ResourceContents permits TextResourceContents, BlobResourceContents {

       
       String uri();

       
       String mimeType();

    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TextResourceContents( // @formatter:off
       @JsonProperty("uri") String uri,
       @JsonProperty("mimeType") String mimeType,
       @JsonProperty("text") String text) implements ResourceContents {
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BlobResourceContents( // @formatter:off
       @JsonProperty("uri") String uri,
       @JsonProperty("mimeType") String mimeType,
       @JsonProperty("blob") String blob) implements ResourceContents {
    } // @formatter:on

    // ---------------------------
    // Prompt Interfaces
    // ---------------------------
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Prompt( // @formatter:off
       @JsonProperty("name") String name,
       @JsonProperty("description") String description,
       @JsonProperty("arguments") List<PromptArgument> arguments) {
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PromptArgument( // @formatter:off
       @JsonProperty("name") String name,
       @JsonProperty("description") String description,
       @JsonProperty("required") Boolean required) {
    }// @formatter:on
    
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PromptMessage( // @formatter:off
       @JsonProperty("role") Role role,
       @JsonProperty("content") Content content) {
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ListPromptsResult( // @formatter:off
       @JsonProperty("prompts") List<Prompt> prompts,
       @JsonProperty("nextCursor") String nextCursor) {
    }// @formatter:on


    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GetPromptRequest(// @formatter:off
       @JsonProperty("name") String name,
       @JsonProperty("arguments") Map<String, Object> arguments) implements Request {
    }// @formatter:off

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GetPromptResult( // @formatter:off
       @JsonProperty("description") String description,
       @JsonProperty("messages") List<PromptMessage> messages) {
    } // @formatter:on

    // ---------------------------
    // Tool Interfaces
    // ---------------------------
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ListToolsResult( // @formatter:off
       @JsonProperty("tools") List<Tool> tools,
       @JsonProperty("nextCursor") String nextCursor) {
    }// @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JsonSchema( // @formatter:off
       @JsonProperty("type") String type,
       @JsonProperty("properties") Map<String, Object> properties,
       @JsonProperty("required") List<String> required,
       @JsonProperty("additionalProperties") Boolean additionalProperties) {
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Tool( // @formatter:off
       @JsonProperty("name") String name,
       @JsonProperty("description") String description,
       @JsonProperty("inputSchema") JsonSchema inputSchema) {
    
       public Tool(String name, String description, String schema) {
          this(name, description, parseSchema(schema));
       }
          
    } // @formatter:on

    private static JsonSchema parseSchema(String schema) {
       try {
          return OBJECT_MAPPER.readValue(schema, JsonSchema.class);
       }
       catch (IOException e) {
          throw new IllegalArgumentException("Invalid schema: " + schema, e);
       }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CallToolRequest(// @formatter:off
       @JsonProperty("name") String name,
       @JsonProperty("arguments") Map<String, Object> arguments) implements Request {
    }// @formatter:off


    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CallToolResult( // @formatter:off
       @JsonProperty("content") List<Content> content,
       @JsonProperty("isError") Boolean isError) {
    } // @formatter:on

    // ---------------------------
    // Sampling Interfaces
    // ---------------------------
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ModelPreferences(// @formatter:off
    @JsonProperty("hints") List<ModelHint> hints,
    @JsonProperty("costPriority") Double costPriority,
    @JsonProperty("speedPriority") Double speedPriority,
    @JsonProperty("intelligencePriority") Double intelligencePriority) {
    
    public static Builder builder() {
       return new Builder();
    }

    public static class Builder {
       private List<ModelHint> hints;
       private Double costPriority;
       private Double speedPriority;
       private Double intelligencePriority;

       public Builder hints(List<ModelHint> hints) {
          this.hints = hints;
          return this;
       }

       public Builder addHint(String name) {
          if (this.hints == null) {
             this.hints = new ArrayList<>();
          }
          this.hints.add(new ModelHint(name));
          return this;
       }

       public Builder costPriority(Double costPriority) {
          this.costPriority = costPriority;
          return this;
       }

       public Builder speedPriority(Double speedPriority) {
          this.speedPriority = speedPriority;
          return this;
       }

       public Builder intelligencePriority(Double intelligencePriority) {
          this.intelligencePriority = intelligencePriority;
          return this;
       }

       public ModelPreferences build() {
          return new ModelPreferences(hints, costPriority, speedPriority, intelligencePriority);
       }
    }
} // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ModelHint(@JsonProperty("name") String name) {
       public static ModelHint of(String name) {
          return new ModelHint(name);
       }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SamplingMessage(// @formatter:off
       @JsonProperty("role") Role role,
       @JsonProperty("content") Content content) {
    } // @formatter:on

    // Sampling and Message Creation
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CreateMessageRequest(// @formatter:off
       @JsonProperty("messages") List<SamplingMessage> messages,
       @JsonProperty("modelPreferences") ModelPreferences modelPreferences,
       @JsonProperty("systemPrompt") String systemPrompt,
       @JsonProperty("includeContext") ContextInclusionStrategy includeContext,
       @JsonProperty("temperature") Double temperature,
       @JsonProperty("maxTokens") int maxTokens,
       @JsonProperty("stopSequences") List<String> stopSequences,           
       @JsonProperty("metadata") Map<String, Object> metadata) implements Request {

       public enum ContextInclusionStrategy {
          @JsonProperty("none") NONE,
          @JsonProperty("thisServer") THIS_SERVER,
          @JsonProperty("allServers") ALL_SERVERS
       }
       
       public static Builder builder() {
          return new Builder();
       }

       public static class Builder {
          private List<SamplingMessage> messages;
          private ModelPreferences modelPreferences;
          private String systemPrompt;
          private ContextInclusionStrategy includeContext;
          private Double temperature;
          private int maxTokens;
          private List<String> stopSequences;
          private Map<String, Object> metadata;

          public Builder messages(List<SamplingMessage> messages) {
             this.messages = messages;
             return this;
          }

          public Builder modelPreferences(ModelPreferences modelPreferences) {
             this.modelPreferences = modelPreferences;
             return this;
          }

          public Builder systemPrompt(String systemPrompt) {
             this.systemPrompt = systemPrompt;
             return this;
          }

          public Builder includeContext(ContextInclusionStrategy includeContext) {
             this.includeContext = includeContext;
             return this;
          }

          public Builder temperature(Double temperature) {
             this.temperature = temperature;
             return this;
          }

          public Builder maxTokens(int maxTokens) {
             this.maxTokens = maxTokens;
             return this;
          }

          public Builder stopSequences(List<String> stopSequences) {
             this.stopSequences = stopSequences;
             return this;
          }

          public Builder metadata(Map<String, Object> metadata) {
             this.metadata = metadata;
             return this;
          }

          public CreateMessageRequest build() {
             return new CreateMessageRequest(messages, modelPreferences, systemPrompt,
                includeContext, temperature, maxTokens, stopSequences, metadata);
          }
       }
    }// @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CreateMessageResult(// @formatter:off
       @JsonProperty("role") Role role,
       @JsonProperty("content") Content content,
       @JsonProperty("model") String model,
       @JsonProperty("stopReason") StopReason stopReason) {
       
       public enum StopReason {
          @JsonProperty("endTurn") END_TURN,
          @JsonProperty("stopSequence") STOP_SEQUENCE,
          @JsonProperty("maxTokens") MAX_TOKENS
       }

       public static Builder builder() {
          return new Builder();
       }

       public static class Builder {
          private Role role = Role.ASSISTANT;
          private Content content;
          private String model;
          private StopReason stopReason = StopReason.END_TURN;

          public Builder role(Role role) {
             this.role = role;
             return this;
          }

          public Builder content(Content content) {
             this.content = content;
             return this;
          }

          public Builder model(String model) {
             this.model = model;
             return this;
          }

          public Builder stopReason(StopReason stopReason) {
             this.stopReason = stopReason;
             return this;
          }

          public Builder message(String message) {
             this.content = new TextContent(message);
             return this;
          }

          public CreateMessageResult build() {
             return new CreateMessageResult(role, content, model, stopReason);
          }
       }
    }// @formatter:on

    // ---------------------------
    // Pagination Interfaces
    // ---------------------------
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaginatedRequest(@JsonProperty("cursor") String cursor) {
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaginatedResult(@JsonProperty("nextCursor") String nextCursor) {
    }

    // ---------------------------
    // Progress and Logging
    // ---------------------------
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProgressNotification(// @formatter:off
       @JsonProperty("progressToken") String progressToken,
       @JsonProperty("progress") double progress,
       @JsonProperty("total") Double total) {
    }// @formatter:on

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LoggingMessageNotification(// @formatter:off
       @JsonProperty("level") LoggingLevel level,
       @JsonProperty("logger") String logger,
       @JsonProperty("data") String data) {

       public static Builder builder() {
          return new Builder();
       }

       public static class Builder {
          private LoggingLevel level = LoggingLevel.INFO;
          private String logger = "server";
          private String data;

          public Builder level(LoggingLevel level) {
             this.level = level;
             return this;
          }

          public Builder logger(String logger) {
             this.logger = logger;
             return this;
          }

          public Builder data(String data) {
             this.data = data;
             return this;
          }

          public LoggingMessageNotification build() {
             return new LoggingMessageNotification(level, logger, data);
          }
       }
    }// @formatter:on

    public enum LoggingLevel {// @formatter:off
       @JsonProperty("debug") DEBUG(0),
       @JsonProperty("info") INFO(1),
       @JsonProperty("notice") NOTICE(2),
       @JsonProperty("warning") WARNING(3),
       @JsonProperty("error") ERROR(4),
       @JsonProperty("critical") CRITICAL(5),
       @JsonProperty("alert") ALERT(6),
       @JsonProperty("emergency") EMERGENCY(7);

       private final int level;

       LoggingLevel(int level) {
          this.level = level;
       }

       public int level() {
          return level;
       }

    } // @formatter:on

    // ---------------------------
    // Autocomplete
    // ---------------------------
    public record CompleteRequest(PromptOrResourceReference ref, CompleteArgument argument) implements Request {
       public sealed interface PromptOrResourceReference permits PromptReference, ResourceReference {

          String type();

       }

       public record PromptReference(// @formatter:off
          @JsonProperty("type") String type,
          @JsonProperty("name") String name) implements PromptOrResourceReference {
       }// @formatter:on

       public record ResourceReference(// @formatter:off
          @JsonProperty("type") String type,
          @JsonProperty("uri") String uri) implements PromptOrResourceReference {
       }// @formatter:on

       public record CompleteArgument(// @formatter:off
          @JsonProperty("name") String name,
          @JsonProperty("value") String value) {
       }// @formatter:on
    }

    public record CompleteResult(CompleteCompletion completion) {
       public record CompleteCompletion(// @formatter:off
          @JsonProperty("values") List<String> values,
          @JsonProperty("total") Integer total,
          @JsonProperty("hasMore") Boolean hasMore) {
       }// @formatter:on
    }

    // ---------------------------
    // Content Types
    // ---------------------------
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({ @JsonSubTypes.Type(value = TextContent.class, name = "text"),
          @JsonSubTypes.Type(value = ImageContent.class, name = "image"),
          @JsonSubTypes.Type(value = EmbeddedResource.class, name = "resource") })
    public sealed interface Content permits TextContent, ImageContent, EmbeddedResource {

       default String type() {
          if (this instanceof TextContent) {
             return "text";
          }
          else if (this instanceof ImageContent) {
             return "image";
          }
          else if (this instanceof EmbeddedResource) {
             return "resource";
          }
          throw new IllegalArgumentException("Unknown content type: " + this);
       }

    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TextContent( // @formatter:off
       @JsonProperty("audience") List<Role> audience,
       @JsonProperty("priority") Double priority,
       @JsonProperty("text") String text) implements Content { // @formatter:on

       public TextContent(String content) {
          this(null, null, content);
       }
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ImageContent( // @formatter:off
       @JsonProperty("audience") List<Role> audience,
       @JsonProperty("priority") Double priority,
       @JsonProperty("data") String data,
       @JsonProperty("mimeType") String mimeType) implements Content { // @formatter:on
    }

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EmbeddedResource( // @formatter:off
       @JsonProperty("audience") List<Role> audience,
       @JsonProperty("priority") Double priority,
       @JsonProperty("resource") ResourceContents resource) implements Content { // @formatter:on
    }

    // ---------------------------
    // Roots
    // ---------------------------
    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Root( // @formatter:off
       @JsonProperty("uri") String uri,
       @JsonProperty("name") String name) {
    } // @formatter:on

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ListRootsResult( // @formatter:off
       @JsonProperty("roots") List<Root> roots) {
    } // @formatter:on

}
```

#### 消息类型

- JSONRPCMessage：JSON-RPC消息的基接口
- JSONRPCRequest：JSON-RPC请求消息
- JSONRPCNotification：JSON-RPC通知消息
- JSONRPCResponse：JSON-RPC响应消息

#### 错误码

- ErrorCodes：定义了标准的JSON-RPC错误码，用于在响应中标识错误类型

#### 功能模块数据结构

资源管理（Resources）

- Resource：服务器可以读取的资源
- ResourceTemplate：参数化的资源模版
- ListResourcesResult：资源列表的查询结果
- ReadResourceRequest：读取资源的请求
- ReadResourceResult：读取资源的结果

提示词管理（Prompts）

- Prompt：服务器提供的提示词或提示词模版
- ListPromptsResult：提示词列表的查询结果
- GetPromptRequest：获取提示词的请求
- GetPromptResult：获取提示词的结果

工具管理（Tools）

- Tool：表示服务器提供的工具
- ListToolsResult：工具列表的查询结果
- CallToolRequest：调用工具的请求
- CallToolResult：调用工具的结果

采样管理（Sampling）

- SamplingMessage：采样消息
- CreateMessageRequest：创建采样消息的请求
- CreateMessageResult：创建采样消息的结果

#### 客户端和服务端能力

- ClientCapabilitie：客户端的能力，如实验性功能、根目录能力和采样能力
- ServerCapabilities：服务器的能力，如日志记录、提示词和工具管理能力

#### 日志和进度

- LoggingMessageNotification：服务器发送给客户端的日志消息
- ProgressNotification：服务器发送给客户端的进度通知

#### 分页和自动补全

- PaginatedRequest：分页请求，包含游标字段
- PaginatedResult：分页结果，包含下一个游标字段
- CompleteRequest：自动补全请求
- CompleteResult：自动补全结果

#### 内容类型

- Content：消息内容的基接口，支持文本、图片和嵌入资源
- TextContent：文本内容
- ImageContent：图片内容
- EmbeddedResource：嵌入的资源内容

#### 根目录管理

- Root：服务器可以操作的根目录或文件
- ListRootsResult：根目录列表的查询结果

## MCP实战

### Stdio

#### Server

写法同下文Webflux端Server保持一致。完成后打成jar包形式

![](/img/blog/mcp-desc/stdio-jar.png)

#### Client

mcp-servers-config.json

```JSON
{
    "mcpServers": {
        "time-weather": {
            "command": "java",
            "args": [
                "-Dspring.ai.mcp.server.stdio=true",
                "-Dspring.main.web-application-type=none",
                "-Dlogging.pattern.console=",
                "-jar",
                "/Users/guotao/IdeaProjects/SpringAI-Alibaba-Quickstart/Mcp/mcp-server-stdio/target/mcp-server-stdio-1.0-SNAPSHOT.jar"
            ],
            "env": {}
        }
    }
}
```

注意填上Server端jar包的绝对路径

application.yml

```YAML
server:
  port: 8084

spring:
  application:
    name: mcp-client-stdio
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
    mcp:
      client:
        stdio:
          servers-configuration: classpath:/mcp-servers-config.json
```

启动类

```Java
@SpringBootApplication
public class ClientStdioApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientStdioApplication.class, args);
    }

    @Bean
    public CommandLineRunner predefinedQuestions(ChatClient.Builder chatClientBuilder, ToolCallbackProvider tools,
                                                 ConfigurableApplicationContext context) {

        return args -> {
            var chatClient = chatClientBuilder
                    .defaultTools(tools)
                    .build();

            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("\n>>> QUESTION: ");
                String userInput = scanner.nextLine();
                if (userInput.equalsIgnoreCase("exit")) {
                    break;
                }
                System.out.println("\n>>> ASSISTANT: " + chatClient.prompt(userInput).call().content());
            }
            scanner.close();
            context.close();
        };
    }

}
```

#### 效果展示

![](/img/blog/mcp-desc/stdio-example.png)

### Webflux

#### Server

定义时间服务

```Java
@Service
public class TimeService {

    private static final Logger logger = LoggerFactory.getLogger(TimeService.class);

    @Tool(description = "Get the time of a specified city.")
    public String  getCityTimeMethod(@ToolParam(description = "Time zone id, such as Asia/Shanghai") String timeZoneId) {
        logger.info("The current time zone is {}", timeZoneId);
        return String.format("The current time zone is %s and the current time is " + "%s", timeZoneId,
                getTimeByZoneId(timeZoneId));
    }

    private String getTimeByZoneId(String zoneId) {

        // Get the time zone using ZoneId
        ZoneId zid = ZoneId.of(zoneId);

        // Get the current time in this time zone
        ZonedDateTime zonedDateTime = ZonedDateTime.now(zid);

        // Defining a formatter
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

        // Format ZonedDateTime as a string
        String formattedDateTime = zonedDateTime.format(formatter);

        return formattedDateTime;
    }
}
```

定义天气服务

```Java
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yingzi.mcp.server.webflux.component.time.TimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;


@Service
public class OpenMeteoService {

    private static final Logger logger = LoggerFactory.getLogger(OpenMeteoService.class);


    // OpenMeteo免费天气API基础URL
    private static final String BASE_URL = "https://api.open-meteo.com/v1";

    private final RestClient restClient;

    public OpenMeteoService() {
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent", "OpenMeteoClient/1.0")
                .build();
    }

    // OpenMeteo天气数据模型
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WeatherData(
            @JsonProperty("latitude") Double latitude,
            @JsonProperty("longitude") Double longitude,
            @JsonProperty("timezone") String timezone,
            @JsonProperty("current") CurrentWeather current,
            @JsonProperty("daily") DailyForecast daily,
            @JsonProperty("current_units") CurrentUnits currentUnits) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record CurrentWeather(
                @JsonProperty("time") String time,
                @JsonProperty("temperature_2m") Double temperature,
                @JsonProperty("apparent_temperature") Double feelsLike,
                @JsonProperty("relative_humidity_2m") Integer humidity,
                @JsonProperty("precipitation") Double precipitation,
                @JsonProperty("weather_code") Integer weatherCode,
                @JsonProperty("wind_speed_10m") Double windSpeed,
                @JsonProperty("wind_direction_10m") Integer windDirection) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record CurrentUnits(
                @JsonProperty("time") String timeUnit,
                @JsonProperty("temperature_2m") String temperatureUnit,
                @JsonProperty("relative_humidity_2m") String humidityUnit,
                @JsonProperty("wind_speed_10m") String windSpeedUnit) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record DailyForecast(
                @JsonProperty("time") List<String> time,
                @JsonProperty("temperature_2m_max") List<Double> tempMax,
                @JsonProperty("temperature_2m_min") List<Double> tempMin,
                @JsonProperty("precipitation_sum") List<Double> precipitationSum,
                @JsonProperty("weather_code") List<Integer> weatherCode,
                @JsonProperty("wind_speed_10m_max") List<Double> windSpeedMax,
                @JsonProperty("wind_direction_10m_dominant") List<Integer> windDirection) {
        }
    }

    
    private String getWeatherDescription(int code) {
        return switch (code) {
            case 0 -> "晴朗";
            case 1, 2, 3 -> "多云";
            case 45, 48 -> "雾";
            case 51, 53, 55 -> "毛毛雨";
            case 56, 57 -> "冻雨";
            case 61, 63, 65 -> "雨";
            case 66, 67 -> "冻雨";
            case 71, 73, 75 -> "雪";
            case 77 -> "雪粒";
            case 80, 81, 82 -> "阵雨";
            case 85, 86 -> "阵雪";
            case 95 -> "雷暴";
            case 96, 99 -> "雷暴伴有冰雹";
            default -> "未知天气";
        };
    }

    
    private String getWindDirection(int degrees) {
        if (degrees >= 337.5 || degrees < 22.5)
            return "北风";
        if (degrees >= 22.5 && degrees < 67.5)
            return "东北风";
        if (degrees >= 67.5 && degrees < 112.5)
            return "东风";
        if (degrees >= 112.5 && degrees < 157.5)
            return "东南风";
        if (degrees >= 157.5 && degrees < 202.5)
            return "南风";
        if (degrees >= 202.5 && degrees < 247.5)
            return "西南风";
        if (degrees >= 247.5 && degrees < 292.5)
            return "西风";
        return "西北风";
    }

    
    @Tool(description = "获取指定经纬度的天气预报")
    public String getWeatherForecastByLocation(double latitude, double longitude) {
        logger.info("天气预报，当前地区的纬度: {}, 经度: {}", latitude, longitude);

        // 获取天气数据（当前和未来7天）
        var weatherData = restClient.get()
                .uri("/forecast?latitude={latitude}&longitude={longitude}&current=temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,wind_direction_10m&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,weather_code,wind_speed_10m_max,wind_direction_10m_dominant&timezone=auto&forecast_days=7",
                        latitude, longitude)
                .retrieve()
                .body(WeatherData.class);

        // 拼接天气信息
        StringBuilder weatherInfo = new StringBuilder();

        // 添加当前天气信息
        WeatherData.CurrentWeather current = weatherData.current();
        String temperatureUnit = weatherData.currentUnits() != null ? weatherData.currentUnits().temperatureUnit()
                : "°C";
        String windSpeedUnit = weatherData.currentUnits() != null ? weatherData.currentUnits().windSpeedUnit() : "km/h";
        String humidityUnit = weatherData.currentUnits() != null ? weatherData.currentUnits().humidityUnit() : "%";

        weatherInfo.append(String.format("""
                当前天气:
                温度: %.1f%s (体感温度: %.1f%s)
                天气: %s
                风向: %s (%.1f %s)
                湿度: %d%s
                降水量: %.1f 毫米

                """,
                current.temperature(),
                temperatureUnit,
                current.feelsLike(),
                temperatureUnit,
                getWeatherDescription(current.weatherCode()),
                getWindDirection(current.windDirection()),
                current.windSpeed(),
                windSpeedUnit,
                current.humidity(),
                humidityUnit,
                current.precipitation()));

        // 添加未来天气预报
        weatherInfo.append("未来天气预报:\n");
        WeatherData.DailyForecast daily = weatherData.daily();

        for (int i = 0; i < daily.time().size(); i++) {
            String date = daily.time().get(i);
            double tempMin = daily.tempMin().get(i);
            double tempMax = daily.tempMax().get(i);
            int weatherCode = daily.weatherCode().get(i);
            double windSpeed = daily.windSpeedMax().get(i);
            int windDir = daily.windDirection().get(i);
            double precip = daily.precipitationSum().get(i);

            // 格式化日期
            LocalDate localDate = LocalDate.parse(date);
            String formattedDate = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd (EEE)"));

            weatherInfo.append(String.format("""
                    %s:
                    温度: %.1f%s ~ %.1f%s
                    天气: %s
                    风向: %s (%.1f %s)
                    降水量: %.1f 毫米

                    """,
                    formattedDate,
                    tempMin, temperatureUnit,
                    tempMax, temperatureUnit,
                    getWeatherDescription(weatherCode),
                    getWindDirection(windDir),
                    windSpeed, windSpeedUnit,
                    precip));
        }

        return weatherInfo.toString();
    }

    
    @Tool(description = "获取指定位置的空气质量信息（模拟数据）")
    public String getAirQuality(@ToolParam(description = "纬度") double latitude,
                                @ToolParam(description = "经度") double longitude) {
        logger.info("空气质量信息，当前地区的纬度: {}, 经度: {}", latitude, longitude);

        try {
            // 从天气数据中获取基本信息
            var weatherData = restClient.get()
                    .uri("/forecast?latitude={latitude}&longitude={longitude}&current=temperature_2m&timezone=auto",
                            latitude, longitude)
                    .retrieve()
                    .body(WeatherData.class);

            // 模拟空气质量数据 - 实际情况下应该从真实API获取
            // 根据经纬度生成一些随机但相对合理的数据
            int europeanAqi = (int) (Math.random() * 100) + 1;
            int usAqi = (int) (europeanAqi * 1.5);
            double pm10 = Math.random() * 50 + 5;
            double pm25 = Math.random() * 25 + 2;
            double co = Math.random() * 500 + 100;
            double no2 = Math.random() * 40 + 5;
            double so2 = Math.random() * 20 + 1;
            double o3 = Math.random() * 80 + 20;

            // 根据AQI评估空气质量等级
            String europeanAqiLevel = getAqiLevel(europeanAqi);
            String usAqiLevel = getUsAqiLevel(usAqi);

            return String.format("""
                    空气质量信息（模拟数据）:

                    位置: 纬度 %.4f, 经度 %.4f
                    欧洲空气质量指数: %d (%s)
                    美国空气质量指数: %d (%s)
                    PM10: %.1f μg/m³
                    PM2.5: %.1f μg/m³
                    一氧化碳(CO): %.1f μg/m³
                    二氧化氮(NO2): %.1f μg/m³
                    二氧化硫(SO2): %.1f μg/m³
                    臭氧(O3): %.1f μg/m³

                    数据更新时间: %s

                    注意: 由于OpenMeteo空气质量API限制，此处显示模拟数据，仅供参考。
                    """,
                    latitude, longitude,
                    europeanAqi, europeanAqiLevel,
                    usAqi, usAqiLevel,
                    pm10,
                    pm25,
                    co,
                    no2,
                    so2,
                    o3,
                    weatherData.current().time());
        } catch (Exception e) {
            // 如果获取基本天气数据失败，返回完全模拟的数据
            return String.format("""
                    空气质量信息（完全模拟数据）:

                    位置: 纬度 %.4f, 经度 %.4f
                    欧洲空气质量指数: %d (%s)
                    美国空气质量指数: %d (%s)
                    PM10: %.1f μg/m³
                    PM2.5: %.1f μg/m³
                    一氧化碳(CO): %.1f μg/m³
                    二氧化氮(NO2): %.1f μg/m³
                    二氧化硫(SO2): %.1f μg/m³
                    臭氧(O3): %.1f μg/m³

                    注意: 由于API限制，此处显示完全模拟数据，仅供参考。
                    """,
                    latitude, longitude,
                    50, getAqiLevel(50),
                    75, getUsAqiLevel(75),
                    25.0,
                    15.0,
                    300.0,
                    20.0,
                    5.0,
                    40.0);
        }
    }

    
    private String getAqiLevel(Integer aqi) {
        if (aqi == null)
            return "未知";

        if (aqi <= 20)
            return "优";
        else if (aqi <= 40)
            return "良";
        else if (aqi <= 60)
            return "中等";
        else if (aqi <= 80)
            return "较差";
        else if (aqi <= 100)
            return "差";
        else
            return "极差";
    }

    
    private String getUsAqiLevel(Integer aqi) {
        if (aqi == null)
            return "未知";

        if (aqi <= 50)
            return "优";
        else if (aqi <= 100)
            return "中等";
        else if (aqi <= 150)
            return "对敏感人群不健康";
        else if (aqi <= 200)
            return "不健康";
        else if (aqi <= 300)
            return "非常不健康";
        else
            return "危险";
    }

    public static void main(String[] args) {
        OpenMeteoService client = new OpenMeteoService();
        // 北京坐标
        System.out.println(client.getWeatherForecastByLocation(39.9042, 116.4074));
        // 北京空气质量（模拟数据）
        System.out.println(client.getAirQuality(39.9042, 116.4074));
    }
}
```

工具服务统一放在一个配置类中

```Java
@Configuration
public class ToolConfiguration {

    @Bean
    public ToolCallbackProvider weatherTools(OpenMeteoService openMeteoService, TimeService timeService) {
        return MethodToolCallbackProvider.builder().toolObjects(openMeteoService, timeService).build();
    }
}
```

启动server端

```Java
@SpringBootApplication
@Import(ToolConfiguration.class)
public class ServerWebfluxApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerWebfluxApplication.class, args);
    }
}
```

#### Client

```Java
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.Scanner;

@SpringBootApplication(exclude = {
        org.springframework.ai.autoconfigure.mcp.client.SseHttpClientTransportAutoConfiguration.class
})
public class ClientWebfluxApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientWebfluxApplication.class, args);
    }

    @Bean
    public CommandLineRunner predefinedQuestions(ChatClient.Builder chatClientBuilder, ToolCallbackProvider tools,
                                                 ConfigurableApplicationContext context) {

        return args -> {
            var chatClient = chatClientBuilder
                    .defaultTools(tools)
                    .build();

            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("\n>>> QUESTION: ");
                String userInput = scanner.nextLine();
                if (userInput.equalsIgnoreCase("exit")) {
                    break;
                }
                System.out.println("\n>>> ASSISTANT: " + chatClient.prompt(userInput).call().content());
            }
            scanner.close();
            context.close();
        };
    }
}
```

#### 效果展示

先启动server端，再启动client端，在client端端命令行中可进行多轮问答，触发在server配置过的MCP服务

![](/img/blog/mcp-desc/webflux-example.png)

## 参考资料

https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html

[Spring AI 1.0.0 M6新特性MCP](https://blog.csdn.net/qq_42731358/article/details/146097943)

deepseek-v3