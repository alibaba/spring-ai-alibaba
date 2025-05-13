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
declare module 'astro:content' {
	interface Render {
		'.mdx': Promise<{
			Content: import('astro').MarkdownInstance<{}>['Content'];
			headings: import('astro').MarkdownHeading[];
			remarkPluginFrontmatter: Record<string, any>;
		}>;
	}
}

declare module 'astro:content' {
	interface Render {
		'.md': Promise<{
			Content: import('astro').MarkdownInstance<{}>['Content'];
			headings: import('astro').MarkdownHeading[];
			remarkPluginFrontmatter: Record<string, any>;
		}>;
	}
}

declare module 'astro:content' {
	type Flatten<T> = T extends { [K: string]: infer U } ? U : never;

	export type CollectionKey = keyof AnyEntryMap;
	export type CollectionEntry<C extends CollectionKey> = Flatten<AnyEntryMap[C]>;

	export type ContentCollectionKey = keyof ContentEntryMap;
	export type DataCollectionKey = keyof DataEntryMap;

	type AllValuesOf<T> = T extends any ? T[keyof T] : never;
	type ValidContentEntrySlug<C extends keyof ContentEntryMap> = AllValuesOf<
		ContentEntryMap[C]
	>['slug'];

	export function getEntryBySlug<
		C extends keyof ContentEntryMap,
		E extends ValidContentEntrySlug<C> | (string & {}),
	>(
		collection: C,
		// Note that this has to accept a regular string too, for SSR
		entrySlug: E
	): E extends ValidContentEntrySlug<C>
		? Promise<CollectionEntry<C>>
		: Promise<CollectionEntry<C> | undefined>;

	export function getDataEntryById<C extends keyof DataEntryMap, E extends keyof DataEntryMap[C]>(
		collection: C,
		entryId: E
	): Promise<CollectionEntry<C>>;

	export function getCollection<C extends keyof AnyEntryMap, E extends CollectionEntry<C>>(
		collection: C,
		filter?: (entry: CollectionEntry<C>) => entry is E
	): Promise<E[]>;
	export function getCollection<C extends keyof AnyEntryMap>(
		collection: C,
		filter?: (entry: CollectionEntry<C>) => unknown
	): Promise<CollectionEntry<C>[]>;

	export function getEntry<
		C extends keyof ContentEntryMap,
		E extends ValidContentEntrySlug<C> | (string & {}),
	>(entry: {
		collection: C;
		slug: E;
	}): E extends ValidContentEntrySlug<C>
		? Promise<CollectionEntry<C>>
		: Promise<CollectionEntry<C> | undefined>;
	export function getEntry<
		C extends keyof DataEntryMap,
		E extends keyof DataEntryMap[C] | (string & {}),
	>(entry: {
		collection: C;
		id: E;
	}): E extends keyof DataEntryMap[C]
		? Promise<DataEntryMap[C][E]>
		: Promise<CollectionEntry<C> | undefined>;
	export function getEntry<
		C extends keyof ContentEntryMap,
		E extends ValidContentEntrySlug<C> | (string & {}),
	>(
		collection: C,
		slug: E
	): E extends ValidContentEntrySlug<C>
		? Promise<CollectionEntry<C>>
		: Promise<CollectionEntry<C> | undefined>;
	export function getEntry<
		C extends keyof DataEntryMap,
		E extends keyof DataEntryMap[C] | (string & {}),
	>(
		collection: C,
		id: E
	): E extends keyof DataEntryMap[C]
		? Promise<DataEntryMap[C][E]>
		: Promise<CollectionEntry<C> | undefined>;

	/** Resolve an array of entry references from the same collection */
	export function getEntries<C extends keyof ContentEntryMap>(
		entries: {
			collection: C;
			slug: ValidContentEntrySlug<C>;
		}[]
	): Promise<CollectionEntry<C>[]>;
	export function getEntries<C extends keyof DataEntryMap>(
		entries: {
			collection: C;
			id: keyof DataEntryMap[C];
		}[]
	): Promise<CollectionEntry<C>[]>;

	export function reference<C extends keyof AnyEntryMap>(
		collection: C
	): import('astro/zod').ZodEffects<
		import('astro/zod').ZodString,
		C extends keyof ContentEntryMap
			? {
					collection: C;
					slug: ValidContentEntrySlug<C>;
				}
			: {
					collection: C;
					id: keyof DataEntryMap[C];
				}
	>;
	// Allow generic `string` to avoid excessive type errors in the config
	// if `dev` is not running to update as you edit.
	// Invalid collection names will be caught at build time.
	export function reference<C extends string>(
		collection: C
	): import('astro/zod').ZodEffects<import('astro/zod').ZodString, never>;

	type ReturnTypeOrOriginal<T> = T extends (...args: any[]) => infer R ? R : T;
	type InferEntrySchema<C extends keyof AnyEntryMap> = import('astro/zod').infer<
		ReturnTypeOrOriginal<Required<ContentConfig['collections'][C]>['schema']>
	>;

	type ContentEntryMap = {
		"blog": {
"google-a2a-protocol.md": {
	id: "google-a2a-protocol.md";
  slug: "google-a2a-protocol";
  body: string;
  collection: "blog";
  data: InferEntrySchema<"blog">
} & { render(): Render[".md"] };
"news/attend-a-meeting.md": {
	id: "news/attend-a-meeting.md";
  slug: "news/attend-a-meeting";
  body: string;
  collection: "blog";
  data: InferEntrySchema<"blog">
} & { render(): Render[".md"] };
"news/meetup-2024-11-26-shanghai.md": {
	id: "news/meetup-2024-11-26-shanghai.md";
  slug: "news/meetup-2024-11-26-shanghai";
  body: string;
  collection: "blog";
  data: InferEntrySchema<"blog">
} & { render(): Render[".md"] };
"news/spring-ai-alibaba-atom-programming-contest.md": {
	id: "news/spring-ai-alibaba-atom-programming-contest.md";
  slug: "news/spring-ai-alibaba-atom-programming-contest";
  body: string;
  collection: "blog";
  data: InferEntrySchema<"blog">
} & { render(): Render[".md"] };
"spring-ai-alibaba-graph-preview.md": {
	id: "spring-ai-alibaba-graph-preview.md";
  slug: "spring-ai-alibaba-graph-preview";
  body: string;
  collection: "blog";
  data: InferEntrySchema<"blog">
} & { render(): Render[".md"] };
"spring-ai-alibaba-introduction.md": {
	id: "spring-ai-alibaba-introduction.md";
  slug: "spring-ai-alibaba-introduction";
  body: string;
  collection: "blog";
  data: InferEntrySchema<"blog">
} & { render(): Render[".md"] };
"spring-ai-alibaba-mcp-filesystem.md": {
	id: "spring-ai-alibaba-mcp-filesystem.md";
  slug: "spring-ai-alibaba-mcp-filesystem";
  body: string;
  collection: "blog";
  data: InferEntrySchema<"blog">
} & { render(): Render[".md"] };
"spring-ai-alibaba-mcp-streamable-http.md": {
	id: "spring-ai-alibaba-mcp-streamable-http.md";
  slug: "spring-ai-alibaba-mcp-streamable-http";
  body: string;
  collection: "blog";
  data: InferEntrySchema<"blog">
} & { render(): Render[".md"] };
"spring-ai-alibaba-mcp.md": {
	id: "spring-ai-alibaba-mcp.md";
  slug: "spring-ai-alibaba-mcp";
  body: string;
  collection: "blog";
  data: InferEntrySchema<"blog">
} & { render(): Render[".md"] };
"spring-ai-alibaba-module-rag.md": {
	id: "spring-ai-alibaba-module-rag.md";
  slug: "spring-ai-alibaba-module-rag";
  body: string;
  collection: "blog";
  data: InferEntrySchema<"blog">
} & { render(): Render[".md"] };
"spring-ai-alibaba-observability-arms.md": {
	id: "spring-ai-alibaba-observability-arms.md";
  slug: "spring-ai-alibaba-observability-arms";
  body: string;
  collection: "blog";
  data: InferEntrySchema<"blog">
} & { render(): Render[".md"] };
"spring-ai-alibaba-ollama-deepseek.md": {
	id: "spring-ai-alibaba-ollama-deepseek.md";
  slug: "spring-ai-alibaba-ollama-deepseek";
  body: string;
  collection: "blog";
  data: InferEntrySchema<"blog">
} & { render(): Render[".md"] };
"spring-ai-alibaba-ollama-rag.md": {
	id: "spring-ai-alibaba-ollama-rag.md";
  slug: "spring-ai-alibaba-ollama-rag";
  body: string;
  collection: "blog";
  data: InferEntrySchema<"blog">
} & { render(): Render[".md"] };
"spring-ai-alibaba-openmanus.md": {
	id: "spring-ai-alibaba-openmanus.md";
  slug: "spring-ai-alibaba-openmanus";
  body: string;
  collection: "blog";
  data: InferEntrySchema<"blog">
} & { render(): Render[".md"] };
"spring-ai-alibaba-plugin.md": {
	id: "spring-ai-alibaba-plugin.md";
  slug: "spring-ai-alibaba-plugin";
  body: string;
  collection: "blog";
  data: InferEntrySchema<"blog">
} & { render(): Render[".md"] };
"spring-ai-alibaba-rag-ollama.md": {
	id: "spring-ai-alibaba-rag-ollama.md";
  slug: "spring-ai-alibaba-rag-ollama";
  body: string;
  collection: "blog";
  data: InferEntrySchema<"blog">
} & { render(): Render[".md"] };
"spring-ai-dynamic-prompt-nacos.md": {
	id: "spring-ai-dynamic-prompt-nacos.md";
  slug: "spring-ai-dynamic-prompt-nacos";
  body: string;
  collection: "blog";
  data: InferEntrySchema<"blog">
} & { render(): Render[".md"] };
"spring-ai-mcp-desc.md": {
	id: "spring-ai-mcp-desc.md";
  slug: "spring-ai-mcp-desc";
  body: string;
  collection: "blog";
  data: InferEntrySchema<"blog">
} & { render(): Render[".md"] };
"spring-ai-mcp-marketplace.md": {
	id: "spring-ai-mcp-marketplace.md";
  slug: "spring-ai-mcp-marketplace";
  body: string;
  collection: "blog";
  data: InferEntrySchema<"blog">
} & { render(): Render[".md"] };
"spring-ai-toolcalling.md": {
	id: "spring-ai-toolcalling.md";
  slug: "spring-ai-toolcalling";
  body: string;
  collection: "blog";
  data: InferEntrySchema<"blog">
} & { render(): Render[".md"] };
};
"docs": {
"1.0.0-M3.2/en/concepts.md": {
	id: "1.0.0-M3.2/en/concepts.md";
  slug: "100-m32/en/concepts";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M3.2/en/get-started.md": {
	id: "1.0.0-M3.2/en/get-started.md";
  slug: "100-m32/en/get-started";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M3.2/en/overview.md": {
	id: "1.0.0-M3.2/en/overview.md";
  slug: "100-m32/en/overview";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M3.2/en/practices/playground-flight-booking.md": {
	id: "1.0.0-M3.2/en/practices/playground-flight-booking.md";
  slug: "100-m32/en/practices/playground-flight-booking";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M3.2/en/practices/rag.md": {
	id: "1.0.0-M3.2/en/practices/rag.md";
  slug: "100-m32/en/practices/rag";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M3.2/en/tutorials/chat-client.md": {
	id: "1.0.0-M3.2/en/tutorials/chat-client.md";
  slug: "100-m32/en/tutorials/chat-client";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M3.2/en/tutorials/chat-model.md": {
	id: "1.0.0-M3.2/en/tutorials/chat-model.md";
  slug: "100-m32/en/tutorials/chat-model";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M3.2/en/tutorials/embedding.md": {
	id: "1.0.0-M3.2/en/tutorials/embedding.md";
  slug: "100-m32/en/tutorials/embedding";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M3.2/en/tutorials/function-calling.md": {
	id: "1.0.0-M3.2/en/tutorials/function-calling.md";
  slug: "100-m32/en/tutorials/function-calling";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M3.2/en/tutorials/memory.md": {
	id: "1.0.0-M3.2/en/tutorials/memory.md";
  slug: "100-m32/en/tutorials/memory";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M3.2/en/tutorials/prompt.md": {
	id: "1.0.0-M3.2/en/tutorials/prompt.md";
  slug: "100-m32/en/tutorials/prompt";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M3.2/en/tutorials/retriever.md": {
	id: "1.0.0-M3.2/en/tutorials/retriever.md";
  slug: "100-m32/en/tutorials/retriever";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M3.2/en/tutorials/structured-output.md": {
	id: "1.0.0-M3.2/en/tutorials/structured-output.md";
  slug: "100-m32/en/tutorials/structured-output";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M3.2/en/tutorials/vectorstore.md": {
	id: "1.0.0-M3.2/en/tutorials/vectorstore.md";
  slug: "100-m32/en/tutorials/vectorstore";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M3.2/zh-cn/concepts.md": {
	id: "1.0.0-M3.2/zh-cn/concepts.md";
  slug: "100-m32/zh-cn/concepts";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M3.2/zh-cn/get-started.md": {
	id: "1.0.0-M3.2/zh-cn/get-started.md";
  slug: "100-m32/zh-cn/get-started";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M3.2/zh-cn/overview.md": {
	id: "1.0.0-M3.2/zh-cn/overview.md";
  slug: "100-m32/zh-cn/overview";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M3.2/zh-cn/practices/memory.md": {
	id: "1.0.0-M3.2/zh-cn/practices/memory.md";
  slug: "100-m32/zh-cn/practices/memory";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M3.2/zh-cn/practices/playground-flight-booking.md": {
	id: "1.0.0-M3.2/zh-cn/practices/playground-flight-booking.md";
  slug: "100-m32/zh-cn/practices/playground-flight-booking";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M3.2/zh-cn/practices/rag.md": {
	id: "1.0.0-M3.2/zh-cn/practices/rag.md";
  slug: "100-m32/zh-cn/practices/rag";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M3.2/zh-cn/tutorials/chat-client.md": {
	id: "1.0.0-M3.2/zh-cn/tutorials/chat-client.md";
  slug: "100-m32/zh-cn/tutorials/chat-client";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M3.2/zh-cn/tutorials/chat-model.md": {
	id: "1.0.0-M3.2/zh-cn/tutorials/chat-model.md";
  slug: "100-m32/zh-cn/tutorials/chat-model";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M3.2/zh-cn/tutorials/embedding.md": {
	id: "1.0.0-M3.2/zh-cn/tutorials/embedding.md";
  slug: "100-m32/zh-cn/tutorials/embedding";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M3.2/zh-cn/tutorials/function-calling.md": {
	id: "1.0.0-M3.2/zh-cn/tutorials/function-calling.md";
  slug: "100-m32/zh-cn/tutorials/function-calling";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M3.2/zh-cn/tutorials/memory.md": {
	id: "1.0.0-M3.2/zh-cn/tutorials/memory.md";
  slug: "100-m32/zh-cn/tutorials/memory";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M3.2/zh-cn/tutorials/prompt.md": {
	id: "1.0.0-M3.2/zh-cn/tutorials/prompt.md";
  slug: "100-m32/zh-cn/tutorials/prompt";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M3.2/zh-cn/tutorials/retriever.md": {
	id: "1.0.0-M3.2/zh-cn/tutorials/retriever.md";
  slug: "100-m32/zh-cn/tutorials/retriever";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M3.2/zh-cn/tutorials/structured-output.md": {
	id: "1.0.0-M3.2/zh-cn/tutorials/structured-output.md";
  slug: "100-m32/zh-cn/tutorials/structured-output";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M3.2/zh-cn/tutorials/vectorstore.md": {
	id: "1.0.0-M3.2/zh-cn/tutorials/vectorstore.md";
  slug: "100-m32/zh-cn/tutorials/vectorstore";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/en/concepts.md": {
	id: "1.0.0-M5.1/en/concepts.md";
  slug: "100-m51/en/concepts";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/en/get-started.md": {
	id: "1.0.0-M5.1/en/get-started.md";
  slug: "100-m51/en/get-started";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/en/models/dashScope.md": {
	id: "1.0.0-M5.1/en/models/dashScope.md";
  slug: "100-m51/en/models/dashscope";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/en/models/deepseek.md": {
	id: "1.0.0-M5.1/en/models/deepseek.md";
  slug: "100-m51/en/models/deepseek";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/en/models/like-openAI.md": {
	id: "1.0.0-M5.1/en/models/like-openAI.md";
  slug: "100-m51/en/models/like-openai";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/en/models/ollama.md": {
	id: "1.0.0-M5.1/en/models/ollama.md";
  slug: "100-m51/en/models/ollama";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/en/models/openAI.md": {
	id: "1.0.0-M5.1/en/models/openAI.md";
  slug: "100-m51/en/models/openai";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/en/overview.md": {
	id: "1.0.0-M5.1/en/overview.md";
  slug: "100-m51/en/overview";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/en/practices/playground-flight-booking.md": {
	id: "1.0.0-M5.1/en/practices/playground-flight-booking.md";
  slug: "100-m51/en/practices/playground-flight-booking";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/en/practices/rag.md": {
	id: "1.0.0-M5.1/en/practices/rag.md";
  slug: "100-m51/en/practices/rag";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/en/tutorials/chat-client.md": {
	id: "1.0.0-M5.1/en/tutorials/chat-client.md";
  slug: "100-m51/en/tutorials/chat-client";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/en/tutorials/chat-model.md": {
	id: "1.0.0-M5.1/en/tutorials/chat-model.md";
  slug: "100-m51/en/tutorials/chat-model";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/en/tutorials/embedding.md": {
	id: "1.0.0-M5.1/en/tutorials/embedding.md";
  slug: "100-m51/en/tutorials/embedding";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/en/tutorials/function-calling.md": {
	id: "1.0.0-M5.1/en/tutorials/function-calling.md";
  slug: "100-m51/en/tutorials/function-calling";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/en/tutorials/mcp.md": {
	id: "1.0.0-M5.1/en/tutorials/mcp.md";
  slug: "100-m51/en/tutorials/mcp";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/en/tutorials/memory.md": {
	id: "1.0.0-M5.1/en/tutorials/memory.md";
  slug: "100-m51/en/tutorials/memory";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/en/tutorials/prompt.md": {
	id: "1.0.0-M5.1/en/tutorials/prompt.md";
  slug: "100-m51/en/tutorials/prompt";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/en/tutorials/rag.md": {
	id: "1.0.0-M5.1/en/tutorials/rag.md";
  slug: "100-m51/en/tutorials/rag";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/en/tutorials/retriever.md": {
	id: "1.0.0-M5.1/en/tutorials/retriever.md";
  slug: "100-m51/en/tutorials/retriever";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/en/tutorials/structured-output.md": {
	id: "1.0.0-M5.1/en/tutorials/structured-output.md";
  slug: "100-m51/en/tutorials/structured-output";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/en/tutorials/vectorstore.md": {
	id: "1.0.0-M5.1/en/tutorials/vectorstore.md";
  slug: "100-m51/en/tutorials/vectorstore";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/concepts.md": {
	id: "1.0.0-M5.1/zh-cn/concepts.md";
  slug: "100-m51/zh-cn/concepts";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/get-started.md": {
	id: "1.0.0-M5.1/zh-cn/get-started.md";
  slug: "100-m51/zh-cn/get-started";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/integrations/analyticdb.md": {
	id: "1.0.0-M5.1/zh-cn/integrations/analyticdb.md";
  slug: "100-m51/zh-cn/integrations/analyticdb";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/integrations/documentreader.md": {
	id: "1.0.0-M5.1/zh-cn/integrations/documentreader.md";
  slug: "100-m51/zh-cn/integrations/documentreader";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/integrations/tools.md": {
	id: "1.0.0-M5.1/zh-cn/integrations/tools.md";
  slug: "100-m51/zh-cn/integrations/tools";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/integrations/vectorstore.md": {
	id: "1.0.0-M5.1/zh-cn/integrations/vectorstore.md";
  slug: "100-m51/zh-cn/integrations/vectorstore";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/models/dashScope.md": {
	id: "1.0.0-M5.1/zh-cn/models/dashScope.md";
  slug: "100-m51/zh-cn/models/dashscope";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/models/deepseek.md": {
	id: "1.0.0-M5.1/zh-cn/models/deepseek.md";
  slug: "100-m51/zh-cn/models/deepseek";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/models/like-openAI.md": {
	id: "1.0.0-M5.1/zh-cn/models/like-openAI.md";
  slug: "100-m51/zh-cn/models/like-openai";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/models/ollama.md": {
	id: "1.0.0-M5.1/zh-cn/models/ollama.md";
  slug: "100-m51/zh-cn/models/ollama";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/models/openAI.md": {
	id: "1.0.0-M5.1/zh-cn/models/openAI.md";
  slug: "100-m51/zh-cn/models/openai";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/overview.md": {
	id: "1.0.0-M5.1/zh-cn/overview.md";
  slug: "100-m51/zh-cn/overview";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/practices/bailian/rag-agent.md": {
	id: "1.0.0-M5.1/zh-cn/practices/bailian/rag-agent.md";
  slug: "100-m51/zh-cn/practices/bailian/rag-agent";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/practices/bailian/rag-knowledge.md": {
	id: "1.0.0-M5.1/zh-cn/practices/bailian/rag-knowledge.md";
  slug: "100-m51/zh-cn/practices/bailian/rag-knowledge";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/practices/memory.md": {
	id: "1.0.0-M5.1/zh-cn/practices/memory.md";
  slug: "100-m51/zh-cn/practices/memory";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/practices/playground-flight-booking.md": {
	id: "1.0.0-M5.1/zh-cn/practices/playground-flight-booking.md";
  slug: "100-m51/zh-cn/practices/playground-flight-booking";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/practices/rag.md": {
	id: "1.0.0-M5.1/zh-cn/practices/rag.md";
  slug: "100-m51/zh-cn/practices/rag";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/practices/usecase/playground-flight-booking.md": {
	id: "1.0.0-M5.1/zh-cn/practices/usecase/playground-flight-booking.md";
  slug: "100-m51/zh-cn/practices/usecase/playground-flight-booking";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/tutorials/agent-agentic-patterns.md": {
	id: "1.0.0-M5.1/zh-cn/tutorials/agent-agentic-patterns.md";
  slug: "100-m51/zh-cn/tutorials/agent-agentic-patterns";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/tutorials/chat-client.md": {
	id: "1.0.0-M5.1/zh-cn/tutorials/chat-client.md";
  slug: "100-m51/zh-cn/tutorials/chat-client";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/tutorials/chat-model.md": {
	id: "1.0.0-M5.1/zh-cn/tutorials/chat-model.md";
  slug: "100-m51/zh-cn/tutorials/chat-model";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/tutorials/embedding.md": {
	id: "1.0.0-M5.1/zh-cn/tutorials/embedding.md";
  slug: "100-m51/zh-cn/tutorials/embedding";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/tutorials/function-calling.md": {
	id: "1.0.0-M5.1/zh-cn/tutorials/function-calling.md";
  slug: "100-m51/zh-cn/tutorials/function-calling";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/tutorials/mcp.md": {
	id: "1.0.0-M5.1/zh-cn/tutorials/mcp.md";
  slug: "100-m51/zh-cn/tutorials/mcp";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/tutorials/memory.md": {
	id: "1.0.0-M5.1/zh-cn/tutorials/memory.md";
  slug: "100-m51/zh-cn/tutorials/memory";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/tutorials/prompt.md": {
	id: "1.0.0-M5.1/zh-cn/tutorials/prompt.md";
  slug: "100-m51/zh-cn/tutorials/prompt";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/tutorials/rag.md": {
	id: "1.0.0-M5.1/zh-cn/tutorials/rag.md";
  slug: "100-m51/zh-cn/tutorials/rag";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/tutorials/retriever.md": {
	id: "1.0.0-M5.1/zh-cn/tutorials/retriever.md";
  slug: "100-m51/zh-cn/tutorials/retriever";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/tutorials/structured-output.md": {
	id: "1.0.0-M5.1/zh-cn/tutorials/structured-output.md";
  slug: "100-m51/zh-cn/tutorials/structured-output";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M5.1/zh-cn/tutorials/vectorstore.md": {
	id: "1.0.0-M5.1/zh-cn/tutorials/vectorstore.md";
  slug: "100-m51/zh-cn/tutorials/vectorstore";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/en/concepts.md": {
	id: "1.0.0-M6.1/en/concepts.md";
  slug: "100-m61/en/concepts";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/en/get-started.md": {
	id: "1.0.0-M6.1/en/get-started.md";
  slug: "100-m61/en/get-started";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/en/models/dashScope.md": {
	id: "1.0.0-M6.1/en/models/dashScope.md";
  slug: "100-m61/en/models/dashscope";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/en/models/deepseek.md": {
	id: "1.0.0-M6.1/en/models/deepseek.md";
  slug: "100-m61/en/models/deepseek";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/en/models/like-openAI.md": {
	id: "1.0.0-M6.1/en/models/like-openAI.md";
  slug: "100-m61/en/models/like-openai";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/en/models/ollama.md": {
	id: "1.0.0-M6.1/en/models/ollama.md";
  slug: "100-m61/en/models/ollama";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/en/models/openAI.md": {
	id: "1.0.0-M6.1/en/models/openAI.md";
  slug: "100-m61/en/models/openai";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/en/overview.md": {
	id: "1.0.0-M6.1/en/overview.md";
  slug: "100-m61/en/overview";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/en/practices/playground-flight-booking.md": {
	id: "1.0.0-M6.1/en/practices/playground-flight-booking.md";
  slug: "100-m61/en/practices/playground-flight-booking";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/en/practices/rag.md": {
	id: "1.0.0-M6.1/en/practices/rag.md";
  slug: "100-m61/en/practices/rag";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/en/tutorials/chat-client.md": {
	id: "1.0.0-M6.1/en/tutorials/chat-client.md";
  slug: "100-m61/en/tutorials/chat-client";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/en/tutorials/chat-model.md": {
	id: "1.0.0-M6.1/en/tutorials/chat-model.md";
  slug: "100-m61/en/tutorials/chat-model";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/en/tutorials/embedding.md": {
	id: "1.0.0-M6.1/en/tutorials/embedding.md";
  slug: "100-m61/en/tutorials/embedding";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/en/tutorials/function-calling.md": {
	id: "1.0.0-M6.1/en/tutorials/function-calling.md";
  slug: "100-m61/en/tutorials/function-calling";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/en/tutorials/mcp.md": {
	id: "1.0.0-M6.1/en/tutorials/mcp.md";
  slug: "100-m61/en/tutorials/mcp";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/en/tutorials/memory.md": {
	id: "1.0.0-M6.1/en/tutorials/memory.md";
  slug: "100-m61/en/tutorials/memory";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/en/tutorials/prompt.md": {
	id: "1.0.0-M6.1/en/tutorials/prompt.md";
  slug: "100-m61/en/tutorials/prompt";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/en/tutorials/rag.md": {
	id: "1.0.0-M6.1/en/tutorials/rag.md";
  slug: "100-m61/en/tutorials/rag";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/en/tutorials/retriever.md": {
	id: "1.0.0-M6.1/en/tutorials/retriever.md";
  slug: "100-m61/en/tutorials/retriever";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/en/tutorials/structured-output.md": {
	id: "1.0.0-M6.1/en/tutorials/structured-output.md";
  slug: "100-m61/en/tutorials/structured-output";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/en/tutorials/vectorstore.md": {
	id: "1.0.0-M6.1/en/tutorials/vectorstore.md";
  slug: "100-m61/en/tutorials/vectorstore";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/concepts.md": {
	id: "1.0.0-M6.1/zh-cn/concepts.md";
  slug: "100-m61/zh-cn/concepts";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/get-started.md": {
	id: "1.0.0-M6.1/zh-cn/get-started.md";
  slug: "100-m61/zh-cn/get-started";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/integrations/analyticdb.md": {
	id: "1.0.0-M6.1/zh-cn/integrations/analyticdb.md";
  slug: "100-m61/zh-cn/integrations/analyticdb";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/integrations/documentreader.md": {
	id: "1.0.0-M6.1/zh-cn/integrations/documentreader.md";
  slug: "100-m61/zh-cn/integrations/documentreader";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/integrations/tools.md": {
	id: "1.0.0-M6.1/zh-cn/integrations/tools.md";
  slug: "100-m61/zh-cn/integrations/tools";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/integrations/vectorstore.md": {
	id: "1.0.0-M6.1/zh-cn/integrations/vectorstore.md";
  slug: "100-m61/zh-cn/integrations/vectorstore";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/models/dashScope.md": {
	id: "1.0.0-M6.1/zh-cn/models/dashScope.md";
  slug: "100-m61/zh-cn/models/dashscope";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/models/deepseek.md": {
	id: "1.0.0-M6.1/zh-cn/models/deepseek.md";
  slug: "100-m61/zh-cn/models/deepseek";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/models/like-openAI.md": {
	id: "1.0.0-M6.1/zh-cn/models/like-openAI.md";
  slug: "100-m61/zh-cn/models/like-openai";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/models/ollama.md": {
	id: "1.0.0-M6.1/zh-cn/models/ollama.md";
  slug: "100-m61/zh-cn/models/ollama";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/models/openAI.md": {
	id: "1.0.0-M6.1/zh-cn/models/openAI.md";
  slug: "100-m61/zh-cn/models/openai";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/models/qwq.md": {
	id: "1.0.0-M6.1/zh-cn/models/qwq.md";
  slug: "100-m61/zh-cn/models/qwq";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/overview.md": {
	id: "1.0.0-M6.1/zh-cn/overview.md";
  slug: "100-m61/zh-cn/overview";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/practices/bailian/rag-agent.md": {
	id: "1.0.0-M6.1/zh-cn/practices/bailian/rag-agent.md";
  slug: "100-m61/zh-cn/practices/bailian/rag-agent";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/practices/bailian/rag-knowledge.md": {
	id: "1.0.0-M6.1/zh-cn/practices/bailian/rag-knowledge.md";
  slug: "100-m61/zh-cn/practices/bailian/rag-knowledge";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/practices/memory.md": {
	id: "1.0.0-M6.1/zh-cn/practices/memory.md";
  slug: "100-m61/zh-cn/practices/memory";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/practices/playground-flight-booking.md": {
	id: "1.0.0-M6.1/zh-cn/practices/playground-flight-booking.md";
  slug: "100-m61/zh-cn/practices/playground-flight-booking";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/practices/rag.md": {
	id: "1.0.0-M6.1/zh-cn/practices/rag.md";
  slug: "100-m61/zh-cn/practices/rag";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/practices/usecase/playground-flight-booking.md": {
	id: "1.0.0-M6.1/zh-cn/practices/usecase/playground-flight-booking.md";
  slug: "100-m61/zh-cn/practices/usecase/playground-flight-booking";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/tutorials/agent-agentic-patterns.md": {
	id: "1.0.0-M6.1/zh-cn/tutorials/agent-agentic-patterns.md";
  slug: "100-m61/zh-cn/tutorials/agent-agentic-patterns";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/tutorials/chat-client.md": {
	id: "1.0.0-M6.1/zh-cn/tutorials/chat-client.md";
  slug: "100-m61/zh-cn/tutorials/chat-client";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/tutorials/chat-model.md": {
	id: "1.0.0-M6.1/zh-cn/tutorials/chat-model.md";
  slug: "100-m61/zh-cn/tutorials/chat-model";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/tutorials/embedding.md": {
	id: "1.0.0-M6.1/zh-cn/tutorials/embedding.md";
  slug: "100-m61/zh-cn/tutorials/embedding";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/tutorials/function-calling.md": {
	id: "1.0.0-M6.1/zh-cn/tutorials/function-calling.md";
  slug: "100-m61/zh-cn/tutorials/function-calling";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/tutorials/mcp.md": {
	id: "1.0.0-M6.1/zh-cn/tutorials/mcp.md";
  slug: "100-m61/zh-cn/tutorials/mcp";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/tutorials/memory.md": {
	id: "1.0.0-M6.1/zh-cn/tutorials/memory.md";
  slug: "100-m61/zh-cn/tutorials/memory";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/tutorials/prompt.md": {
	id: "1.0.0-M6.1/zh-cn/tutorials/prompt.md";
  slug: "100-m61/zh-cn/tutorials/prompt";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/tutorials/rag.md": {
	id: "1.0.0-M6.1/zh-cn/tutorials/rag.md";
  slug: "100-m61/zh-cn/tutorials/rag";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/tutorials/retriever.md": {
	id: "1.0.0-M6.1/zh-cn/tutorials/retriever.md";
  slug: "100-m61/zh-cn/tutorials/retriever";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/tutorials/structured-output.md": {
	id: "1.0.0-M6.1/zh-cn/tutorials/structured-output.md";
  slug: "100-m61/zh-cn/tutorials/structured-output";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"1.0.0-M6.1/zh-cn/tutorials/vectorstore.md": {
	id: "1.0.0-M6.1/zh-cn/tutorials/vectorstore.md";
  slug: "100-m61/zh-cn/tutorials/vectorstore";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/en/get-started.md": {
	id: "dev/en/get-started.md";
  slug: "dev/en/get-started";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/en/get-started/agent.md": {
	id: "dev/en/get-started/agent.md";
  slug: "dev/en/get-started/agent";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/en/get-started/chatbot.md": {
	id: "dev/en/get-started/chatbot.md";
  slug: "dev/en/get-started/chatbot";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/en/get-started/workflow.md": {
	id: "dev/en/get-started/workflow.md";
  slug: "dev/en/get-started/workflow";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/en/models/dashScope.md": {
	id: "dev/en/models/dashScope.md";
  slug: "dev/en/models/dashscope";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/en/models/deepseek.md": {
	id: "dev/en/models/deepseek.md";
  slug: "dev/en/models/deepseek";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/en/models/like-openAI.md": {
	id: "dev/en/models/like-openAI.md";
  slug: "dev/en/models/like-openai";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/en/models/ollama.md": {
	id: "dev/en/models/ollama.md";
  slug: "dev/en/models/ollama";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/en/models/openAI.md": {
	id: "dev/en/models/openAI.md";
  slug: "dev/en/models/openai";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/en/overview.md": {
	id: "dev/en/overview.md";
  slug: "dev/en/overview";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/en/practices/playground-flight-booking.md": {
	id: "dev/en/practices/playground-flight-booking.md";
  slug: "dev/en/practices/playground-flight-booking";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/en/practices/rag.md": {
	id: "dev/en/practices/rag.md";
  slug: "dev/en/practices/rag";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/en/tutorials/basics/chat-client.md": {
	id: "dev/en/tutorials/basics/chat-client.md";
  slug: "dev/en/tutorials/basics/chat-client";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/en/tutorials/basics/chat-model.md": {
	id: "dev/en/tutorials/basics/chat-model.md";
  slug: "dev/en/tutorials/basics/chat-model";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/en/tutorials/basics/concepts.md": {
	id: "dev/en/tutorials/basics/concepts.md";
  slug: "dev/en/tutorials/basics/concepts";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/en/tutorials/basics/embedding.md": {
	id: "dev/en/tutorials/basics/embedding.md";
  slug: "dev/en/tutorials/basics/embedding";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/en/tutorials/basics/function-calling.md": {
	id: "dev/en/tutorials/basics/function-calling.md";
  slug: "dev/en/tutorials/basics/function-calling";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/en/tutorials/basics/memory.md": {
	id: "dev/en/tutorials/basics/memory.md";
  slug: "dev/en/tutorials/basics/memory";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/en/tutorials/basics/prompt.md": {
	id: "dev/en/tutorials/basics/prompt.md";
  slug: "dev/en/tutorials/basics/prompt";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/en/tutorials/basics/retriever.md": {
	id: "dev/en/tutorials/basics/retriever.md";
  slug: "dev/en/tutorials/basics/retriever";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/en/tutorials/basics/structured-output.md": {
	id: "dev/en/tutorials/basics/structured-output.md";
  slug: "dev/en/tutorials/basics/structured-output";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/en/tutorials/basics/vectorstore.md": {
	id: "dev/en/tutorials/basics/vectorstore.md";
  slug: "dev/en/tutorials/basics/vectorstore";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/en/tutorials/graph/whats-spring-ai-alibaba-graph.md": {
	id: "dev/en/tutorials/graph/whats-spring-ai-alibaba-graph.md";
  slug: "dev/en/tutorials/graph/whats-spring-ai-alibaba-graph";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/zh-cn/get-started.md": {
	id: "dev/zh-cn/get-started.md";
  slug: "dev/zh-cn/get-started";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/zh-cn/get-started/agent.md": {
	id: "dev/zh-cn/get-started/agent.md";
  slug: "dev/zh-cn/get-started/agent";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/zh-cn/get-started/chatbot.md": {
	id: "dev/zh-cn/get-started/chatbot.md";
  slug: "dev/zh-cn/get-started/chatbot";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/zh-cn/get-started/workflow.md": {
	id: "dev/zh-cn/get-started/workflow.md";
  slug: "dev/zh-cn/get-started/workflow";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/zh-cn/models/dashScope.md": {
	id: "dev/zh-cn/models/dashScope.md";
  slug: "dev/zh-cn/models/dashscope";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/zh-cn/models/deepseek.md": {
	id: "dev/zh-cn/models/deepseek.md";
  slug: "dev/zh-cn/models/deepseek";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/zh-cn/models/like-openAI.md": {
	id: "dev/zh-cn/models/like-openAI.md";
  slug: "dev/zh-cn/models/like-openai";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/zh-cn/models/ollama.md": {
	id: "dev/zh-cn/models/ollama.md";
  slug: "dev/zh-cn/models/ollama";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/zh-cn/models/openAI.md": {
	id: "dev/zh-cn/models/openAI.md";
  slug: "dev/zh-cn/models/openai";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/zh-cn/overview.md": {
	id: "dev/zh-cn/overview.md";
  slug: "dev/zh-cn/overview";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/zh-cn/practices/playground-flight-booking.md": {
	id: "dev/zh-cn/practices/playground-flight-booking.md";
  slug: "dev/zh-cn/practices/playground-flight-booking";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/zh-cn/practices/rag.md": {
	id: "dev/zh-cn/practices/rag.md";
  slug: "dev/zh-cn/practices/rag";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/zh-cn/tutorials/basics/chat-client.md": {
	id: "dev/zh-cn/tutorials/basics/chat-client.md";
  slug: "dev/zh-cn/tutorials/basics/chat-client";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/zh-cn/tutorials/basics/chat-model.md": {
	id: "dev/zh-cn/tutorials/basics/chat-model.md";
  slug: "dev/zh-cn/tutorials/basics/chat-model";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/zh-cn/tutorials/basics/concepts.md": {
	id: "dev/zh-cn/tutorials/basics/concepts.md";
  slug: "dev/zh-cn/tutorials/basics/concepts";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/zh-cn/tutorials/basics/embedding.md": {
	id: "dev/zh-cn/tutorials/basics/embedding.md";
  slug: "dev/zh-cn/tutorials/basics/embedding";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/zh-cn/tutorials/basics/function-calling.md": {
	id: "dev/zh-cn/tutorials/basics/function-calling.md";
  slug: "dev/zh-cn/tutorials/basics/function-calling";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/zh-cn/tutorials/basics/memory.md": {
	id: "dev/zh-cn/tutorials/basics/memory.md";
  slug: "dev/zh-cn/tutorials/basics/memory";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/zh-cn/tutorials/basics/prompt.md": {
	id: "dev/zh-cn/tutorials/basics/prompt.md";
  slug: "dev/zh-cn/tutorials/basics/prompt";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/zh-cn/tutorials/basics/retriever.md": {
	id: "dev/zh-cn/tutorials/basics/retriever.md";
  slug: "dev/zh-cn/tutorials/basics/retriever";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/zh-cn/tutorials/basics/structured-output.md": {
	id: "dev/zh-cn/tutorials/basics/structured-output.md";
  slug: "dev/zh-cn/tutorials/basics/structured-output";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/zh-cn/tutorials/basics/vectorstore.md": {
	id: "dev/zh-cn/tutorials/basics/vectorstore.md";
  slug: "dev/zh-cn/tutorials/basics/vectorstore";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"dev/zh-cn/tutorials/graph/whats-spring-ai-alibaba-graph.md": {
	id: "dev/zh-cn/tutorials/graph/whats-spring-ai-alibaba-graph.md";
  slug: "dev/zh-cn/tutorials/graph/whats-spring-ai-alibaba-graph";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"developer/en/contributor-guide/file-write-guide_dev.md": {
	id: "developer/en/contributor-guide/file-write-guide_dev.md";
  slug: "developer/en/contributor-guide/file-write-guide_dev";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"developer/en/contributor-guide/new-contributor-guide_dev.md": {
	id: "developer/en/contributor-guide/new-contributor-guide_dev.md";
  slug: "developer/en/contributor-guide/new-contributor-guide_dev";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"developer/en/contributor-guide/reporting-security-issues_dev.md": {
	id: "developer/en/contributor-guide/reporting-security-issues_dev.md";
  slug: "developer/en/contributor-guide/reporting-security-issues_dev";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"developer/en/developers_dev.md": {
	id: "developer/en/developers_dev.md";
  slug: "developer/en/developers_dev";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"developer/zh-cn/contributor-guide/file-write-guide_dev.md": {
	id: "developer/zh-cn/contributor-guide/file-write-guide_dev.md";
  slug: "developer/zh-cn/contributor-guide/file-write-guide_dev";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"developer/zh-cn/contributor-guide/new-contributor-guide_dev.md": {
	id: "developer/zh-cn/contributor-guide/new-contributor-guide_dev.md";
  slug: "developer/zh-cn/contributor-guide/new-contributor-guide_dev";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"developer/zh-cn/contributor-guide/reporting-security-issues_dev.md": {
	id: "developer/zh-cn/contributor-guide/reporting-security-issues_dev.md";
  slug: "developer/zh-cn/contributor-guide/reporting-security-issues_dev";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
"developer/zh-cn/developers_dev.md": {
	id: "developer/zh-cn/developers_dev.md";
  slug: "developer/zh-cn/developers_dev";
  body: string;
  collection: "docs";
  data: InferEntrySchema<"docs">
} & { render(): Render[".md"] };
};

	};

	type DataEntryMap = {
		"i18n": {
};

	};

	type AnyEntryMap = ContentEntryMap & DataEntryMap;

	export type ContentConfig = typeof import("./../src/content/config.js");
}
