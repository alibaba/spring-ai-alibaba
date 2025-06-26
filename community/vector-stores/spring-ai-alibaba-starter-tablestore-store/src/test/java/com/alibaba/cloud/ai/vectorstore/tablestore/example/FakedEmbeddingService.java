package com.alibaba.cloud.ai.vectorstore.tablestore.example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
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
