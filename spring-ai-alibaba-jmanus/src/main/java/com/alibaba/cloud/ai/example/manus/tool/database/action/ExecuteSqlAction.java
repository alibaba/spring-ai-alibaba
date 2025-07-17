package com.alibaba.cloud.ai.example.manus.tool.database.action;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.cloud.ai.example.manus.tool.database.AbstractDatabaseAction;
import com.alibaba.cloud.ai.example.manus.tool.database.DatabaseRequestVO;
import com.alibaba.cloud.ai.example.manus.tool.database.DataSourceService;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;

public class ExecuteSqlAction extends AbstractDatabaseAction {
    @Override
    public ToolExecuteResult execute(DatabaseRequestVO requestVO, DataSourceService dataSourceService) {
        String query = requestVO.getQuery();
        if (query == null || query.trim().isEmpty()) {
            return new ToolExecuteResult("错误: 缺少查询语句");
        }
        String[] statements = query.split(";");
        List<String> results = new ArrayList<>();
        try (Connection conn = dataSourceService.getConnection(); Statement stmt = conn.createStatement()) {
            for (String sql : statements) {
                sql = sql.trim();
                if (sql.isEmpty()) continue;
                boolean hasResultSet = stmt.execute(sql);
                if (hasResultSet) {
                    try (ResultSet rs = stmt.getResultSet()) {
                        results.add(formatResultSet(rs));
                    }
                } else {
                    int updateCount = stmt.getUpdateCount();
                    results.add("执行成功。影响行数: " + updateCount);
                }
            }
            return new ToolExecuteResult(String.join("\n---\n", results));
        } catch (SQLException e) {
            return new ToolExecuteResult("SQL执行失败: " + e.getMessage());
        }
    }

    private String formatResultSet(ResultSet rs) throws SQLException {
        StringBuilder sb = new StringBuilder();
        int columnCount = rs.getMetaData().getColumnCount();
        // 列名
        for (int i = 1; i <= columnCount; i++) {
            sb.append(rs.getMetaData().getColumnName(i));
            if (i < columnCount) sb.append(",");
        }
        sb.append("\n");
        // 数据
        while (rs.next()) {
            for (int i = 1; i <= columnCount; i++) {
                Object val = rs.getObject(i);
                sb.append(val == null ? "NULL" : val.toString());
                if (i < columnCount) sb.append(",");
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }
} 