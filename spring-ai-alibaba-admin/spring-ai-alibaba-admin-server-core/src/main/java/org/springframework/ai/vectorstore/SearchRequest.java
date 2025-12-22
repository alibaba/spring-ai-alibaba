/*
 * Copyright 2023-2024 the original author or authors.
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

package org.springframework.ai.vectorstore;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Similarity search request. Use the {@link SearchRequest#builder()} to create the
 * instance of a {@link SearchRequest}.
 *
 * @author Christian Tzolov
 * @author Thomas Vitale
 * @author Ilayaperumal Gopinathan
 */
public final class SearchRequest {

	/**
	 * Similarity threshold that accepts all search scores. A threshold value of 0.0 means
	 * any similarity is accepted or disable the similarity threshold filtering. A
	 * threshold value of 1.0 means an exact match is required.
	 */
	public static final double SIMILARITY_THRESHOLD_ACCEPT_ALL = 0.4;

	/**
	 * Default value for the top 'k' similar results to return.
	 */
	public static final int DEFAULT_TOP_K = 4;

	/**
	 * Default value is empty string.
	 */
	private String query = "";

	private int topK = DEFAULT_TOP_K;

	private int from;

	private double similarityThreshold = SIMILARITY_THRESHOLD_ACCEPT_ALL;

	private SearchType searchType = SearchType.SEMANTIC;

	/**
	 * Ratio of results for semantic and full-text search. Default value is 0.7. Means 0.7
	 * topK semantic results and (1 - 0.7) * topK full-text results.
	 */
	private float hybridWeight = 0.7f;

	@Nullable
	private Filter.Expression filterExpression;

	/**
	 * Copy an existing {@link SearchRequest.Builder} instance.
	 * @param originalSearchRequest {@link SearchRequest} instance to copy.
	 * @return Returns new {@link SearchRequest.Builder} instance.
	 */
	public static Builder from(SearchRequest originalSearchRequest) {
		return builder().query(originalSearchRequest.getQuery())
			.topK(originalSearchRequest.getTopK())
			.similarityThreshold(originalSearchRequest.getSimilarityThreshold())
			.filterExpression(originalSearchRequest.getFilterExpression());
	}

	public String getQuery() {
		return this.query;
	}

	public int getTopK() {
		return this.topK;
	}

	public int getFrom() {
		return from;
	}

	public SearchType getSearchType() {
		return this.searchType;
	}

	public double getSimilarityThreshold() {
		return this.similarityThreshold;
	}

	public float getHybridWeight() {
		return hybridWeight;
	}

	@Nullable
	public Filter.Expression getFilterExpression() {
		return this.filterExpression;
	}

	public boolean hasFilterExpression() {
		return this.filterExpression != null;
	}

	/**
	 * Builder for creating the SearchRequest instance.
	 * @return the builder.
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * SearchRequest Builder.
	 */
	public static class Builder {

		private final SearchRequest searchRequest = new SearchRequest();

		/**
		 * @param query Text to use for embedding similarity comparison.
		 * @return this builder.
		 */
		public Builder query(String query) {
			Assert.notNull(query, "Query can not be null.");
			this.searchRequest.query = query;
			return this;
		}

		public Builder from(int from) {
			this.searchRequest.from = from;
			return this;
		}

		/**
		 * @param topK the top 'k' similar results to return.
		 * @return this builder.
		 */
		public Builder topK(int topK) {
			Assert.isTrue(topK >= 0, "TopK should be positive.");
			this.searchRequest.topK = topK;
			return this;
		}

		/**
		 * Similarity threshold score to filter the search response by. Only documents
		 * with similarity score equal or greater than the 'threshold' will be returned.
		 * Note that this is a post-processing step performed on the client not the server
		 * side. A threshold value of 0.0 means any similarity is accepted or disable the
		 * similarity threshold filtering. A threshold value of 1.0 means an exact match
		 * is required.
		 * @param threshold The lower bound of the similarity score.
		 * @return this builder.
		 */
		public Builder similarityThreshold(double threshold) {
			Assert.isTrue(threshold >= 0 && threshold <= 1, "Similarity threshold must be in [0,1] range.");
			this.searchRequest.similarityThreshold = threshold;
			return this;
		}

		/**
		 * Sets disables the similarity threshold by setting it to 0.0 - all results are
		 * accepted.
		 * @return this builder.
		 */
		public Builder similarityThresholdAll() {
			this.searchRequest.similarityThreshold = 0.0;
			return this;
		}

		/**
		 * Retrieves documents by query embedding similarity and matching the filters.
		 * Value of 'null' means that no metadata filters will be applied to the search.
		 * <p>
		 * For example if the {@link Document#getMetadata()} schema is:
		 *
		 * <pre>{@code
		 * &#123;
		 * "country": <Text>,
		 * "city": <Text>,
		 * "year": <Number>,
		 * "price": <Decimal>,
		 * "isActive": <Boolean>
		 * &#125;
		 * }</pre>
		 * <p>
		 * you can constrain the search result to only UK countries with isActive=true and
		 * year equal or greater 2020. You can build this such metadata filter
		 * programmatically like this:
		 *
		 * <pre>{@code
		 * var exp = new Filter.Expression(AND,
		 * 		new Expression(EQ, new Key("country"), new Value("UK")),
		 * 		new Expression(AND,
		 * 				new Expression(GTE, new Key("year"), new Value(2020)),
		 * 				new Expression(EQ, new Key("isActive"), new Value(true))));
		 * }</pre>
		 * <p>
		 * The {@link Filter.Expression} is portable across all vector stores.<br/>
		 * <p>
		 * <p>
		 * The {@link FilterExpressionBuilder} is a DSL creating expressions
		 * programmatically:
		 *
		 * <pre>{@code
		 * var b = new FilterExpressionBuilder();
		 * var exp = b.and(
		 * 		b.eq("country", "UK"),
		 * 		b.and(
		 * 			b.gte("year", 2020),
		 * 			b.eq("isActive", true)));
		 * }</pre>
		 * <p>
		 * The {@link FilterExpressionTextParser} converts textual, SQL like filter
		 * expression language into {@link Filter.Expression}:
		 *
		 * <pre>{@code
		 * var parser = new FilterExpressionTextParser();
		 * var exp = parser.parse("country == 'UK' && isActive == true && year >=2020");
		 * }</pre>
		 * @param expression {@link Filter.Expression} instance used to define the
		 * metadata filter criteria. The 'null' value stands for no expression filters.
		 * @return this builder.
		 */
		public Builder filterExpression(@Nullable Filter.Expression expression) {
			this.searchRequest.filterExpression = expression;
			return this;
		}

		/**
		 * Document metadata filter expression. For example if your
		 * {@link Document#getMetadata()} has a schema like:
		 *
		 * <pre>{@code
		 * &#123;
		 * "country": <Text>,
		 * "city": <Text>,
		 * "year": <Number>,
		 * "price": <Decimal>,
		 * "isActive": <Boolean>
		 * &#125;
		 * }</pre>
		 * <p>
		 * then you can constrain the search result with metadata filter expressions like:
		 *
		 * <pre>{@code
		 * country == 'UK' && year >= 2020 && isActive == true
		 * Or
		 * country == 'BG' && (city NOT IN ['Sofia', 'Plovdiv'] || price < 134.34)
		 * }</pre>
		 * <p>
		 * This ensures that the response contains only embeddings that match the
		 * specified filer criteria. <br/>
		 * <p>
		 * The declarative, SQL like, filter syntax is portable across all vector stores
		 * supporting the filter search feature.<br/>
		 * <p>
		 * The {@link FilterExpressionTextParser} is used to convert the text filter
		 * expression into {@link Filter.Expression}.
		 * @param textExpression declarative, portable, SQL like, metadata filter syntax.
		 * The 'null' value stands for no expression filters.
		 * @return this.builder
		 */
		public Builder filterExpression(@Nullable String textExpression) {
			this.searchRequest.filterExpression = (textExpression != null)
					? new FilterExpressionTextParser().parse(textExpression) : null;
			return this;
		}

		public Builder searchType(SearchType searchType) {
			this.searchRequest.searchType = searchType;
			return this;
		}

		public Builder hybridWeight(float ratio) {
			this.searchRequest.hybridWeight = ratio;
			return this;
		}

		public SearchRequest build() {
			return this.searchRequest;
		}

	}

}
