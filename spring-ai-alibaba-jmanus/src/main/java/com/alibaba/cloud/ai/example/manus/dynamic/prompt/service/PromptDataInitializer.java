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

package com.alibaba.cloud.ai.example.manus.dynamic.prompt.service;

import com.alibaba.cloud.ai.example.manus.dynamic.prompt.model.enums.PromptEnum;
import com.alibaba.cloud.ai.example.manus.dynamic.prompt.model.po.PromptEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.prompt.repository.PromptRepository;
import com.alibaba.cloud.ai.example.manus.prompt.PromptLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@Component
public class PromptDataInitializer implements CommandLineRunner {

	private final PromptRepository promptRepository;

	private final PromptLoader promptLoader;

	@Value("${namespace.value}")
	private String namespace;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private static final Logger log = LoggerFactory.getLogger(PromptDataInitializer.class);

	public PromptDataInitializer(PromptRepository promptRepository, PromptLoader promptLoader) {
		this.promptRepository = promptRepository;
		this.promptLoader = promptLoader;
	}

	@Override
	public void run(String... args) {

		for (PromptEnum prompt : PromptEnum.values()) {
			try {
				createPromptIfNotExists(prompt);
			}
			catch (Exception e) {
				// 记录日志或抛出异常
				log.error("Failed to initialize prompt data", e);
				throw e;
			}
		}

	}

	/**
	 * 碰到 could not execute statement [Unique index or primary key violation:
	 * "public.unique_prompt_name_INDEX_C ON public.prompt(prompt_name NULLS FIRST) VALUES
	 * ( 'PLANNING_PLAN_CREATION' )"; SQL statement: 是因为，老的约束是
	 * 针对prompt_name的唯一约束，而新的约束是针对namespace和prompt_name的唯一约束. jpa无法处理这种情况，所以需要你删除一下
	 * prompt表， 然后 重启应用，他会自动处理 。
	 *
	 * english ver : The error "could not execute statement [Unique index or primary key
	 * violation: "public.unique_prompt_name_INDEX_C ON public.prompt(prompt_name NULLS
	 * FIRST) VALUES ( 'PLANNING_PLAN_CREATION' )"; SQL statement:" occurs because the old
	 * constraint was a unique constraint on prompt_name, while the new constraint is a
	 * unique constraint on both namespace and prompt_name. JPA cannot handle this
	 * situation, so you need to delete the prompt table and restart the application,
	 * which will automatically handle it.
	 *
	 *
	 */
	private void createPromptIfNotExists(PromptEnum prompt) {
		// 开启事务为了兼容postgres数据库的大型对象无法被使用在自动确认事物交易模式问题
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
			promptEntity.setPromptDescription(prompt.getPromptDescription());
			promptEntity.setMessageType(prompt.getMessageType().name());
			promptEntity.setType(prompt.getType().name());
			promptEntity.setBuiltIn(prompt.getBuiltIn());
			String promptContent = promptLoader.loadPrompt(prompt.getPromptPath());
			promptEntity.setPromptContent(promptContent);
			promptRepository.save(promptEntity);
			log.info("Initialized prompt: {}", prompt.getPromptName());
		}
	}

}
