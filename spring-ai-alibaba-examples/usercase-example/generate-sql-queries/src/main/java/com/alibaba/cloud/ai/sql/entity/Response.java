package com.alibaba.cloud.ai.sql.entity;

import java.util.List;
import java.util.Map;

public record Response(String sqlQuery, List<Map<String, Object>> results) { }
