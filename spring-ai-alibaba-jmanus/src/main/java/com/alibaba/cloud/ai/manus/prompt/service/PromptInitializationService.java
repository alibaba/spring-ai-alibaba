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
package com.alibaba.cloud.ai.manus.prompt.service;

import com.alibaba.cloud.ai.manus.prompt.PromptLoader;
import com.alibaba.cloud.ai.manus.prompt.model.enums.PromptEnum;
import com.alibaba.cloud.ai.manus.prompt.model.po.PromptEntity;
import com.alibaba.cloud.ai.manus.prompt.repository.PromptRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@Service
public class PromptInitializationService {

	private final PromptRepository promptRepository;

	private final PromptLoader promptLoader;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private static final Logger log = LoggerFactory.getLogger(PromptInitializationService.class);

	public PromptInitializationService(PromptRepository promptRepository, PromptLoader promptLoader) {
		this.promptRepository = promptRepository;
		this.promptLoader = promptLoader;
	}

	/**
	 * Create prompt templates if they don't exist
	 * @param namespace Namespace
	 */
	public void initializePromptsForNamespace(String namespace) {
		String defaultLanguage = "en";
		for (PromptEnum prompt : PromptEnum.values()) {
			createPromptIfNotExists(namespace, prompt, defaultLanguage);
		}
	}

	public void initializePromptsForNamespaceWithLanguage(String namespace, String language) {
		for (PromptEnum prompt : PromptEnum.values()) {
			updatePromptForLanguage(namespace, prompt, language);
		}
	}

	/**
	 * The error "could not execute statement [Unique index or primary key violation:
	 * "public.unique_prompt_name_INDEX_C ON public.prompt(prompt_name NULLS FIRST) VALUES
	 * ( 'PLANNING_PLAN_CREATION' )"; SQL statement:" occurs because the old constraint
	 * was a unique constraint on prompt_name, while the new constraint is a unique
	 * constraint on both namespace and prompt_name. JPA cannot handle this situation, so
	 * you need to delete the prompt table and restart the application, which will
	 * automatically handle it.
	 */
	private void createPromptIfNotExists(String namespace, PromptEnum prompt, String language) {
		// Start transaction to handle PostgreSQL large object compatibility issues in
		// auto-commit mode
		DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
		TransactionStatus transaction = transactionManager.getTransaction(transactionDefinition);
		PromptEntity promptEntity = null;
		try {
			promptEntity = promptRepository.findByNamespaceAndPromptName(namespace, prompt.getPromptName());
			transactionManager.commit(transaction);
		}
		catch (TransactionException e) {
			transactionManager.rollback(transaction);
		}

		if (promptEntity == null) {
			promptEntity = new PromptEntity();
			promptEntity.setPromptName(prompt.getPromptName());
			promptEntity.setNamespace(namespace);
			promptEntity.setPromptDescription(prompt.getPromptDescriptionForLanguage(language));
			promptEntity.setMessageType(prompt.getMessageType().name());
			promptEntity.setType(prompt.getType().name());
			promptEntity.setBuiltIn(prompt.getBuiltIn());

			String promptPath = prompt.getPromptPathForLanguage(language);
			String promptContent = promptLoader.loadPrompt(promptPath);
			promptEntity.setPromptContent(promptContent);

			try {
				promptRepository.save(promptEntity);
				log.info("Created prompt: {} for namespace: {} with language: {}", prompt.getPromptName(), namespace,
						language);
			}
			catch (Exception e) {
				log.error("Failed to create prompt: {} for namespace: {} with language: {}", prompt.getPromptName(),
						namespace, language, e);
			}
		}
		else {
			log.debug("Prompt already exists: {} for namespace: {}", prompt.getPromptName(), namespace);
		}
	}

	private void updatePromptForLanguage(String namespace, PromptEnum prompt, String language) {
		DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
		TransactionStatus transaction = transactionManager.getTransaction(transactionDefinition);
		PromptEntity promptEntity = null;
		try {
			promptEntity = promptRepository.findByNamespaceAndPromptName(namespace, prompt.getPromptName());
			transactionManager.commit(transaction);
		}
		catch (TransactionException e) {
			transactionManager.rollback(transaction);
		}

		if (promptEntity != null) {
			promptEntity.setPromptDescription(prompt.getPromptDescriptionForLanguage(language));

			String promptPath = prompt.getPromptPathForLanguage(language);
			String promptContent = promptLoader.loadPrompt(promptPath);
			promptEntity.setPromptContent(promptContent);

			try {
				promptRepository.save(promptEntity);
				log.info("Updated prompt: {} for namespace: {} with language: {}", prompt.getPromptName(), namespace,
						language);
			}
			catch (Exception e) {
				log.error("Failed to update prompt: {} for namespace: {} with language: {}", prompt.getPromptName(),
						namespace, language, e);
			}
		}
		else {
			createPromptIfNotExists(namespace, prompt, language);
		}
	}

}
