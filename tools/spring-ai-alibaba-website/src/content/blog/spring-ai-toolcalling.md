---
title: Spring AI 源码解析：Tool Calling链路调用流程及示例
keywords: [Spring AI, Spring AI Alibaba, Tool Calling, Remote API]
description: "Tool工具允许模型与一组API或工具进行交互，增强模型功能，主要用于信息检索、采取特定操作等"
author: "影子"
date: "2025-04-01"
category: article
---

Tool工具允许模型与一组API或工具进行交互，增强模型功能，主要用于：

- 信息检索：从外部数据源检索信息，如数据库、Web服务、文件系统或Web搜索引擎等
- 采取行动：可用于在软件系统中执行特定操作，如发送电子邮件、在数据库中创建新记录、触发工作流等

注：
- 本版源码解析取自[Spring-ai（20250321）](https://github.com/spring-projects/spring-ai)仓库最新代码（暂未发版），目前最新的1.0.0.-M6有部分类和方法将过期，故不在此讨论范畴中

本文实践代码可见spingr-ai-alibaba-examples项目下的[spring-ai-alibaba-tool-calling-examples](https://github.com/springaialibaba/spring-ai-alibaba-examples/tree/main/spring-ai-alibaba-tool-calling-example)

## 理论部分

![](/img/blog/spring-ai-tool-calling/toolcalling-overall.png)

1. 在聊天请求中包含工具的定义，包括工具名称、描述、输入模式
2. 当AI模型决定调用一个工具时，会发送一个响应，包含工具名称和输入参数（大模型提取文本根据输入模式转化而得）
3. 应用程序将使用工具名称并使用提供的输入参数
4. 工具计算结果后将结果返回给应用程序
5. 应用程序再将结果发送给模型
6. 模型添加工具结果作为附加的上下文信息生成最终响应

### 工具调用链路（核心）

下图以ChatClient调用tools(String... toolNames)方法全链路流程展示

![](/img/blog/spring-ai-tool-calling/flow.png)

```Java
public class DefaultChatClient implements ChatClient {
    @Override
    public ChatClientRequestSpec tools(String... toolNames) {
        Assert.notNull(toolNames, "toolNames cannot be null");
        Assert.noNullElements(toolNames, "toolNames cannot contain null elements");
        this.functionNames.addAll(List.of(toolNames));
        return this;
    }
    
    @Override
    public ChatClientRequestSpec tools(FunctionCallback... toolCallbacks) {
        Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
        Assert.noNullElements(toolCallbacks, "toolCallbacks cannot contain null elements");
        this.functionCallbacks.addAll(List.of(toolCallbacks));
        return this;
    }
    
    @Override
    public ChatClientRequestSpec tools(List<ToolCallback> toolCallbacks) {
        Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
        Assert.noNullElements(toolCallbacks, "toolCallbacks cannot contain null elements");
        this.functionCallbacks.addAll(toolCallbacks);
        return this;
    }
    
    @Override
    public ChatClientRequestSpec tools(Object... toolObjects) {
        Assert.notNull(toolObjects, "toolObjects cannot be null");
        Assert.noNullElements(toolObjects, "toolObjects cannot contain null elements");
        this.functionCallbacks.addAll(Arrays.asList(ToolCallbacks.from(toolObjects)));
        return this;
    }
    
    @Override
    public ChatClientRequestSpec tools(ToolCallbackProvider... toolCallbackProviders) {
        Assert.notNull(toolCallbackProviders, "toolCallbackProviders cannot be null");
        Assert.noNullElements(toolCallbackProviders, "toolCallbackProviders cannot contain null elements");
        for (ToolCallbackProvider toolCallbackProvider : toolCallbackProviders) {
           this.functionCallbacks.addAll(List.of(toolCallbackProvider.getToolCallbacks()));
        }
        return this;
    }

}
```

### Tool（工具注解）

```java
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Tool {

    /**
     * The name of the tool. If not provided, the method name will be used.
     */
    String name() default "";

    /**
     * The description of the tool. If not provided, the method name will be used.
     */
    String description() default "";

    /**
     * Whether the tool result should be returned directly or passed back to the model.
     */
    boolean returnDirect() default false;

    /**
     * The class to use to convert the tool call result to a String.
     */
    Class<? extends ToolCallResultConverter> resultConverter() default DefaultToolCallResultConverter.class;

}
```

### ToolDefinition（工具定义）

```Java
public interface ToolDefinition {
    // 工具的名称，提供给一个模型时要求唯一标识
    String name();
    // 工具描述
    String description();
    // 工具的输入模式
    String inputSchema();

    static DefaultToolDefinition.Builder builder() {
       return DefaultToolDefinition.builder();
    }
    // 从方法中提取出工具的名称、描述、输入模式
    static DefaultToolDefinition.Builder builder(Method method) {
       Assert.notNull(method, "method cannot be null");
       return DefaultToolDefinition.builder()
          .name(ToolUtils.getToolName(method))
          .description(ToolUtils.getToolDescription(method))
          .inputSchema(JsonSchemaGenerator.generateForMethodInput(method));
    }

    static ToolDefinition from(Method method) {
       return ToolDefinition.builder(method).build();
    }

}
```

#### DefaultToolDefinition

```Java
public record DefaultToolDefinition(String name, String description, String inputSchema) implements ToolDefinition {

    public DefaultToolDefinition {
       Assert.hasText(name, "name cannot be null or empty");
       Assert.hasText(description, "description cannot be null or empty");
       Assert.hasText(inputSchema, "inputSchema cannot be null or empty");
    }

    public static Builder builder() {
       return new Builder();
    }

    public static class Builder {

       private String name;

       private String description;

       private String inputSchema;

       private Builder() {
       }

       public Builder name(String name) {
          this.name = name;
          return this;
       }

       public Builder description(String description) {
          this.description = description;
          return this;
       }

       public Builder inputSchema(String inputSchema) {
          this.inputSchema = inputSchema;
          return this;
       }

       public ToolDefinition build() {
          if (!StringUtils.hasText(description)) {
             description = ToolUtils.getToolDescriptionFromName(name);
          }
          return new DefaultToolDefinition(name, description, inputSchema);
       }

    }

}
```

### ToolMetadata（工具元数据）

现阶段只用于控制直接将工具结果返回，不再走模型响应

```Java
public interface ToolMetadata {

    default boolean returnDirect() {
       return false;
    }

    static DefaultToolMetadata.Builder builder() {
       return DefaultToolMetadata.builder();
    }

    static ToolMetadata from(Method method) {
       Assert.notNull(method, "method cannot be null");
       return DefaultToolMetadata.builder().returnDirect(ToolUtils.getToolReturnDirect(method)).build();
    }

}
```

#### DefaultToolMetadata

```Java
public record DefaultToolMetadata(boolean returnDirect) implements ToolMetadata {

    public static Builder builder() {
       return new Builder();
    }

    public static class Builder {

       private boolean returnDirect = false;

       private Builder() {
       }

       public Builder returnDirect(boolean returnDirect) {
          this.returnDirect = returnDirect;
          return this;
       }

       public ToolMetadata build() {
          return new DefaultToolMetadata(returnDirect);
       }

    }

}
```

### ToolCallback（工具回调）

```Java
public interface ToolCallback{
    
    // AI模型用来确定何时以及如何调用工具的定义
    ToolDefinition getToolDefinition();
    
    // 元数据提供了额外的信息怎么操作工具
    default ToolMetadata getToolMetadata() {
       return ToolMetadata.builder().build();
    }
    
    // toolInput为工具的输入，最终返回结果工具的结果
    String call(String toolInput);
    
    // toolInput为工具的输入，tooContext为工具的上下文信息
    default String call(String toolInput, @Nullable ToolContext tooContext) {
       if (tooContext != null && !tooContext.getContext().isEmpty()) {
          throw new UnsupportedOperationException("Tool context is not supported!");
       }
       return call(toolInput);
    }

}
```

#### MethodToolCallback

核心方法主要关注call

1. 将模型处理后的字符串文本，转化为对应的输入模式

   1. ```Java
      Map<String, Object> toolArguments = extractToolArguments(toolInput);
      Object[] methodArguments = buildMethodArguments(toolArguments, toolContext);
      ```

2. 调用工具的方法+输入参数，得到工具的输出结果

   1. ```Java
      Object result = callMethod(methodArguments);
      ```

3. 将工具的输出结果的类型进行转化

   1. ```
      Type returnType = toolMethod.getGenericReturnType();
      return toolCallResultConverter.convert(result, returnType);
      ```

```Java
public class MethodToolCallback implements ToolCallback {

    private static final Logger logger = LoggerFactory.getLogger(MethodToolCallback.class);

    private static final ToolCallResultConverter DEFAULT_RESULT_CONVERTER = new DefaultToolCallResultConverter();

    private static final ToolMetadata DEFAULT_TOOL_METADATA = ToolMetadata.builder().build();

    private final ToolDefinition toolDefinition;

    private final ToolMetadata toolMetadata;

    private final Method toolMethod;

    @Nullable
    private final Object toolObject;

    private final ToolCallResultConverter toolCallResultConverter;

    public MethodToolCallback(ToolDefinition toolDefinition, @Nullable ToolMetadata toolMetadata, Method toolMethod,
          @Nullable Object toolObject, @Nullable ToolCallResultConverter toolCallResultConverter) {
       Assert.notNull(toolDefinition, "toolDefinition cannot be null");
       Assert.notNull(toolMethod, "toolMethod cannot be null");
       Assert.isTrue(Modifier.isStatic(toolMethod.getModifiers()) || toolObject != null,
             "toolObject cannot be null for non-static methods");
       this.toolDefinition = toolDefinition;
       this.toolMetadata = toolMetadata != null ? toolMetadata : DEFAULT_TOOL_METADATA;
       this.toolMethod = toolMethod;
       this.toolObject = toolObject;
       this.toolCallResultConverter = toolCallResultConverter != null ? toolCallResultConverter
             : DEFAULT_RESULT_CONVERTER;
    }

    @Override
    public ToolDefinition getToolDefinition() {
       return toolDefinition;
    }

    @Override
    public ToolMetadata getToolMetadata() {
       return toolMetadata;
    }

    @Override
    public String call(String toolInput) {
       return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
       Assert.hasText(toolInput, "toolInput cannot be null or empty");

       logger.debug("Starting execution of tool: {}", toolDefinition.name());

       validateToolContextSupport(toolContext);

       Map<String, Object> toolArguments = extractToolArguments(toolInput);

       Object[] methodArguments = buildMethodArguments(toolArguments, toolContext);

       Object result = callMethod(methodArguments);

       logger.debug("Successful execution of tool: {}", toolDefinition.name());

       Type returnType = toolMethod.getGenericReturnType();

       return toolCallResultConverter.convert(result, returnType);
    }

    private void validateToolContextSupport(@Nullable ToolContext toolContext) {
       var isNonEmptyToolContextProvided = toolContext != null && !CollectionUtils.isEmpty(toolContext.getContext());
       var isToolContextAcceptedByMethod = Stream.of(toolMethod.getParameterTypes())
          .anyMatch(type -> ClassUtils.isAssignable(type, ToolContext.class));
       if (isToolContextAcceptedByMethod && !isNonEmptyToolContextProvided) {
          throw new IllegalArgumentException("ToolContext is required by the method as an argument");
       }
    }

    private Map<String, Object> extractToolArguments(String toolInput) {
       return JsonParser.fromJson(toolInput, new TypeReference<>() {
       });
    }

    // Based on the implementation in MethodInvokingFunctionCallback.
    private Object[] buildMethodArguments(Map<String, Object> toolInputArguments, @Nullable ToolContext toolContext) {
       return Stream.of(toolMethod.getParameters()).map(parameter -> {
          if (parameter.getType().isAssignableFrom(ToolContext.class)) {
             return toolContext;
          }
          Object rawArgument = toolInputArguments.get(parameter.getName());
          return buildTypedArgument(rawArgument, parameter.getType());
       }).toArray();
    }

    @Nullable
    private Object buildTypedArgument(@Nullable Object value, Class<?> type) {
       if (value == null) {
          return null;
       }
       return JsonParser.toTypedObject(value, type);
    }

    @Nullable
    private Object callMethod(Object[] methodArguments) {
       if (isObjectNotPublic() || isMethodNotPublic()) {
          toolMethod.setAccessible(true);
       }

       Object result;
       try {
          result = toolMethod.invoke(toolObject, methodArguments);
       }
       catch (IllegalAccessException ex) {
          throw new IllegalStateException("Could not access method: " + ex.getMessage(), ex);
       }
       catch (InvocationTargetException ex) {
          throw new ToolExecutionException(toolDefinition, ex.getCause());
       }
       return result;
    }

    private boolean isObjectNotPublic() {
       return toolObject != null && !Modifier.isPublic(toolObject.getClass().getModifiers());
    }

    private boolean isMethodNotPublic() {
       return !Modifier.isPublic(toolMethod.getModifiers());
    }

    @Override
    public String toString() {
       return "MethodToolCallback{" + "toolDefinition=" + toolDefinition + ", toolMetadata=" + toolMetadata + '}';
    }

    public static Builder builder() {
       return new Builder();
    }

    public static class Builder {

       private ToolDefinition toolDefinition;

       private ToolMetadata toolMetadata;

       private Method toolMethod;

       private Object toolObject;

       private ToolCallResultConverter toolCallResultConverter;

       private Builder() {
       }

       public Builder toolDefinition(ToolDefinition toolDefinition) {
          this.toolDefinition = toolDefinition;
          return this;
       }

       public Builder toolMetadata(ToolMetadata toolMetadata) {
          this.toolMetadata = toolMetadata;
          return this;
       }

       public Builder toolMethod(Method toolMethod) {
          this.toolMethod = toolMethod;
          return this;
       }

       public Builder toolObject(Object toolObject) {
          this.toolObject = toolObject;
          return this;
       }

       public Builder toolCallResultConverter(ToolCallResultConverter toolCallResultConverter) {
          this.toolCallResultConverter = toolCallResultConverter;
          return this;
       }

       public MethodToolCallback build() {
          return new MethodToolCallback(toolDefinition, toolMetadata, toolMethod, toolObject,
                toolCallResultConverter);
       }

    }

}
```

#### FunctionToolCallback

核心方法主要关注call

1. 模型提取的toolInput为json字符串，先转为定义的Request类型

```Java
I request = JsonParser.fromJson(toolInput, toolInputType);
```

1. 工具调用，返回对应的工具结果

```Java
O response = toolFunction.apply(request, toolContext);
public class FunctionToolCallback<I, O> implements ToolCallback {

    private static final Logger logger = LoggerFactory.getLogger(FunctionToolCallback.class);

    private static final ToolCallResultConverter DEFAULT_RESULT_CONVERTER = new DefaultToolCallResultConverter();

    private static final ToolMetadata DEFAULT_TOOL_METADATA = ToolMetadata.builder().build();

    private final ToolDefinition toolDefinition;

    private final ToolMetadata toolMetadata;

    private final Type toolInputType;

    private final BiFunction<I, ToolContext, O> toolFunction;

    private final ToolCallResultConverter toolCallResultConverter;

    public FunctionToolCallback(ToolDefinition toolDefinition, @Nullable ToolMetadata toolMetadata, Type toolInputType,
          BiFunction<I, ToolContext, O> toolFunction, @Nullable ToolCallResultConverter toolCallResultConverter) {
       Assert.notNull(toolDefinition, "toolDefinition cannot be null");
       Assert.notNull(toolInputType, "toolInputType cannot be null");
       Assert.notNull(toolFunction, "toolFunction cannot be null");
       this.toolDefinition = toolDefinition;
       this.toolMetadata = toolMetadata != null ? toolMetadata : DEFAULT_TOOL_METADATA;
       this.toolFunction = toolFunction;
       this.toolInputType = toolInputType;
       this.toolCallResultConverter = toolCallResultConverter != null ? toolCallResultConverter
             : DEFAULT_RESULT_CONVERTER;
    }

    @Override
    public ToolDefinition getToolDefinition() {
       return toolDefinition;
    }

    @Override
    public ToolMetadata getToolMetadata() {
       return toolMetadata;
    }

    @Override
    public String call(String toolInput) {
       return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
       Assert.hasText(toolInput, "toolInput cannot be null or empty");

       logger.debug("Starting execution of tool: {}", toolDefinition.name());

       I request = JsonParser.fromJson(toolInput, toolInputType);
       O response = toolFunction.apply(request, toolContext);

       logger.debug("Successful execution of tool: {}", toolDefinition.name());

       return toolCallResultConverter.convert(response, null);
    }

    @Override
    public String toString() {
       return "FunctionToolCallback{" + "toolDefinition=" + toolDefinition + ", toolMetadata=" + toolMetadata + '}';
    }


    public static <I, O> Builder<I, O> builder(String name, BiFunction<I, ToolContext, O> function) {
       return new Builder<>(name, function);
    }

  
    public static <I, O> Builder<I, O> builder(String name, Function<I, O> function) {
       Assert.notNull(function, "function cannot be null");
       return new Builder<>(name, (request, context) -> function.apply(request));
    }


    public static <O> Builder<Void, O> builder(String name, Supplier<O> supplier) {
       Assert.notNull(supplier, "supplier cannot be null");
       Function<Void, O> function = input -> supplier.get();
       return builder(name, function).inputType(Void.class);
    }


    public static <I> Builder<I, Void> builder(String name, Consumer<I> consumer) {
       Assert.notNull(consumer, "consumer cannot be null");
       Function<I, Void> function = (I input) -> {
          consumer.accept(input);
          return null;
       };
       return builder(name, function);
    }

    public static class Builder<I, O> {

       private String name;

       private String description;

       private String inputSchema;

       private Type inputType;

       private ToolMetadata toolMetadata;

       private BiFunction<I, ToolContext, O> toolFunction;

       private ToolCallResultConverter toolCallResultConverter;

       private Builder(String name, BiFunction<I, ToolContext, O> toolFunction) {
          Assert.hasText(name, "name cannot be null or empty");
          Assert.notNull(toolFunction, "toolFunction cannot be null");
          this.name = name;
          this.toolFunction = toolFunction;
       }

       public Builder<I, O> description(String description) {
          this.description = description;
          return this;
       }

       public Builder<I, O> inputSchema(String inputSchema) {
          this.inputSchema = inputSchema;
          return this;
       }

       public Builder<I, O> inputType(Type inputType) {
          this.inputType = inputType;
          return this;
       }

       public Builder<I, O> inputType(ParameterizedTypeReference<?> inputType) {
          Assert.notNull(inputType, "inputType cannot be null");
          this.inputType = inputType.getType();
          return this;
       }

       public Builder<I, O> toolMetadata(ToolMetadata toolMetadata) {
          this.toolMetadata = toolMetadata;
          return this;
       }

       public Builder<I, O> toolCallResultConverter(ToolCallResultConverter toolCallResultConverter) {
          this.toolCallResultConverter = toolCallResultConverter;
          return this;
       }

       public FunctionToolCallback<I, O> build() {
          Assert.notNull(inputType, "inputType cannot be null");
          var toolDefinition = ToolDefinition.builder()
             .name(name)
             .description(
                   StringUtils.hasText(description) ? description : ToolUtils.getToolDescriptionFromName(name))
             .inputSchema(
                   StringUtils.hasText(inputSchema) ? inputSchema : JsonSchemaGenerator.generateForType(inputType))
             .build();
          return new FunctionToolCallback<>(toolDefinition, toolMetadata, inputType, toolFunction,
                toolCallResultConverter);
       }

    }

}
```

### ToolCallbackProvider（工具回调实例提供）

主要用于集中管理和提供工具回调

- getToolCallbacks：获得工具回调数组

```Java
public interface ToolCallbackProvider {

    ToolCallback[] getToolCallbacks();

    static ToolCallbackProvider from(List<? extends FunctionCallback> toolCallbacks) {
       return new StaticToolCallbackProvider(toolCallbacks);
    }

    static ToolCallbackProvider from(FunctionCallback... toolCallbacks) {
       return new StaticToolCallbackProvider(toolCallbacks);
    }

}
```

#### MethodToolCallbackProvider

获取MethodToolCallback实例

```Java
public class MethodToolCallbackProvider implements ToolCallbackProvider {

    private static final Logger logger = LoggerFactory.getLogger(MethodToolCallbackProvider.class);

    private final List<Object> toolObjects;

    private MethodToolCallbackProvider(List<Object> toolObjects) {
       Assert.notNull(toolObjects, "toolObjects cannot be null");
       Assert.noNullElements(toolObjects, "toolObjects cannot contain null elements");
       this.toolObjects = toolObjects;
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
       var toolCallbacks = toolObjects.stream()
          .map(toolObject -> Stream
             .of(ReflectionUtils.getDeclaredMethods(
                   AopUtils.isAopProxy(toolObject) ? AopUtils.getTargetClass(toolObject) : toolObject.getClass()))
             .filter(toolMethod -> toolMethod.isAnnotationPresent(Tool.class))
             .filter(toolMethod -> !isFunctionalType(toolMethod))
             .map(toolMethod -> MethodToolCallback.builder()
                .toolDefinition(ToolDefinition.from(toolMethod))
                .toolMetadata(ToolMetadata.from(toolMethod))
                .toolMethod(toolMethod)
                .toolObject(toolObject)
                .toolCallResultConverter(ToolUtils.getToolCallResultConverter(toolMethod))
                .build())
             .toArray(ToolCallback[]::new))
          .flatMap(Stream::of)
          .toArray(ToolCallback[]::new);

       validateToolCallbacks(toolCallbacks);

       return toolCallbacks;
    }

    private boolean isFunctionalType(Method toolMethod) {
       var isFunction = ClassUtils.isAssignable(toolMethod.getReturnType(), Function.class)
             || ClassUtils.isAssignable(toolMethod.getReturnType(), Supplier.class)
             || ClassUtils.isAssignable(toolMethod.getReturnType(), Consumer.class);

       if (isFunction) {
          logger.warn("Method {} is annotated with @Tool but returns a functional type. "
                + "This is not supported and the method will be ignored.", toolMethod.getName());
       }

       return isFunction;
    }

    private void validateToolCallbacks(ToolCallback[] toolCallbacks) {
       List<String> duplicateToolNames = ToolUtils.getDuplicateToolNames(toolCallbacks);
       if (!duplicateToolNames.isEmpty()) {
          throw new IllegalStateException("Multiple tools with the same name (%s) found in sources: %s".formatted(
                String.join(", ", duplicateToolNames),
                toolObjects.stream().map(o -> o.getClass().getName()).collect(Collectors.joining(", "))));
       }
    }

    public static Builder builder() {
       return new Builder();
    }

    public static class Builder {

       private List<Object> toolObjects;

       private Builder() {
       }

       public Builder toolObjects(Object... toolObjects) {
          Assert.notNull(toolObjects, "toolObjects cannot be null");
          this.toolObjects = Arrays.asList(toolObjects);
          return this;
       }

       public MethodToolCallbackProvider build() {
          return new MethodToolCallbackProvider(toolObjects);
       }

    }

}
```

#### StaticToolCallbackProvider

提供FunctionToolCallback，但目测还没有实现该功能

```java
public class StaticToolCallbackProvider implements ToolCallbackProvider {

    private final FunctionCallback[] toolCallbacks;

    public StaticToolCallbackProvider(FunctionCallback... toolCallbacks) {
       Assert.notNull(toolCallbacks, "ToolCallbacks must not be null");
       this.toolCallbacks = toolCallbacks;
    }

    public StaticToolCallbackProvider(List<? extends FunctionCallback> toolCallbacks) {
       Assert.noNullElements(toolCallbacks, "toolCallbacks cannot contain null elements");
       this.toolCallbacks = toolCallbacks.toArray(new FunctionCallback[0]);
    }

    @Override
    public FunctionCallback[] getToolCallbacks() {
       return this.toolCallbacks;
    }

}
```

### ToolCallingManager（工具回调管理器）

```c++
public interface ToolCallingManager {

     // 从配置中提取工具的定义，确保模型能正确识别和使用工具
    List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions);

    // 根据模型的响应，执行响应的工具调用，并返回执行结果
    ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse);

    // 构建工具调用管理器
    static DefaultToolCallingManager.Builder builder() {
       return DefaultToolCallingManager.builder();
    }

}
```

#### DefaultToolCallingManager

核心功能如下

1. 解析工具定义（resolveToolDefinitions）：从ToolCallingChatOptions中解析出工具定义，确保模型能正确识别和使用工具
2. 执行工具调用（executeToolCalls）：根据模型响应，执行相应的工具调用，并返回工具的执行结果
3. 构建工具上下文（buildToolContext）：为工具调用提供上下文信息，历史的Message记录
4. 管理工具回调：通过 ToolCallbackResolver 解析工具回调，支持动态工具调用

```Java
public class DefaultToolCallingManager implements ToolCallingManager {

    @Override
    public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
        Assert.notNull(chatOptions, "chatOptions cannot be null");
    
        List<FunctionCallback> toolCallbacks = new ArrayList<>(chatOptions.getToolCallbacks());
        for (String toolName : chatOptions.getToolNames()) {
           // Skip the tool if it is already present in the request toolCallbacks.
           // That might happen if a tool is defined in the options
           // both as a ToolCallback and as a tool name.
           if (chatOptions.getToolCallbacks().stream().anyMatch(tool -> tool.getName().equals(toolName))) {
              continue;
           }
           FunctionCallback toolCallback = toolCallbackResolver.resolve(toolName);
           if (toolCallback == null) {
              throw new IllegalStateException("No ToolCallback found for tool name: " + toolName);
           }
           toolCallbacks.add(toolCallback);
        }
    
        return toolCallbacks.stream().map(functionCallback -> {
           if (functionCallback instanceof ToolCallback toolCallback) {
              return toolCallback.getToolDefinition();
           }
           else {
              return ToolDefinition.builder()
                 .name(functionCallback.getName())
                 .description(functionCallback.getDescription())
                 .inputSchema(functionCallback.getInputTypeSchema())
                 .build();
           }
        }).toList();
    }
    
    @Override
    public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
        Assert.notNull(prompt, "prompt cannot be null");
        Assert.notNull(chatResponse, "chatResponse cannot be null");
    
        Optional<Generation> toolCallGeneration = chatResponse.getResults()
           .stream()
           .filter(g -> !CollectionUtils.isEmpty(g.getOutput().getToolCalls()))
           .findFirst();
    
        if (toolCallGeneration.isEmpty()) {
           throw new IllegalStateException("No tool call requested by the chat model");
        }
    
        AssistantMessage assistantMessage = toolCallGeneration.get().getOutput();
    
        ToolContext toolContext = buildToolContext(prompt, assistantMessage);
    
        InternalToolExecutionResult internalToolExecutionResult = executeToolCall(prompt, assistantMessage,
              toolContext);
    
        List<Message> conversationHistory = buildConversationHistoryAfterToolExecution(prompt.getInstructions(),
              assistantMessage, internalToolExecutionResult.toolResponseMessage());
    
        return ToolExecutionResult.builder()
           .conversationHistory(conversationHistory)
           .returnDirect(internalToolExecutionResult.returnDirect())
           .build();
    }
    
}
```

### ToolCallResultConverter（工具结果转换器）

```Java
@FunctionalInterface
public interface ToolCallResultConverter {

    // result：工具结果，returnType：返回类型
    String convert(@Nullable Object result, @Nullable Type returnType);

}
```

#### DefaultToolCallResultConverter

ToolCallResultConverter接口类暂时的唯一实现，转为Json化的字符串

```java
public final class DefaultToolCallResultConverter implements ToolCallResultConverter {

    private static final Logger logger = LoggerFactory.getLogger(DefaultToolCallResultConverter.class);

    @Override
    public String convert(@Nullable Object result, @Nullable Type returnType) {
       if (returnType == Void.TYPE) {
          logger.debug("The tool has no return type. Converting to conventional response.");
          return "Done";
       }
       else {
          logger.debug("Converting tool result to JSON.");
          return JsonParser.toJson(result);
       }
    }

}
```

### ToolContext（工具上下文）

被构建于工具回调管理器

作用：

1. 用于封装工具执行的上下文信息，确保上下文不可变，从而保证线程安全
2. 通过getContext方法获取整个上下文，通过getToolCallHistory方法获取Message的历史记录

```Java
public class ToolContext {

    public static final String TOOL_CALL_HISTORY = "TOOL_CALL_HISTORY";

    private final Map<String, Object> context;

    public ToolContext(Map<String, Object> context) {
       this.context = Collections.unmodifiableMap(context);
    }

    public Map<String, Object> getContext() {
       return this.context;
    }

    @SuppressWarnings("unchecked")
    public List<Message> getToolCallHistory() {
       return (List<Message>) this.context.get(TOOL_CALL_HISTORY);
    }

}
```

### ToolUtils（工具常见方法封装）

从方法上提取名称，主要根据方法上是否有Tool注解，若无则统一设置为方法名

1. getToolName：获取工具名称
2. getToolDescriptionFromName：根据工具名称生成工具的描述
3. getToolDescription：获取工具描述
4. getToolReturnDirect：判断工具是否直接返回结果
5. getToolCallResultConverter：获取工具的结果转换器
6. getDuplicateToolNames：检查工具回调列表中是否有重复的工具名称

```Java
public final class ToolUtils {

    private ToolUtils() {
    }

    public static String getToolName(Method method) {
       Assert.notNull(method, "method cannot be null");
       var tool = method.getAnnotation(Tool.class);
       if (tool == null) {
          return method.getName();
       }
       return StringUtils.hasText(tool.name()) ? tool.name() : method.getName();
    }

    public static String getToolDescriptionFromName(String toolName) {
       Assert.hasText(toolName, "toolName cannot be null or empty");
       return ParsingUtils.reConcatenateCamelCase(toolName, " ");
    }

    public static String getToolDescription(Method method) {
       Assert.notNull(method, "method cannot be null");
       var tool = method.getAnnotation(Tool.class);
       if (tool == null) {
          return ParsingUtils.reConcatenateCamelCase(method.getName(), " ");
       }
       return StringUtils.hasText(tool.description()) ? tool.description() : method.getName();
    }

    public static boolean getToolReturnDirect(Method method) {
       Assert.notNull(method, "method cannot be null");
       var tool = method.getAnnotation(Tool.class);
       return tool != null && tool.returnDirect();
    }

    public static ToolCallResultConverter getToolCallResultConverter(Method method) {
       Assert.notNull(method, "method cannot be null");
       var tool = method.getAnnotation(Tool.class);
       if (tool == null) {
          return new DefaultToolCallResultConverter();
       }
       var type = tool.resultConverter();
       try {
          return type.getDeclaredConstructor().newInstance();
       }
       catch (Exception e) {
          throw new IllegalArgumentException("Failed to instantiate ToolCallResultConverter: " + type, e);
       }
    }

    public static List<String> getDuplicateToolNames(List<FunctionCallback> toolCallbacks) {
       Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
       return toolCallbacks.stream()
          .collect(Collectors.groupingBy(FunctionCallback::getName, Collectors.counting()))
          .entrySet()
          .stream()
          .filter(entry -> entry.getValue() > 1)
          .map(Map.Entry::getKey)
          .collect(Collectors.toList());
    }

    public static List<String> getDuplicateToolNames(FunctionCallback... toolCallbacks) {
       Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
       return getDuplicateToolNames(Arrays.asList(toolCallbacks));
    }

}
```

## tool工具实战（FunctionToolCallback && MethodToolCallBack版）

### application.yml

```yaml
server:
  port: 8083

spring:
  application:
    name: Tool-Calling

  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}

    toolcalling:
      baidutranslate:
        enabled: true
        app-id : ${BAIDU_TRANSLATE_APP_ID}
        secret-key: ${BAIDU_TRANSLATE_SECRET_KEY}

      time:
        enabled: true

      weather:
        enabled: true
        api-key: ${WEATHER_API_KEY}
```

百度翻译API接入文档：https://api.fanyi.baidu.com/product/113

天气预测API接入文档：https://www.weatherapi.com/docs/

### 当前时间

#### TimeAutoConfiguration

```python
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

@Configuration
@ConditionalOnClass({GetCurrentTimeByTimeZoneIdService.class})
@ConditionalOnProperty(prefix = "spring.ai.toolcalling.time", name = "enabled", havingValue = "true")
public class TimeAutoConfiguration {

    @Bean(name = "getCityTimeFunction")
    @ConditionalOnMissingBean
    @Description("Get the time of a specified city.")
    public GetCurrentTimeByTimeZoneIdService getCityTimeFunction() {
        return new GetCurrentTimeByTimeZoneIdService();
    }

}
```

1. `@Configuration`：定义为配置类
2. `@ConditionalOnClass({GetCurrentTimeByTimeZoneIdService.class})`：只有当类路径中存在GetCurrentTimeByTimeZoneIdService类才会加载该配置类
3. `@ConditionalOnProperty(prefix = "spring.ai.toolcalling.time", name = "enabled", havingValue = "true")`：只有当配置文件处spring.ai.toolcalling.time.enabled值为true时才会加载该配置类
4. `@Bean(name = "getCityTimeFunction")`：定义该Bean名称为getCityTimeFunction，并注册到Spring容器中
5. `@ConditionalOnMissingBean`：只有当Spring容器中不存在GetCurrentTimeByTimeZoneIdService类型的Bean时，才会创建该Bean

#### GetCurrentTimeByTimeZoneIdService

```Java
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class GetCurrentTimeByTimeZoneIdService implements Function<GetCurrentTimeByTimeZoneIdService.Request, GetCurrentTimeByTimeZoneIdService.Response> {

    private static final Logger logger = LoggerFactory.getLogger(GetCurrentTimeByTimeZoneIdService.class);

    @Override
    public GetCurrentTimeByTimeZoneIdService.Response apply(GetCurrentTimeByTimeZoneIdService.Request request) {
        String timeZoneId = request.timeZoneId;
        logger.info("The current time zone is {}", timeZoneId);
        return new Response(String.format("The current time zone is %s and the current time is " + "%s", timeZoneId,
                ZoneUtils.getTimeByZoneId(timeZoneId)));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonClassDescription("Get the current time based on time zone id")
    public record Request(@JsonProperty(required = true, value = "timeZoneId") @JsonPropertyDescription("Time "
            + "zone id, such as Asia/Shanghai") String timeZoneId) {
    }

    @JsonClassDescription("Current time in that time zone")
    public record Response(@JsonPropertyDescription("A description containing the current time zone and the current time in that time zone") String description) {
    }

}
```

1. 实现Function接口，重写apply方法，确保输入为Request、输出为Response
2. Request类为记录类，需要添加方法描述（JsonClassDescription） + 参数描述（JsonProperty），主要用于让模型提取出对应的输入参数timeZoneId

#### ZoneUtils

```java
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ZoneUtils {

    public static String getTimeByZoneId(String zoneId) {

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

时区工具类，用来获取zoneId时区的当前时间

#### TimeTools（单独实现MethodToolCallback版）

```Java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class TimeTools {
    private static final Logger logger = LoggerFactory.getLogger(TimeTools.class);

    @Tool(description = "Get the time of a specified city.")
    public String  getCityTimeMethod(@ToolParam(description = "Time zone id, such as Asia/Shanghai") String timeZoneId) {
        logger.info("The current time zone is {}", timeZoneId);
        return String.format("The current time zone is %s and the current time is " + "%s", timeZoneId,
                ZoneUtils.getTimeByZoneId(timeZoneId));
    }
}
```

#### TimeController

```Java
import com.yingzi.toolCalling.component.time.TimeTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/time")
public class TimeController {

    private final ChatClient dashScopeChatClient;

    public TimeController(ChatClient.Builder chatClientBuilder) {
        this.dashScopeChatClient = chatClientBuilder.build();
    }

    /**
     * 无工具版
     */
    @GetMapping("/chat")
    public String simpleChat(@RequestParam(value = "query", defaultValue = "请告诉我现在北京时间几点了") String query) {
        return dashScopeChatClient.prompt(query).call().content();
    }

    /**
     * 调用工具版 - function
     */
    @GetMapping("/chat-tool-function")
    public String chatTranslateFunction(@RequestParam(value = "query", defaultValue = "请告诉我现在北京时间几点了") String query) {
        return dashScopeChatClient.prompt(query).tools("getCityTimeFunction").call().content();
    }

    /**
     * 调用工具版 - method
     */
    @GetMapping("/chat-tool-method")
    public String chatTranslateMethod(@RequestParam(value = "query", defaultValue = "请告诉我现在北京时间几点了") String query) {
        return dashScopeChatClient.prompt(query).tools(new TimeTools()).call().content();
    }

}
```

提供无工具版、工具版接口

#### 效果展示

无工具版

![](/img/blog/spring-ai-tool-calling/time.png)

工具版 - function

![](/img/blog/spring-ai-tool-calling/time-function.png)

工具版 - method

![](/img/blog/spring-ai-tool-calling/time-method.png)

### 天气预测

#### WeatherAutoConfiguration

```python
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

@Configuration
@ConditionalOnClass(WeatherService.class)
@EnableConfigurationProperties(WeatherProperties.class)
@ConditionalOnProperty(prefix = "spring.ai.toolcalling.weather", name = "enabled", havingValue = "true")
public class WeatherAutoConfiguration {

    @Bean(name = "getWeatherFunction")
    @ConditionalOnMissingBean
    @Description("Use api.weather to get weather information.")
    public WeatherService getWeatherServiceFunction(WeatherProperties properties) {
        return new WeatherService(properties);
    }

}
```

1. `@Configuration`：定义为配置类
2. `@ConditionalOnClass({WeatherService.class})`：只有当类路径中存在WeatherService类才会加载该配置类
3. `@ConditionalOnProperty(prefix = "spring.ai.toolcalling.weather", name = "enabled", havingValue = "true")`：只有当配置文件处spring.ai.toolcalling.weather.enabled值为true时才会加载该配置类
4. `@Bean(name = "getWeatherFunction")`：定义该Bean名称为getWeatherFunction，并注册到Spring容器中
5. `@ConditionalOnMissingBean`：只有当Spring容器中不存在GetCurrentTimeByTimeZoneIdService类型的Bean时，才会创建该Bean
6. `@EnableConfigurationProperties(WeatherProperties.class)`：启用对WeatherProperties类的配置属性支持

#### WeatherProperties

```Java
@ConfigurationProperties(prefix = "spring.ai.toolcalling.weather")
public class WeatherProperties {

    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

}
```

从配置文件中获取apiKey

#### WeatherService

```Java
import cn.hutool.extra.pinyin.PinyinUtil;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class WeatherService implements Function<WeatherService.Request, WeatherService.Response> {

    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);

    private static final String WEATHER_API_URL = "https://api.weatherapi.com/v1/forecast.json";

    private final WebClient webClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public WeatherService(WeatherProperties properties) {
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .defaultHeader("key", properties.getApiKey())
                .build();
    }

    public static Response fromJson(Map<String, Object> json) {
        Map<String, Object> location = (Map<String, Object>) json.get("location");
        Map<String, Object> current = (Map<String, Object>) json.get("current");
        Map<String, Object> forecast = (Map<String, Object>) json.get("forecast");
        List<Map<String, Object>> forecastDays = (List<Map<String, Object>>) forecast.get("forecastday");
        String city = (String) location.get("name");
        return new Response(city, current, forecastDays);
    }

    @Override
    public Response apply(Request request) {
        if (request == null || !StringUtils.hasText(request.city())) {
            logger.error("Invalid request: city is required.");
            return null;
        }
        String location = preprocessLocation(request.city());
        String url = UriComponentsBuilder.fromHttpUrl(WEATHER_API_URL)
                .queryParam("q", location)
                .queryParam("days", request.days())
                .toUriString();
        try {
            Mono<String> responseMono = webClient.get().uri(url).retrieve().bodyToMono(String.class);
            String jsonResponse = responseMono.block();
            assert jsonResponse != null;

            Response response = fromJson(objectMapper.readValue(jsonResponse, new TypeReference<Map<String, Object>>() {
            }));
            logger.info("Weather data fetched successfully for city: {}", response.city());
            return response;
        }
        catch (Exception e) {
            logger.error("Failed to fetch weather data: {}", e.getMessage());
            return null;
        }
    }

    // Use the tools in hutool to convert Chinese place names into pinyin
    private String preprocessLocation(String location) {
        if (containsChinese(location)) {
            return PinyinUtil.getPinyin(location, "");
        }
        return location;
    }

    private boolean containsChinese(String str) {
        return str.matches(".*[\u4e00-\u9fa5].*");
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonClassDescription("Weather Service API request")
    public record Request(
            @JsonProperty(required = true, value = "city") @JsonPropertyDescription("city name") String city,

            @JsonProperty(required = true,
                    value = "days") @JsonPropertyDescription("Number of days of weather forecast. Value ranges from 1 to 14") int days) {
    }

    @JsonClassDescription("Weather Service API response")
    public record Response(
            @JsonProperty(required = true, value = "city") @JsonPropertyDescription("city name") String city,
            @JsonProperty(required = true, value = "current") @JsonPropertyDescription("Current weather info") Map<String, Object> current,
            @JsonProperty(required = true, value = "forecastDays") @JsonPropertyDescription("Forecast weather info")
            List<Map<String, Object>> forecastDays) {
    }

}
```

1. 实现Function接口，重写apply方法，确保输入为Request、输出为Response
2. Request类为记录类，需要添加方法描述（JsonClassDescription） + 参数描述（JsonProperty），主要用于让模型提取出对应的输入参数city、days
3. 先尝试调通所用API接口，查看数据格式，根据需要取对应的返回数据（这里可以不用全取，token太多模型返回有点慢）

#### WeatherTools（单独实现MethodToolCallback版）

```Java
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public class WeatherTools {

    private static final Logger logger = LoggerFactory.getLogger(WeatherTools.class);

    private static final String WEATHER_API_URL = "https://api.weatherapi.com/v1/forecast.json";

    private final WebClient webClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public WeatherTools(WeatherProperties properties) {
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .defaultHeader("key", properties.getApiKey())
                .build();
    }

    @Tool(description = "Use api.weather to get weather information.")
    public Response getWeatherServiceMethod(@ToolParam(description = "City name") String city,
                                            @ToolParam(description = "Number of days of weather forecast. Value ranges from 1 to 14") int days) {

        if (!StringUtils.hasText(city)) {
            logger.error("Invalid request: city is required.");
            return null;
        }
        String location = WeatherUtils.preprocessLocation(city);
        String url = UriComponentsBuilder.fromHttpUrl(WEATHER_API_URL)
                .queryParam("q", location)
                .queryParam("days", days)
                .toUriString();
        logger.info("url : {}", url);
        try {
            Mono<String> responseMono = webClient.get().uri(url).retrieve().bodyToMono(String.class);
            String jsonResponse = responseMono.block();
            assert jsonResponse != null;

            Response response = fromJson(objectMapper.readValue(jsonResponse, new TypeReference<Map<String, Object>>() {
            }));
            logger.info("Weather data fetched successfully for city: {}", response.city());
            return response;
        } catch (Exception e) {
            logger.error("Failed to fetch weather data: {}", e.getMessage());
            return null;
        }
    }

    public static Response fromJson(Map<String, Object> json) {
        Map<String, Object> location = (Map<String, Object>) json.get("location");
        Map<String, Object> current = (Map<String, Object>) json.get("current");
        Map<String, Object> forecast = (Map<String, Object>) json.get("forecast");
        List<Map<String, Object>> forecastDays = (List<Map<String, Object>>) forecast.get("forecastday");
        String city = (String) location.get("name");
        return new Response(city, current, forecastDays);
    }

    public record Response(String city, Map<String, Object> current, List<Map<String, Object>> forecastDays) {
    }


}
```

#### WeatherController

```Java
import com.yingzi.toolCalling.component.weather.WeatherProperties;
import com.yingzi.toolCalling.component.weather.WeatherTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/weather")
public class WeatherController {

    private final ChatClient dashScopeChatClient;

    private final WeatherProperties weatherProperties;


    public WeatherController(ChatClient.Builder chatClientBuilder, WeatherProperties weatherProperties) {
        this.dashScopeChatClient = chatClientBuilder.build();
        this.weatherProperties = weatherProperties;
    }

    /**
     * 无工具版
     */
    @GetMapping("/chat")
    public String simpleChat(@RequestParam(value = "query", defaultValue = "请告诉我北京1天以后的天气") String query) {
        return dashScopeChatClient.prompt(query).call().content();
    }

    /**
     * 调用工具版 - function
     */
    @GetMapping("/chat-tool-function")
    public String chatTranslateFunction(@RequestParam(value = "query", defaultValue = "请告诉我北京1天以后的天气") String query) {
        return dashScopeChatClient.prompt(query).tools("getWeatherFunction").call().content();
    }

    /**
     * 调用工具版 - method
     */
    @GetMapping("/chat-tool-method")
    public String chatTranslateMethod(@RequestParam(value = "query", defaultValue = "请告诉我北京1天以后的天气") String query) {
        return dashScopeChatClient.prompt(query).tools(new WeatherTools(weatherProperties)).call().content();
    }
}
```

#### 效果展示

无工具版

![](/img/blog/spring-ai-tool-calling/weather.png)

工具版 - function

![](/img/blog/spring-ai-tool-calling/weather-function.png)

工具版 - method

![](/img/blog/spring-ai-tool-calling/weather-method.png)

### 百度翻译

#### BaidutranslateAutoConfiguration

```python
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

@Configuration
@ConditionalOnClass(BaidutranslateService.class)
@EnableConfigurationProperties(BaidutranslateProperties.class)
@ConditionalOnProperty(prefix = "spring.ai.toolcalling.baidutranslate", name = "enabled", havingValue = "true")
public class BaidutranslateAutoConfiguration {

    @Bean(name = "baiduTranslateFunction")
    @ConditionalOnMissingBean
    @Description("Baidu translation function for general text translation")
    public BaidutranslateService baiduTranslateFunction(BaidutranslateProperties properties) {
        return new BaidutranslateService(properties);
    }

}
```

同上

#### BaidutranslateProperties

```Java
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.ai.toolcalling.baidutranslate")
public class BaidutranslateProperties {

    private String appId;

    private String secretKey;

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

}
```

同上

#### BaidutranslateService

```java
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.DigestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

public class BaidutranslateService implements Function<BaidutranslateService.Request, BaidutranslateService.Response> {

    private static final Logger logger = LoggerFactory.getLogger(BaidutranslateService.class);

    private static final String TRANSLATE_HOST_URL = "https://fanyi-api.baidu.com/api/trans/vip/translate";

    private static final Random random = new Random();

    private final String appId;

    private final String secretKey;

    private final WebClient webClient;

    public BaidutranslateService(BaidutranslateProperties properties) {
        assert StringUtils.hasText(properties.getAppId());
        this.appId = properties.getAppId();
        assert StringUtils.hasText(properties.getSecretKey());
        this.secretKey = properties.getSecretKey();

        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .build();
    }

    @Override
    public Response apply(Request request) {
        if (request == null || !StringUtils.hasText(request.q) || !StringUtils.hasText(request.from)
                || !StringUtils.hasText(request.to)) {
            return null;
        }
        String salt = String.valueOf(random.nextInt(100000));
        String sign = DigestUtils.md5DigestAsHex((appId + request.q + salt + secretKey).getBytes());
        String url = UriComponentsBuilder.fromHttpUrl(TRANSLATE_HOST_URL).toUriString();
        try {
            MultiValueMap<String, String> body = constructRequestBody(request, salt, sign);
            Mono<String> responseMono = webClient.post().uri(url).bodyValue(body).retrieve().bodyToMono(String.class);

            String responseData = responseMono.block();
            assert responseData != null;
            logger.info("Translation request: {}, response: {}", request.q, responseData);

            return parseResponse(responseData);

        }
        catch (Exception e) {
            logger.error("Failed to invoke translate API due to: {}", e.getMessage());
            return null;
        }
    }

    private MultiValueMap<String, String> constructRequestBody(Request request, String salt, String sign) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("q", request.q);
        body.add("from", request.from);
        body.add("to", request.to);
        body.add("appid", appId);
        body.add("salt", salt);
        body.add("sign", sign);
        return body;
    }

    private Response parseResponse(String responseData) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, String> translations = new HashMap<>();
            TranslationResponse responseList = mapper.readValue(responseData, TranslationResponse.class);
            String to = responseList.to;
            List<TranslationResult> translationsList = responseList.trans_result;
            if (translationsList != null) {
                for (TranslationResult translation : translationsList) {
                    String translatedText = translation.dst;
                    translations.put(to, translatedText);
                    logger.info("Translated text to {}: {}", to, translatedText);
                }
            }
            return new Response(translations);
        }
        catch (Exception e) {
            try {
                Map<String, String> responseList = mapper.readValue(responseData,
                        mapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));
                logger.info(
                        "Translation exception, please inquire Baidu translation api documentation to info error_code:{}",
                        responseList);
                return new Response(responseList);
            }
            catch (Exception ex) {
                logger.error("Failed to parse json due to: {}", ex.getMessage());
                return null;
            }
        }
    }

    @JsonClassDescription("Request to translate text to a target language")
    public record Request(
            @JsonProperty(required = true,
                    value = "q") @JsonPropertyDescription("Content that needs to be translated") String q,
            @JsonProperty(required = true,
                    value = "from") @JsonPropertyDescription("Source language that needs to be translated") String from,
            @JsonProperty(required = true,
                    value = "to") @JsonPropertyDescription("Target language to translate into") String to) {
    }

    @JsonClassDescription("Response to translate text to a target language")
    public record Response(Map<String, String> translatedTexts) {
    }

    @JsonClassDescription("part of the response")
    public record TranslationResult(
            @JsonProperty(required = true, value = "src") @JsonPropertyDescription("Original Content") String src,
            @JsonProperty(required = true, value = "dst") @JsonPropertyDescription("Final Result") String dst) {
    }

    @JsonClassDescription("complete response")
    public record TranslationResponse(
            @JsonProperty(required = true,
                    value = "from") @JsonPropertyDescription("Source language that needs to be translated") String from,
            @JsonProperty(required = true,
                    value = "to") @JsonPropertyDescription("Target language to translate into") String to,
            @JsonProperty(required = true,
                    value = "trans_result") @JsonPropertyDescription("part of the response") List<TranslationResult> trans_result) {
    }

}
```

1. Request中指定三个参数
   1. q：什么内容需要被翻译
   2. from：源内容需要什么语言
   3. to：目标内容为什么语言
2. 这里对翻译API返回的字段类型做了更加详细的描述

#### BaidutranslateTools（单独实现MethodToolCallback版）

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.HttpHeaders;
import org.springframework.util.DigestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BaidutranslateTools {

    private static final Logger logger = LoggerFactory.getLogger(BaidutranslateTools.class);

    private static final String TRANSLATE_HOST_URL = "https://fanyi-api.baidu.com/api/trans/vip/translate";

    private static final Random random = new Random();
    private final WebClient webClient;
    private final String appId;
    private final String secretKey;

    public BaidutranslateTools(BaidutranslateProperties properties) {
        assert StringUtils.hasText(properties.getAppId());
        this.appId = properties.getAppId();
        assert StringUtils.hasText(properties.getSecretKey());
        this.secretKey = properties.getSecretKey();

        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .build();
    }

    @Tool(description = "Baidu translation function for general text translation")
    public Map<String, String> baiduTranslateMethod(@ToolParam(description = "Content that needs to be translated") String q,
                                                      @ToolParam(description = "Source language that needs to be translated") String from,
                                                      @ToolParam(description = "Target language to translate into") String to) {
        if (!StringUtils.hasText(q) || !StringUtils.hasText(from)
                || !StringUtils.hasText(to)) {
            return null;
        }
        String salt = String.valueOf(random.nextInt(100000));
        String sign = DigestUtils.md5DigestAsHex((appId + q + salt + secretKey).getBytes());
        String url = UriComponentsBuilder.fromHttpUrl(TRANSLATE_HOST_URL).toUriString();
        try {
            MultiValueMap<String, String> body = constructRequestBody(q, from, to, salt, sign);
            Mono<String> responseMono = webClient.post().uri(url).bodyValue(body).retrieve().bodyToMono(String.class);

            String responseData = responseMono.block();
            assert responseData != null;
            logger.info("Translation request: {}, response: {}", q, responseData);

            return parseResponse(responseData);

        }
        catch (Exception e) {
            logger.error("Failed to invoke translate API due to: {}", e.getMessage());
            return null;
        }
    }

    private MultiValueMap<String, String> constructRequestBody(String q, String from, String to, String salt, String sign) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("q", q);
        body.add("from", from);
        body.add("to", to);
        body.add("appid", appId);
        body.add("salt", salt);
        body.add("sign", sign);
        return body;
    }

    private Map<String, String> parseResponse(String responseData) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, String> translations = new HashMap<>();
            TranslationResponse responseList = mapper.readValue(responseData, TranslationResponse.class);
            String to = responseList.to;
            List<TranslationResult> translationsList = responseList.trans_result;
            if (translationsList != null) {
                for (TranslationResult translation : translationsList) {
                    String translatedText = translation.dst;
                    translations.put(to, translatedText);
                    logger.info("Translated text to {}: {}", to, translatedText);
                }
            }
            return translations;
        }
        catch (Exception e) {
            try {
                Map<String, String> responseList = mapper.readValue(responseData,
                        mapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));
                logger.info(
                        "Translation exception, please inquire Baidu translation api documentation to info error_code:{}",
                        responseList);
                return responseList;
            }
            catch (Exception ex) {
                logger.error("Failed to parse json due to: {}", ex.getMessage());
                return null;
            }
        }
    }

    public record TranslationResult(String src, String dst) {
    }

    public record TranslationResponse(String from, String to, List<TranslationResult> trans_result) {
    }
}
```

#### TranslateController

```Java
import com.yingzi.toolCalling.component.baidutranslate.BaidutranslateProperties;
import com.yingzi.toolCalling.component.baidutranslate.BaidutranslateTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/translate")
public class TranslateController {

    private final ChatClient dashScopeChatClient;
    private final BaidutranslateProperties baidutranslateProperties;


    public TranslateController(ChatClient.Builder chatClientBuilder, BaidutranslateProperties baidutranslateProperties) {
        this.dashScopeChatClient = chatClientBuilder.build();
        this.baidutranslateProperties = baidutranslateProperties;
    }

    /**
     * 无工具版
     */
    @GetMapping("/chat")
    public String simpleChat(@RequestParam(value = "query", defaultValue = "帮我把以下内容翻译成英文：你好，世界。") String query) {
        return dashScopeChatClient.prompt(query).call().content();
    }

    /**
     * 调用工具版 - function
     */
    @GetMapping("/chat-tool-function")
    public String chatTranslateFunction(@RequestParam(value = "query", defaultValue = "帮我把以下内容翻译成英文：你好，世界。") String query) {
        return dashScopeChatClient.prompt(query).tools("baiduTranslateFunction").call().content();
    }

    /**
     * 调用工具版 - method
     */
    @GetMapping("/chat-tool-method")
    public String chatTranslateMethod(@RequestParam(value = "query", defaultValue = "帮我把以下内容翻译成英文：你好，世界。") String query) {
        // 从配置文件中，获取，自动加载
        return dashScopeChatClient.prompt(query).tools(new BaidutranslateTools(baidutranslateProperties)).call().content();
    }

}
```

#### 效果展示

无工具版

![](/img/blog/spring-ai-tool-calling/translate.png)

工具版 - function

![](/img/blog/spring-ai-tool-calling/translate-function.png)

工具版 - method

![](/img/blog/spring-ai-tool-calling/translate-method.png)

## 参考资料

https://docs.spring.io/spring-ai/reference/api/tools.html#_quick_start

https://docs.spring.io/spring-ai/reference/api/tools-migration.html

[Spring AI 框架在升级，Function Calling 废弃，被 Tool Calling 取代](https://juejin.cn/post/7470423971310436390)

https://mp.weixin.qq.com/s/kcQ1lifA8oH2ee16QxDYYg

Spring-ai-alibaba项目下tool工具模块

- spring-ai-alibaba-starter-tool-calling-time
- spring-ai-alibaba-starter-tool-calling-weather
- spring-ai-alibaba-starter-tool-calling-baidutranslate