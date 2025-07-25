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

package com.alibaba.cloud.ai.node;

import com.alibaba.cloud.ai.constant.StreamResponseType;
import com.alibaba.cloud.ai.dto.KeywordExtractionResult;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.service.base.BaseNl2SqlService;
import com.alibaba.cloud.ai.util.ChatResponseUtil;
import com.alibaba.cloud.ai.util.StateUtils;
import com.alibaba.cloud.ai.util.StreamingChatGeneratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.alibaba.cloud.ai.constant.Constant.*;

/**
 * Keyword, entity, and temporal information extraction node to prepare for subsequent
 * schema recall.
 *
 * This node is responsible for: - Extracting evidences from user input - Extracting
 * keywords based on evidences - Preparing structured information for schema recall -
 * Providing streaming feedback during extraction process
 *
 * @author zhangshenghang
 */
public class KeywordExtractNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(KeywordExtractNode.class);

	private final BaseNl2SqlService baseNl2SqlService;

	public KeywordExtractNode(BaseNl2SqlService baseNl2SqlService) {
		this.baseNl2SqlService = baseNl2SqlService;
	}

	/**
	 * 处理多个问题变体，提取关键词并合并结果 使用并行流处理提高多问题处理效率
	 * @param questions 问题变体列表
	 * @return 提取结果列表
	 */
	private List<KeywordExtractionResult> processMultipleQuestions(List<String> questions) {
		return questions.parallelStream().map(question -> {
			try {

				List<String> evidences = baseNl2SqlService.extractEvidences(question);
				List<String> keywords = baseNl2SqlService.extractKeywords(question, evidences);

				logger.info("成功从问题变体提取关键词: 问题=\"{}\", 关键词={}", question, keywords);
				return new KeywordExtractionResult(question, evidences, keywords);
			}
			catch (Exception e) {

				logger.warn("从问题变体提取关键词失败: 问题=\"{}\", 错误={}", question, e.getMessage());
				return new KeywordExtractionResult(question, false);
			}
		}).collect(java.util.stream.Collectors.toList());
	}

	/**
	 * 合并多个问题变体的关键词，去重并保持原始问题关键词优先
	 * @param extractionResults 提取结果列表
	 * @param originalQuestion 原始问题
	 * @return 合并后的关键词列表
	 */
	private List<String> mergeKeywords(List<KeywordExtractionResult> extractionResults, String originalQuestion) {
		if (extractionResults.isEmpty()) {
			return List.of();
		}

		Set<String> mergedKeywords = new LinkedHashSet<>();

		extractionResults.stream()
			.filter(result -> result.isSuccessful() && result.getQuestion().equals(originalQuestion))
			.findFirst()
			.ifPresent(result -> mergedKeywords.addAll(result.getKeywords()));

		extractionResults.stream()
			.filter(result -> result.isSuccessful() && !result.getQuestion().equals(originalQuestion))
			.forEach(result -> mergedKeywords.addAll(result.getKeywords()));

		return new ArrayList<>(mergedKeywords);
	}

	/**
	 * 合并多个问题变体的证据，去重
	 * @param extractionResults 提取结果列表
	 * @return 合并后的证据列表
	 */
	private List<String> mergeEvidences(List<KeywordExtractionResult> extractionResults) {
		Set<String> mergedEvidences = new HashSet<>();

		extractionResults.stream()
			.filter(KeywordExtractionResult::isSuccessful)
			.forEach(result -> mergedEvidences.addAll(result.getEvidences()));

		return new ArrayList<>(mergedEvidences);
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("Entering {} node", this.getClass().getSimpleName());

		String input = StateUtils.getStringValue(state, QUERY_REWRITE_NODE_OUTPUT,
				StateUtils.getStringValue(state, INPUT_KEY));

		try {
			logger.info("开始增强关键词提取处理...");

			List<String> expandedQuestions = baseNl2SqlService.expandQuestion(input);
			logger.info("问题扩展结果: {}", expandedQuestions);

			List<KeywordExtractionResult> extractionResults = processMultipleQuestions(expandedQuestions);

			List<String> mergedKeywords = mergeKeywords(extractionResults, input);
			List<String> mergedEvidences = mergeEvidences(extractionResults);

			logger.info("[{}] 增强提取结果 - 证据: {}, 关键词: {}", this.getClass().getSimpleName(), mergedEvidences,
					mergedKeywords);

			Flux<ChatResponse> displayFlux = createEnhancedDisplayFlux(extractionResults, mergedKeywords,
					mergedEvidences);

			var generator = StreamingChatGeneratorUtil
				.createStreamingGeneratorWithMessages(
						this.getClass(), state, v -> Map.of(KEYWORD_EXTRACT_NODE_OUTPUT, mergedKeywords, EVIDENCES,
								mergedEvidences, RESULT, mergedKeywords),
						displayFlux, StreamResponseType.KEYWORD_EXTRACT);

			return Map.of(KEYWORD_EXTRACT_NODE_OUTPUT, generator);

		}
		catch (Exception e) {

			logger.warn("增强关键词提取失败，回退到原始处理方法: {}", e.getMessage());
			return fallbackToOriginalProcessing(state, input);
		}
	}

	/**
	 * 创建增强的流式响应
	 * @param extractionResults 提取结果列表
	 * @param mergedKeywords 合并后的关键词
	 * @param mergedEvidences 合并后的证据
	 * @return 流式响应
	 */
	private Flux<ChatResponse> createEnhancedDisplayFlux(List<KeywordExtractionResult> extractionResults,
			List<String> mergedKeywords, List<String> mergedEvidences) {
		return Flux.create(emitter -> {
			emitter.next(ChatResponseUtil.createCustomStatusResponse("开始增强关键词提取..."));
			emitter.next(ChatResponseUtil.createCustomStatusResponse("正在扩展问题理解..."));

			for (KeywordExtractionResult result : extractionResults) {
				if (result.isSuccessful()) {
					emitter
						.next(ChatResponseUtil.createCustomStatusResponse("处理问题变体: \"" + result.getQuestion() + "\""));
					emitter.next(ChatResponseUtil
						.createCustomStatusResponse("提取的证据: " + String.join(", ", result.getEvidences())));
					emitter.next(ChatResponseUtil
						.createCustomStatusResponse("提取的关键词: " + String.join(", ", result.getKeywords())));
				}
			}

			emitter.next(ChatResponseUtil.createCustomStatusResponse("合并多个问题变体的结果..."));
			emitter.next(ChatResponseUtil.createCustomStatusResponse("合并后的证据: " + String.join(", ", mergedEvidences)));
			emitter.next(ChatResponseUtil.createCustomStatusResponse("合并后的关键词: " + String.join(", ", mergedKeywords)));
			emitter.next(ChatResponseUtil.createCustomStatusResponse("关键词提取完成."));
			emitter.complete();
		});
	}

	/**
	 * 回退到原始处理方法
	 * @param state 状态
	 * @param input 输入
	 * @return 处理结果
	 * @throws Exception 处理异常
	 */
	private Map<String, Object> fallbackToOriginalProcessing(OverAllState state, String input) throws Exception {

		List<String> evidences = baseNl2SqlService.extractEvidences(input);
		List<String> keywords = baseNl2SqlService.extractKeywords(input, evidences);

		logger.info("[{}] 原始提取结果 - 证据: {}, 关键词: {}", this.getClass().getSimpleName(), evidences, keywords);

		Flux<ChatResponse> displayFlux = Flux.create(emitter -> {
			emitter.next(ChatResponseUtil.createCustomStatusResponse("开始提取关键词..."));
			emitter.next(ChatResponseUtil.createCustomStatusResponse("正在提取证据..."));
			emitter.next(ChatResponseUtil.createCustomStatusResponse("提取的证据: " + String.join(", ", evidences)));
			emitter.next(ChatResponseUtil.createCustomStatusResponse("正在提取关键词..."));
			emitter.next(ChatResponseUtil.createCustomStatusResponse("提取的关键词: " + String.join(", ", keywords)));
			emitter.next(ChatResponseUtil.createCustomStatusResponse("关键词提取完成."));
			emitter.complete();
		});

		var generator = StreamingChatGeneratorUtil.createStreamingGeneratorWithMessages(this.getClass(), state,
				v -> Map.of(KEYWORD_EXTRACT_NODE_OUTPUT, keywords, EVIDENCES, evidences, RESULT, keywords), displayFlux,
				StreamResponseType.KEYWORD_EXTRACT);

		return Map.of(KEYWORD_EXTRACT_NODE_OUTPUT, generator);
	}

}
