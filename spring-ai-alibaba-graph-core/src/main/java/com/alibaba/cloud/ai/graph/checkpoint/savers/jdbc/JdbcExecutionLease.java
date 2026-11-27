/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.checkpoint.savers.jdbc;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver.ExecutionLease;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.UUID;

import static com.alibaba.cloud.ai.graph.checkpoint.savers.jdbc.AbstractJdbcCheckpointSaver.EXECUTION_LEASE_SECONDS;

/**
 * Coordinates durable graph executions without holding a pooled connection
 * while a graph node, subgraph or tool is running.
 */
final class JdbcExecutionLease implements ExecutionLease {

	static final String CONFIG_KEY = "_JDBC_EXECUTION_LEASE_";

	private static final Logger logger = LoggerFactory.getLogger(JdbcExecutionLease.class);
	private static final Duration LEASE_DURATION = Duration.ofSeconds(EXECUTION_LEASE_SECONDS);

	private final AbstractJdbcCheckpointSaver saver;
	private final String threadId;
	private final String token = UUID.randomUUID().toString();
	private final String now;
	private final String expires;
	private final Disposable renewal;

	JdbcExecutionLease(AbstractJdbcCheckpointSaver saver, String threadId, String now, String expires)
			throws SQLException {
		this.saver = saver;
		this.threadId = threadId;
		this.now = now;
		this.expires = expires;
		claim();
		renewal = Flux.interval(LEASE_DURATION.dividedBy(3))
				.onBackpressureDrop()
				.concatMap(ignored -> Mono.fromCallable(this::renew).subscribeOn(Schedulers.boundedElastic()))
				.takeWhile(Boolean::booleanValue)
				.subscribe(ignored -> { }, error -> logger.warn("Graph execution lease renewal stopped", error));
	}

	private void claim() throws SQLException {
		try (var conn = saver.openLeaseConnection()) {
			conn.setAutoCommit(true);
			try (var update = conn.prepareStatement("""
					UPDATE graph_execution_lease SET owner_token = ?, expires_at = %s
					WHERE thread_name = ? AND expires_at <= %s
					""".formatted(expires, now))) {
				update.setString(1, token);
				update.setString(2, threadId);
				if (update.executeUpdate() == 1) {
					return;
				}
			}
			try (var insert = conn.prepareStatement("""
					INSERT INTO graph_execution_lease (thread_name, owner_token, expires_at) VALUES (?, ?, %s)
					""".formatted(expires))) {
				insert.setString(1, threadId);
				insert.setString(2, token);
				insert.executeUpdate();
			}
			catch (SQLException ex) {
				// Duplicate keys are reported in JDBC's integrity-constraint SQL state class.
				if (ex.getSQLState() != null && ex.getSQLState().startsWith("23")) {
					throw new IllegalStateException("A durable graph execution is already active for thread '" + threadId + "'", ex);
				}
				throw ex;
			}
		}
	}

	private boolean renew() {
		try (var conn = saver.openLeaseConnection()) {
			conn.setAutoCommit(true);
			try (var update = conn.prepareStatement("""
					UPDATE graph_execution_lease SET expires_at = %s
					WHERE thread_name = ? AND owner_token = ? AND expires_at > %s
					""".formatted(expires, now))) {
				update.setString(1, threadId);
				update.setString(2, token);
				return update.executeUpdate() == 1;
			}
		}
		catch (SQLException ex) {
			logger.warn("Could not renew the graph execution lease for thread '{}'", threadId, ex);
			return true;
		}
	}

	@Override
	public RunnableConfig configure(RunnableConfig config) {
		return RunnableConfig.builder(config).addMetadata(CONFIG_KEY, this).build();
	}

	boolean belongsTo(AbstractJdbcCheckpointSaver owner, String thread) {
		return saver == owner && threadId.equals(thread);
	}

	/** Locks the lease row in the same transaction as the checkpoint write. */
	void fence(Connection connection) throws SQLException {
		try (var verify = connection.prepareStatement("""
				SELECT owner_token FROM graph_execution_lease
				WHERE thread_name = ? AND owner_token = ? AND expires_at > %s FOR UPDATE
				""".formatted(now))) {
			verify.setString(1, threadId);
			verify.setString(2, token);
			try (var rows = verify.executeQuery()) {
				if (!rows.next()) {
					throw new IllegalStateException("The graph execution lease for thread '" + threadId + "' has expired or been replaced");
				}
			}
		}
	}

	@Override
	public void close() {
		renewal.dispose();
		try (var conn = saver.openLeaseConnection()) {
			conn.setAutoCommit(true);
			try (var release = conn.prepareStatement(
					"DELETE FROM graph_execution_lease WHERE thread_name = ? AND owner_token = ?")) {
				release.setString(1, threadId);
				release.setString(2, token);
				release.executeUpdate();
			}
		}
		catch (SQLException ex) {
			logger.warn("Could not release the graph execution lease for thread '{}'; it will expire", threadId, ex);
		}
	}

}
