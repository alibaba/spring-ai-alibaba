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
package com.alibaba.cloud.ai.vectorstore.tablestore.example;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class FakedEmbeddingService implements EmbeddingModel {

	private final int dim;

	public FakedEmbeddingService(int dim) {
		this.dim = dim;
	}

	@Override
	public EmbeddingResponse call(EmbeddingRequest request) {
		int size = request.getInstructions().size();
		List<Embedding> embeddings = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			embeddings.add(new Embedding(innerEmbed(request.getInstructions().get(i)), i));
		}
		return new EmbeddingResponse(embeddings);
	}

	private float[] innerEmbed(String text) {
		return randomVector(dim);
	}

	@Override
	public float[] embed(Document document) {
		Assert.notNull(document.getText(), "Document text must not be null");
		return innerEmbed(document.getText());
	}

	public static float[] randomVector(int dim) {
		float[] vec = new float[dim];
		for (int i = 0; i < dim; i++) {
			vec[i] = ThreadLocalRandom.current().nextFloat();
			if (ThreadLocalRandom.current().nextBoolean()) {
				vec[i] = -vec[i];
			}
		}
		return l2normalize(vec, true);
	}

	public static float[] l2normalize(float[] v, boolean throwOnZero) {
		double squareSum = 0.0f;
		int dim = v.length;
		for (float x : v) {
			squareSum += x * x;
		}
		if (squareSum == 0) {
			if (throwOnZero) {
				throw new IllegalArgumentException("normalize a zero-length vector");
			}
			else {
				return v;
			}
		}
		double length = Math.sqrt(squareSum);
		for (int i = 0; i < dim; i++) {
			v[i] /= length;
		}
		return v;
	}

}
