package com.alibaba.cloud.ai.example.manus.tool.database.action;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.alibaba.cloud.ai.example.manus.tool.database.AbstractDatabaseAction;
import com.alibaba.cloud.ai.example.manus.tool.database.DatabaseRequestVO;
import com.alibaba.cloud.ai.example.manus.tool.database.DataSourceService;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;

public class GetTableIndexAction extends AbstractDatabaseAction {
    @Override
    public ToolExecuteResult execute(DatabaseRequestVO requestVO, DataSourceService dataSourceService) {
        String text = requestVO.getText();
        if (text == null || text.trim().isEmpty()) {
            return new ToolExecuteResult("缺少查询语句");
        }
        String[] tableNames = text.split(",");
        StringBuilder inClause = new StringBuilder();
        for (int i = 0; i < tableNames.length; i++) {
            inClause.append("?");
            if (i < tableNames.length - 1) inClause.append(",");
        }
        String sql = "SELECT TABLE_NAME, INDEX_NAME, COLUMN_NAME, SEQ_IN_INDEX, NON_UNIQUE, INDEX_TYPE FROM information_schema.STATISTICS WHERE TABLE_NAME IN (" + inClause + ") ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX";
        StringBuilder sb = new StringBuilder();
        try (Connection conn = dataSourceService.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < tableNames.length; i++) {
                ps.setString(i + 1, tableNames[i].trim());
            }
            try (ResultSet rs = ps.executeQuery()) {
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
            }
            return new ToolExecuteResult(sb.toString().trim());
        } catch (SQLException e) {
            return new ToolExecuteResult("执行查询时出错: " + e.getMessage());
        }
    }
} 