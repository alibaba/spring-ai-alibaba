package com.alibaba.cloud.ai.example.manus.tool.database.action;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.alibaba.cloud.ai.example.manus.tool.database.action.AbstractDatabaseAction;
import com.alibaba.cloud.ai.example.manus.tool.database.DatabaseRequestVO;
import com.alibaba.cloud.ai.example.manus.tool.database.DataSourceService;
import com.alibaba.cloud.ai.example.manus.tool.database.meta.TableMeta;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        try (Connection conn = dataSourceService.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + text + "%");
            try (ResultSet rs = ps.executeQuery()) {
                List<TableMeta> tableMetaList = new java.util.ArrayList<>();
                while (rs.next()) {
                    TableMeta meta = new TableMeta();
                    meta.setTableName(rs.getString("TABLE_NAME"));
                    meta.setTableDesc(rs.getString("TABLE_COMMENT"));
                    meta.setColumns(null);
                    meta.setIndexes(null);
                    tableMetaList.add(meta);
                }
                ObjectMapper objectMapper = new ObjectMapper();
                String json = objectMapper.writeValueAsString(tableMetaList);
                log.info("GetTableNameAction result: {}", json);
                return new ToolExecuteResult(json);
            }
        } catch (Exception e) {
            return new ToolExecuteResult("执行查询时出错: " + e.getMessage());
        }
    }
} 