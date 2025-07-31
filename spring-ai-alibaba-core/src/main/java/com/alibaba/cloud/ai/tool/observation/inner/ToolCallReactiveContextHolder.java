package com.alibaba.cloud.ai.tool.observation.inner;

import reactor.util.context.Context;
import reactor.util.context.ContextView;

/**
 * Copied from <a
 * href=https://github.com/spring-projects/spring-ai/blob/main/spring-ai-model/src/main/java/org/springframework/ai/model/tool/internal/ToolCallReactiveContextHolder.java>upstream
 * (1.0.0)</a>. <br/>
 * This class bridges blocking Tools call and the reactive context. When calling tools, it
 * captures the context in a thread local, making it available to re-inject in a nested
 * reactive call.
 *
 * @author Daniel Garnier-Moiroux
 * @since 1.1.0
 */
public class ToolCallReactiveContextHolder {

	private static final ThreadLocal<ContextView> context = ThreadLocal.withInitial(Context::empty);

	public static void setContext(ContextView contextView) {
		context.set(contextView);
	}

	public static ContextView getContext() {
		return context.get();
	}

	public static void clearContext() {
		context.remove();
	}

}
