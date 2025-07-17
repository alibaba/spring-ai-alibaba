package com.alibaba.cloud.ai.example.manus.tool.database;

import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;

public abstract class AbstractDatabaseAction {
    /**
     * 执行数据库操作
     * @param requestVO 请求参数
     * @param dataSourceService 数据源服务
     * @return 执行结果
     */
    public abstract ToolExecuteResult execute(DatabaseRequestVO requestVO, DataSourceService dataSourceService);
} 