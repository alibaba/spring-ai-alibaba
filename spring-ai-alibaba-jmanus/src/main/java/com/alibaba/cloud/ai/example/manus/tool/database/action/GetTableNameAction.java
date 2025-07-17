package com.alibaba.cloud.ai.example.manus.tool.database.action;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.alibaba.cloud.ai.example.manus.tool.database.AbstractDatabaseAction;
import com.alibaba.cloud.ai.example.manus.tool.database.DatabaseRequestVO;
import com.alibaba.cloud.ai.example.manus.tool.database.DataSourceService;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetTableNameAction extends AbstractDatabaseAction {
    private static final Logger log = LoggerFactory.getLogger(GetTableNameAction.class);

    @Override
    public ToolExecuteResult execute(DatabaseRequestVO requestVO, DataSourceService dataSourceService) {
        String text = requestVO.getText();
        if (text == null || text.trim().isEmpty()) {
            return new ToolExecuteResult("缺少查询语句");
        }
        String sql = "SELECT TABLE_SCHEMA, TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES WHERE TABLE_COMMENT LIKE ?";
        StringBuilder sb = new StringBuilder();
        try (Connection conn = dataSourceService.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + text + "%");
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
                    for (int i = 1; i < columnCount; i++) {
                        Object val = rs.getObject(i);
                        sb.append(val == null ? "NULL" : val.toString());
                        if (i < columnCount) sb.append(",");
                    }
                    sb.append("\n");

                }
            }
            log.info("GetTableNameAction result: {}", sb);
            return new ToolExecuteResult(sb.toString().trim());
        } catch (SQLException e) {
            return new ToolExecuteResult("执行查询时出错: " + e.getMessage());
        }
    }
} 