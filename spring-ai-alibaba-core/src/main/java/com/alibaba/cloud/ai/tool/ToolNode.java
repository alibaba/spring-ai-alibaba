package com.alibaba.cloud.ai.tool;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static dev.langchain4j.agent.tool.ToolSpecifications.toolSpecificationFrom;

@Slf4j
public final class ToolNode {

    @Value
    @Accessors( fluent = true)
    static class Specification {
        @NonNull
        ToolSpecification value;
        @NonNull
        ToolExecutor executor;


        public Specification(@NonNull Object objectWithTool, @NonNull Method method ) {
            this.value = toolSpecificationFrom(method);
            this.executor = new DefaultToolExecutor(objectWithTool, method);
        }
    }

    public static ToolNode of( Collection<Object> objectsWithTools) {

        List<Specification> toolSpecifications = new ArrayList<>();

        for (Object objectWithTool : objectsWithTools ) {
            for (Method method : objectWithTool.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(Tool.class)) {
                    toolSpecifications.add( new Specification( objectWithTool, method));
                }
            }
        }
        return new ToolNode(toolSpecifications);
    }

    public static ToolNode of(Object ...objectsWithTools) {
        return of( Arrays.asList(objectsWithTools) );
    }


    private final List<Specification> entries;

    private ToolNode(@NonNull List<Specification> entries) {
        if( entries.isEmpty() ) {
            throw new IllegalArgumentException("entries cannot be empty!");
        }
        this.entries = entries;
    }

    public List<ToolSpecification> toolSpecifications() {
        return this.entries.stream()
                .map(Specification::value)
                .collect(Collectors.toList());
    }

    public Optional<ToolExecutionResultMessage> execute( @NonNull ToolExecutionRequest request, Object memoryId ) {
        log.trace( "execute: {}", request.name() );

        return entries.stream()
                .filter( v -> v.value().name().equals(request.name()))
                .findFirst()
                .map( e -> {
                    String value = e.executor().execute(request, memoryId);
                    return new ToolExecutionResultMessage( request.id(), request.name(), value );
                })
                ;
    }

    public Optional<ToolExecutionResultMessage> execute(@NonNull Collection<ToolExecutionRequest> requests, Object memoryId ) {
        for( ToolExecutionRequest request : requests ) {

            Optional<ToolExecutionResultMessage> result = execute( request, memoryId );

            if( result.isPresent() ) {
                return result;
            }
        }
        return Optional.empty();
    }

    public Optional<ToolExecutionResultMessage> execute( ToolExecutionRequest request ) {
        return execute( request, null );
    }

    public Optional<ToolExecutionResultMessage> execute( Collection<ToolExecutionRequest> requests ) {
        return execute( requests, null );
    }

}
