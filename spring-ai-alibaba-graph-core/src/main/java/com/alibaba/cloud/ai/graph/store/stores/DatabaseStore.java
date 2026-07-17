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
package com.alibaba.cloud.ai.graph.store.stores;

import com.alibaba.cloud.ai.graph.store.*;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.logging.Logger;

/**
 * Relational database-based implementation of the Store interface.
 * <p>
 * This implementation uses JDBC for database storage, supporting MySQL, PostgreSQL, H2,
 * and other JDBC-compatible databases. It provides ACID compliance and enterprise-grade
 * reliability.
 * </p>
 *
 * @author Spring AI Alibaba
 * @since 1.0.0.3
 */
public class DatabaseStore extends BaseStore {

    private static final String DEFAULT_TABLE_NAME = "spring_ai_store";

    private final DataSource dataSource;

    private final ObjectMapper objectMapper;

    private final String tableName;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private volatile DatabaseDialect databaseDialect;

    /**
     * Database dialect types for SQL adaptation.
     */
    private enum DatabaseDialect {

        H2, MYSQL, POSTGRESQL, ORACLE, OTHER

    }

    /**
     * Constructor with default table name.
     *
     * @param dataSource database data source
     */
    public DatabaseStore(DataSource dataSource) {
        this(dataSource, DEFAULT_TABLE_NAME);
    }

    /**
     * Constructor with custom table name.
     *
     * @param dataSource database data source
     * @param tableName  table name
     */
    public DatabaseStore(DataSource dataSource, String tableName) {
        this.dataSource = dataSource;
        // Fall back to the default table when caller does not provide a custom name.
        this.tableName = resolveTableName(tableName);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
        initializeTable();
    }

    /**
     * Create a DatabaseStore from an existing JDBC URL and user credentials using the
     * default table name.
     *
     * @param jdbcUrl  full JDBC URL
     * @param username database username
     * @param password database password
     * @return a DatabaseStore instance
     */
    public static DatabaseStore fromJdbcUrl(String jdbcUrl, String username, String password) {
        return fromJdbcUrl(jdbcUrl, username, password, null);
    }

    /**
     * Create a DatabaseStore from an existing JDBC URL and user credentials.
     *
     * @param jdbcUrl  full JDBC URL
     * @param username database username
     * @param password database password
     * @param tableName table name used by this store
     * @return a DatabaseStore instance
     */
    public static DatabaseStore fromJdbcUrl(String jdbcUrl, String username, String password, String tableName) {
        // Build a tiny DataSource wrapper so users can use DatabaseStore without creating a DataSource bean.
        DataSource dataSource = new SimpleJdbcDataSource(jdbcUrl, username, password);
        return new DatabaseStore(dataSource, tableName);
    }

    /**
     * Create a DatabaseStore from basic connection parameters using the default table
     * name.
     *
     * @param dbType   database type
     * @param host     database host
     * @param port     database port
     * @param database database name (or service name for Oracle)
     * @param username database username
     * @param password database password
     * @return a DatabaseStore instance
     */
    public static DatabaseStore fromConnectionInfo(String dbType, String host, int port, String database,
                                                   String username, String password) {
        return fromConnectionInfo(dbType, host, port, database, username, password, null);
    }

    /**
     * Create a DatabaseStore from basic connection parameters.
     * Supported dbType values: mysql, postgresql, oracle, h2.
     *
     * @param dbType   database type
     * @param host     database host
     * @param port     database port
     * @param database database name (or service name for Oracle)
     * @param username database username
     * @param password database password
     * @param tableName table name used by this store
     * @return a DatabaseStore instance
     */
    public static DatabaseStore fromConnectionInfo(String dbType, String host, int port, String database,
                                                   String username, String password, String tableName) {
        String jdbcUrl = buildJdbcUrl(dbType, host, port, database);
        return fromJdbcUrl(jdbcUrl, username, password, tableName);
    }

    /**
     * Build a JDBC URL from simple connection parameters.
     *
     * @param dbType   database type
     * @param host     database host
     * @param port     database port
     * @param database database name (or service name for Oracle)
     * @return normalized JDBC URL
     */
    public static String buildJdbcUrl(String dbType, String host, int port, String database) {
        validateConnectionInfo(dbType, host, port, database);
        String normalized = dbType.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            // MySQL supports optional auto database creation via URL parameters.
            case "mysql" -> "jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            case "postgresql", "postgres", "pgsql" -> "jdbc:postgresql://" + host + ":" + port + "/" + database;
            case "oracle" -> "jdbc:oracle:thin:@" + host + ":" + port + ":" + database;
            case "h2" -> "jdbc:h2:mem:" + database + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
            default -> throw new IllegalArgumentException(
                    "Unsupported dbType: " + dbType + ". Supported values: mysql, postgresql, oracle, h2");
        };
    }

    /**
     * Validate connection parameters for JDBC URL construction.
     *
     * @param dbType   database type
     * @param host     database host
     * @param port     database port
     * @param database database name
     */
    private static void validateConnectionInfo(String dbType, String host, int port, String database) {
        if (dbType == null || dbType.isBlank()) {
            throw new IllegalArgumentException("dbType cannot be null or blank");
        }
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host cannot be null or blank");
        }
        if (port <= 0) {
            throw new IllegalArgumentException("port must be greater than 0");
        }
        if (database == null || database.isBlank()) {
            throw new IllegalArgumentException("database cannot be null or blank");
        }
    }

    /**
     * Resolve table name with backward-compatible default behavior.
     *
     * @param tableName candidate table name
     * @return resolved table name
     */
    private static String resolveTableName(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return DEFAULT_TABLE_NAME;
        }
        return tableName;
    }

    /**
     * Minimal JDBC DataSource implementation based on DriverManager.
     * This keeps dependency footprint small and avoids introducing extra pooling libs.
     */
    private static final class SimpleJdbcDataSource implements DataSource {

        private final String jdbcUrl;

        private final String username;

        private final String password;

        /**
         * Construct a simple DataSource from JDBC URL and credentials.
         *
         * @param jdbcUrl JDBC URL
         * @param username database username
         * @param password database password
         */
        private SimpleJdbcDataSource(String jdbcUrl, String username, String password) {
            if (jdbcUrl == null || jdbcUrl.isBlank()) {
                throw new IllegalArgumentException("jdbcUrl cannot be null or blank");
            }
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.password = password;
        }

        /**
         * Open a connection using configured credentials.
         *
         * @return JDBC connection
         * @throws SQLException if connection opening fails
         */
        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(jdbcUrl, username, password);
        }

        /**
         * Open a connection using provided credentials.
         *
         * @param username username for this connection
         * @param password password for this connection
         * @return JDBC connection
         * @throws SQLException if connection opening fails
         */
        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return DriverManager.getConnection(jdbcUrl, username, password);
        }

        /**
         * Unwrap to vendor-specific interface.
         *
         * @param iface target interface
         * @return wrapped interface
         * @param <T> generic target type
         * @throws SQLException if unsupported
         */
        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new SQLFeatureNotSupportedException("unwrap is not supported");
        }

        /**
         * Check if wrapper can provide target interface.
         *
         * @param iface target interface
         * @return false because unwrap is not supported
         * @param <T> generic target type
         * @throws SQLException never thrown in this implementation
         */
        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return false;
        }

        /**
         * Return writer used for logging.
         *
         * @return null because not managed by this DataSource
         * @throws SQLException never thrown in this implementation
         */
        @Override
        public PrintWriter getLogWriter() throws SQLException {
            return null;
        }

        /**
         * Set writer used for logging.
         *
         * @param out print writer
         * @throws SQLException never thrown in this implementation
         */
        @Override
        public void setLogWriter(PrintWriter out) throws SQLException {
            // No-op: this simple implementation does not manage a dedicated log writer.
        }

        /**
         * Set login timeout.
         *
         * @param seconds timeout seconds
         * @throws SQLException if DriverManager cannot apply the timeout
         */
        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
            DriverManager.setLoginTimeout(seconds);
        }

        /**
         * Get login timeout.
         *
         * @return timeout seconds
         * @throws SQLException if DriverManager cannot return the timeout
         */
        @Override
        public int getLoginTimeout() throws SQLException {
            return DriverManager.getLoginTimeout();
        }

        /**
         * Return parent logger.
         *
         * @return global logger
         * @throws SQLFeatureNotSupportedException never thrown in this implementation
         */
        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return Logger.getGlobal();
        }

    }

    @Override
    public void putItem(StoreItem item) {
        validatePutItem(item);

        lock.writeLock().lock();
        try {
            String itemId = createItemId(item.getNamespace(), item.getKey());
            String itemHash = createItemHash(itemId);
            String namespaceJson = objectMapper.writeValueAsString(item.getNamespace());
            String valueJson = objectMapper.writeValueAsString(item.getValue());

            try (Connection conn = dataSource.getConnection()) {
                executeUpsert(conn, itemId, itemHash, namespaceJson, item.getKey(), valueJson,
                        new Timestamp(item.getCreatedAt()), new Timestamp(item.getUpdatedAt()));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to store item in database", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Execute UPSERT using dialect-specific SQL for mainstream databases. Falls back to
     * a generic UPDATE-then-INSERT path for unsupported dialects.
     *
     * @param conn          database connection
     * @param itemId        item id
     * @param namespaceJson serialized namespace
     * @param key           key name
     * @param valueJson     serialized value
     * @param createdAt     created timestamp
     * @param updatedAt     updated timestamp
     * @throws SQLException if SQL execution fails
     */
    private void executeUpsert(Connection conn, String itemId, String itemHash, String namespaceJson, String key, String valueJson,
                               Timestamp createdAt, Timestamp updatedAt) throws SQLException {
        DatabaseDialect dialect = getDatabaseDialect(conn);
        switch (dialect) {
            case H2 -> executeH2Merge(conn, itemId, itemHash, namespaceJson, key, valueJson, createdAt, updatedAt);
            case MYSQL -> executeMysqlUpsert(conn, itemId, itemHash, namespaceJson, key, valueJson, createdAt, updatedAt);
            case POSTGRESQL -> executePostgresqlUpsert(conn, itemId, itemHash, namespaceJson, key, valueJson, createdAt,
                    updatedAt);
            case ORACLE -> executeOracleUpsert(conn, itemId, itemHash, namespaceJson, key, valueJson, createdAt, updatedAt);
            case OTHER -> throw new UnsupportedOperationException(
                    "Unsupported database dialect: " + dialect + ". Supported dialects: H2, MySQL, PostgreSQL, Oracle");
        }
    }

    /**
     * H2 database UPSERT implementation using MERGE INTO syntax.
     *
     * @param conn          database connection
     * @param itemId        unique primary key
     * @param namespaceJson serialized namespace
     * @param key           business key
     * @param valueJson     serialized value
     * @param createdAt     created timestamp
     * @param updatedAt     updated timestamp
     * @throws SQLException if SQL execution fails
     */
    private void executeH2Merge(Connection conn, String itemId, String itemHash, String namespaceJson, String key, String valueJson,
                                Timestamp createdAt, Timestamp updatedAt) throws SQLException {
        // MERGE by id_hash to avoid long-key index limitations across dialects.
        String sql = "MERGE INTO " + tableName + " (id, id_hash, namespace, key_name, value_json, created_at, updated_at) "
                + "KEY(id_hash) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            stmt.setString(2, itemHash);
            stmt.setString(3, namespaceJson);
            stmt.setString(4, key);
            stmt.setString(5, valueJson);
            stmt.setTimestamp(6, createdAt);
            stmt.setTimestamp(7, updatedAt);
            stmt.executeUpdate();
        }
    }

    /**
     * MySQL database UPSERT implementation using ON DUPLICATE KEY UPDATE syntax.
     *
     * @param conn          database connection
     * @param itemId        unique primary key
     * @param namespaceJson serialized namespace
     * @param key           business key
     * @param valueJson     serialized value
     * @param createdAt     created timestamp
     * @param updatedAt     updated timestamp
     * @throws SQLException if SQL execution fails
     */
    private void executeMysqlUpsert(Connection conn, String itemId, String itemHash, String namespaceJson, String key, String valueJson,
                                    Timestamp createdAt, Timestamp updatedAt) throws SQLException {
        // INSERT ... ON DUPLICATE KEY UPDATE ... VALUES(...)
        String sql = "INSERT INTO " + tableName + " (id, id_hash, namespace, key_name, value_json, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE id = VALUES(id), namespace = VALUES(namespace), key_name = VALUES(key_name), "
                + "value_json = VALUES(value_json), updated_at = VALUES(updated_at)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            stmt.setString(2, itemHash);
            stmt.setString(3, namespaceJson);
            stmt.setString(4, key);
            stmt.setString(5, valueJson);
            stmt.setTimestamp(6, createdAt);
            stmt.setTimestamp(7, updatedAt);
            stmt.executeUpdate();
        }
    }

    /**
     * PostgreSQL database UPSERT implementation using ON CONFLICT DO UPDATE syntax.
     *
     * @param conn          database connection
     * @param itemId        unique primary key
     * @param namespaceJson serialized namespace
     * @param key           business key
     * @param valueJson     serialized value
     * @param createdAt     created timestamp
     * @param updatedAt     updated timestamp
     * @throws SQLException if SQL execution fails
     */
    private void executePostgresqlUpsert(Connection conn, String itemId, String itemHash, String namespaceJson, String key,
                                         String valueJson, Timestamp createdAt, Timestamp updatedAt) throws SQLException {
        // INSERT ... ON CONFLICT (id_hash) DO UPDATE SET ... = EXCLUDED....
        String sql = "INSERT INTO " + tableName + " (id, id_hash, namespace, key_name, value_json, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT (id_hash) DO UPDATE SET id = EXCLUDED.id, namespace = EXCLUDED.namespace, "
                + "key_name = EXCLUDED.key_name, value_json = EXCLUDED.value_json, "
                + "updated_at = EXCLUDED.updated_at";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            stmt.setString(2, itemHash);
            stmt.setString(3, namespaceJson);
            stmt.setString(4, key);
            stmt.setString(5, valueJson);
            stmt.setTimestamp(6, createdAt);
            stmt.setTimestamp(7, updatedAt);
            stmt.executeUpdate();
        }
    }

    /**
     * Oracle database UPSERT implementation using MERGE INTO syntax.
     *
     * @param conn          database connection
     * @param itemId        unique primary key
     * @param namespaceJson serialized namespace
     * @param key           business key
     * @param valueJson     serialized value
     * @param createdAt     created timestamp
     * @param updatedAt     updated timestamp
     * @throws SQLException if SQL execution fails
     */
    private void executeOracleUpsert(Connection conn, String itemId, String itemHash, String namespaceJson, String key, String valueJson,
                                     Timestamp createdAt, Timestamp updatedAt) throws SQLException {
        // MERGE by id_hash to avoid oversized unique-key issues.
        String sql = "MERGE INTO " + tableName + " USING DUAL ON (id_hash = ?) " + "WHEN MATCHED THEN UPDATE SET "
                + "id = ?, namespace = ?, key_name = ?, value_json = ?, updated_at = ? "
                + "WHEN NOT MATCHED THEN INSERT (id, id_hash, namespace, key_name, value_json, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemHash);
            stmt.setString(2, itemId);
            stmt.setString(3, namespaceJson);
            stmt.setString(4, key);
            stmt.setString(5, valueJson);
            stmt.setTimestamp(6, updatedAt);
            stmt.setString(7, itemId);
            stmt.setString(8, itemHash);
            stmt.setString(9, namespaceJson);
            stmt.setString(10, key);
            stmt.setString(11, valueJson);
            stmt.setTimestamp(12, createdAt);
            stmt.setTimestamp(13, updatedAt);
            stmt.executeUpdate();
        }
    }

    /**
     * Identify and cache database dialect based on JDBC metadata.
     *
     * @param conn database connection
     * @return database dialect
     * @throws SQLException if metadata retrieval fails
     */
    private DatabaseDialect getDatabaseDialect(Connection conn) throws SQLException {
        DatabaseDialect cached = databaseDialect;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (databaseDialect == null) {
                String productName = conn.getMetaData().getDatabaseProductName();
                String normalized = productName == null ? "" : productName.toLowerCase(Locale.ROOT);
                if (normalized.contains("postgresql")) {
                    databaseDialect = DatabaseDialect.POSTGRESQL;
                } else if (normalized.contains("mysql")) {
                    databaseDialect = DatabaseDialect.MYSQL;
                } else if (normalized.contains("h2")) {
                    databaseDialect = DatabaseDialect.H2;
                } else if (normalized.contains("oracle")) {
                    databaseDialect = DatabaseDialect.ORACLE;
                } else {
                    databaseDialect = DatabaseDialect.OTHER;
                }
            }
            return databaseDialect;
        }
    }

    @Override
    public Optional<StoreItem> getItem(List<String> namespace, String key) {
        validateGetItem(namespace, key);

        lock.readLock().lock();
        try {
            String itemId = createItemId(namespace, key);
            String itemHash = createItemHash(itemId);
            String sql = "SELECT namespace, key_name, value_json, created_at, updated_at FROM " + tableName
                    + " WHERE id_hash = ?";

            try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, itemHash);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return Optional.of(resultSetToStoreItem(rs));
                }

                return Optional.empty();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve item from database", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean deleteItem(List<String> namespace, String key) {
        validateDeleteItem(namespace, key);

        lock.writeLock().lock();
        try {
            String itemId = createItemId(namespace, key);
            String itemHash = createItemHash(itemId);
            String sql = "DELETE FROM " + tableName + " WHERE id_hash = ?";

            try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, itemHash);
                return stmt.executeUpdate() > 0;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete item from database", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public StoreSearchResult searchItems(StoreSearchRequest searchRequest) {
        validateSearchItems(searchRequest);

        lock.readLock().lock();
        try {
            List<StoreItem> allItems = getAllItems();

            // Apply filters
            List<StoreItem> filteredItems = allItems.stream()
                    .filter(item -> matchesSearchCriteria(item, searchRequest))
                    .collect(Collectors.toList());

            // Sort items
            if (!searchRequest.getSortFields().isEmpty()) {
                filteredItems.sort(createComparator(searchRequest));
            }

            long totalCount = filteredItems.size();

            // Apply pagination
            int offset = searchRequest.getOffset();
            int limit = searchRequest.getLimit();

            if (offset >= filteredItems.size()) {
                return StoreSearchResult.of(Collections.emptyList(), totalCount, offset, limit);
            }

            int endIndex = Math.min(offset + limit, filteredItems.size());
            List<StoreItem> resultItems = filteredItems.subList(offset, endIndex);

            return StoreSearchResult.of(resultItems, totalCount, offset, limit);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<String> listNamespaces(NamespaceListRequest namespaceRequest) {
        validateListNamespaces(namespaceRequest);

        lock.readLock().lock();
        try {
            Set<String> namespaceSet = new HashSet<>();
            List<String> prefixFilter = namespaceRequest.getNamespace();

            List<StoreItem> allItems = getAllItems();

            for (StoreItem item : allItems) {
                List<String> itemNamespace = item.getNamespace();

                // Check if namespace starts with prefix filter
                if (!prefixFilter.isEmpty() && !startsWithPrefix(itemNamespace, prefixFilter)) {
                    continue;
                }

                // Generate all possible namespace paths up to maxDepth
                int maxDepth = namespaceRequest.getMaxDepth();
                int depth = (maxDepth == -1) ? itemNamespace.size() : Math.min(maxDepth, itemNamespace.size());

                for (int i = 1; i <= depth; i++) {
                    String namespacePath = String.join("/", itemNamespace.subList(0, i));
                    namespaceSet.add(namespacePath);
                }
            }

            List<String> namespaces = new ArrayList<>(namespaceSet);
            Collections.sort(namespaces);

            // Apply pagination
            int offset = namespaceRequest.getOffset();
            int limit = namespaceRequest.getLimit();

            if (offset >= namespaces.size()) {
                return Collections.emptyList();
            }

            int endIndex = Math.min(offset + limit, namespaces.size());
            return namespaces.subList(offset, endIndex);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        String sql = "DELETE FROM " + tableName;

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clear database store", e);
        }
    }

    @Override
    public long size() {
        String sql = "SELECT COUNT(*) FROM " + tableName;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get store size from database", e);
        }
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Initialize database table with dialect-specific DDL.
     * - Keep business unique key `id` for logical identity and upsert conflict handling.
     * - Add auto-increment `pk_id` as physical primary key.
     */
    private void initializeTable() {
        String sql;
        boolean shouldCreate = true;
        try (Connection conn = dataSource.getConnection()) {
            DatabaseDialect dialect = getDatabaseDialect(conn);
            if (dialect == DatabaseDialect.ORACLE) {
                // Oracle does not support CREATE TABLE IF NOT EXISTS, so we pre-check table existence.
                shouldCreate = !tableExists(conn, tableName);
            }
            sql = switch (dialect) {
                case ORACLE -> "CREATE TABLE " + tableName + " ("
                        + "pk_id NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, "
                        + "id VARCHAR2(4000) NOT NULL, "
                        + "id_hash CHAR(64) NOT NULL UNIQUE, "
                        + "namespace VARCHAR2(4000), " + "key_name VARCHAR2(500), " + "value_json CLOB, "
                        + "created_at TIMESTAMP, " + "updated_at TIMESTAMP" + ")";
                case MYSQL -> "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                        + "pk_id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                        + "id VARCHAR(2048) NOT NULL, "
                        + "id_hash CHAR(64) NOT NULL UNIQUE, "
                        + "namespace TEXT, " + "key_name VARCHAR(500), " + "value_json TEXT, "
                        + "created_at TIMESTAMP, " + "updated_at TIMESTAMP" + ")";
                case POSTGRESQL -> "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                        + "pk_id BIGSERIAL PRIMARY KEY, "
                        + "id TEXT NOT NULL, "
                        + "id_hash CHAR(64) NOT NULL UNIQUE, "
                        + "namespace TEXT, " + "key_name VARCHAR(500), " + "value_json TEXT, "
                        + "created_at TIMESTAMP, " + "updated_at TIMESTAMP" + ")";
                case H2 -> "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                        + "pk_id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                        + "id TEXT NOT NULL, "
                        + "id_hash CHAR(64) NOT NULL UNIQUE, "
                        + "namespace TEXT, " + "key_name VARCHAR(500), " + "value_json TEXT, "
                        + "created_at TIMESTAMP, " + "updated_at TIMESTAMP" + ")";
                case OTHER -> throw new UnsupportedOperationException(
                        "Unsupported database dialect: " + dialect + ". Supported dialects: H2, MySQL, PostgreSQL, Oracle");
            };
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize table", e);
        }

        if (!shouldCreate) {
            return;
        }

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize table", e);
        }
    }

    /**
     * Check whether a table already exists in the current database schema/catalog.
     *
     * @param conn      JDBC connection
     * @param tableName target table name
     * @return true if table exists, false otherwise
     * @throws SQLException if metadata querying fails
     */
    private boolean tableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        String normalized = tableName == null ? null : tableName.toUpperCase(Locale.ROOT);
        try (ResultSet rs = metaData.getTables(conn.getCatalog(), conn.getSchema(), normalized, new String[] { "TABLE" })) {
            if (rs.next()) {
                return true;
            }
        }
        try (ResultSet rs = metaData.getTables(null, null, normalized, new String[] { "TABLE" })) {
            return rs.next();
        }
    }

    /**
     * Create item ID from namespace and key.
     *
     * @param namespace namespace
     * @param key       key
     * @return item ID
     */
    private String createItemId(List<String> namespace, String key) {
        return createStoreKey(namespace, key);
    }

    /**
     * Create a fixed-length hash for long business identifiers to keep unique indexes
     * stable across different database dialects.
     *
     * @param itemId original business identifier
     * @return sha256 hex string
     */
    private String createItemHash(String itemId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(itemId.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create item hash", e);
        }
    }

    /**
     * Get all items from database.
     *
     * @return list of all items
     */
    private List<StoreItem> getAllItems() {
        List<StoreItem> items = new ArrayList<>();
        String sql = "SELECT namespace, key_name, value_json, created_at, updated_at FROM " + tableName;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                try {
                    items.add(resultSetToStoreItem(rs));
                } catch (Exception e) {
                    // Skip invalid items
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve all items from database", e);
        }

        return items;
    }

    /**
     * Convert ResultSet to StoreItem.
     *
     * @param rs result set
     * @return StoreItem
     * @throws Exception if conversion fails
     */
    @SuppressWarnings("unchecked")
    private StoreItem resultSetToStoreItem(ResultSet rs) {
        try {
            String namespaceJson = rs.getString("namespace");
            String key = rs.getString("key_name");
            String valueJson = rs.getString("value_json");
            Timestamp createdAt = rs.getTimestamp("created_at");
            Timestamp updatedAt = rs.getTimestamp("updated_at");

            objectMapper.findAndRegisterModules();
            JavaType namespaceType = objectMapper.getTypeFactory().constructCollectionType(List.class, String.class);
            JavaType valueType = objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class);
            List<String> namespace = objectMapper.readValue(namespaceJson, namespaceType);
            Map<String, Object> value = objectMapper.readValue(valueJson, valueType);

            return new StoreItem(namespace, key, value, createdAt.getTime(), updatedAt.getTime());
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize store item", e);
        }
    }

}
