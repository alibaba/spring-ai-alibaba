package com.alibaba.cloud.ai.graph.agent.tool;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;

import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * 工具接口，定义工具方法，支持中断的方法，流式输出方法以及默认值返回方法。
 * ToolContext中必须要有agent_name的值，否则会报错
 *
 * @author vlsmb
 * @since 2025/10/25
 */
public interface BaseTool<T, R> extends BiFunction<T, ToolContext, R> {

    String AGENT_NAME = "agent_name";

    /**
     * 工具方法，已经在AbstractTool中实现
     * @param t 输入参数
     * @param toolContext 工具上下文
     * @return 输出结果
     */
    R apply(T t, ToolContext toolContext);

    default R applyInterruptible(T t, ToolContext toolContext) throws Exception {
        return applyInterruptible(t, toolContext, null);
    }

    /**
     * 允许中断的apply方法，实现类需要需要实现此方法
     * @param t 输入参数
     * @param toolContext 工具上下文
     * @param streamConsumer 每一个元素的Consumer（如果工具不支持流式可以置null）
     * @return 输出结果
     */
    R applyInterruptible(T t, ToolContext toolContext, Consumer<? super R> streamConsumer) throws Exception;

    /**
     * 返回包装后的ToolCallback
     */
    ToolCallback toolCallback();

    /**
     * 流式方法，如果工具支持则需要重写
     * @param t 输入参数
     * @param toolContext 工具上下文
     * @return 最终结果
     */
    default Flux<R> applyStream(T t, ToolContext toolContext) {
        throw new UnsupportedOperationException("Not supported");
    }

    /**
     * 失败后返回的默认值
     * @param t 输入参数
     * @param toolContext 工具上下文
     * @param throwable 异常，如果为CancellationException则表示工具被取消
     * @return 默认值
     */
    R fallback(T t, ToolContext toolContext, Throwable throwable);

    void cancel(ToolContext toolContext);

    boolean isCancelled(ToolContext toolContext);

    default String getAgentName(ToolContext toolContext) {
        Object obj = toolContext.getContext().get(AGENT_NAME);
        if(!(obj instanceof String agentName)) {
            throw new IllegalArgumentException("agent_name must be a string");
        }
        return agentName;
    }
}
