# Parallel Agent 预制参数梳理

## 对比分析

### mapreduce_define.md (详细配置) vs mapreduce_cmd.md (简化配置)

## Parallel Agent 需要预制的核心参数：

### 1. 执行策略参数
```json
{
  "executionStrategy": "CONCURRENT",  // 并发执行策略
  "concurrencyLevel": 3               // 并发级别（执行配置中）
}
```

### 2. 子智能体管理参数
```json
{
  "subAgents": ["agent_name"],        // 子智能体列表（可以是预定义的智能体名称）
  "autoScaling": true,                // 是否根据数据量自动扩展子智能体数量
  "maxSubAgents": 10                  // 最大子智能体数量限制
}
```

### 3. 数据分片参数
```json
{
  "dataSplitting": {
    "splitStrategy": "AUTO_DETECT",   // 分片策略：AUTO_DETECT, FIXED_SIZE, CHAPTER_BASED
    "maxChunkSize": 5000,             // 最大块大小
    "chunkUnit": "CHARACTERS",        // 分片单位：CHARACTERS, WORDS, LINES
    "overlapSize": 100                // 分片间重叠大小（保持上下文连续性）
  }
}
```

### 4. 输入输出处理参数
```json
{
  "inputHandling": {
    "inputFormat": "AUTO_DETECT",     // 输入格式自动检测
    "encoding": "UTF-8",              // 文件编码
    "preprocessor": "TextNormalizer"  // 预处理器
  },
  "outputHandling": {
    "outputFormat": "JSON",           // 输出格式
    "aggregationStrategy": "MERGE",   // 结果聚合策略
    "outputKey": "parallel_result"    // 输出键名
  }
}
```

### 5. 容错和监控参数
```json
{
  "faultTolerance": {
    "retryPolicy": {
      "maxRetries": 3,
      "retryInterval": "5s"
    },
    "failureHandling": "SKIP_AND_CONTINUE"  // 失败处理策略
  },
  "monitoring": {
    "progressTracking": true,
    "performanceMetrics": true
  }
}
```

### 6. 资源管理参数
```json
{
  "resourceManagement": {
    "memoryPerTask": "512MB",
    "timeoutPerTask": "30m",
    "queueSize": 100
  }
}
```

## 简化配置映射

从 mapreduce_cmd.md 的简化配置到预制参数的映射：

```json
{
  "agentType": "PARALLEL_AGENT",
  "stepRequirement": "Map阶段：并行处理文件块和内容总结",
  "subAgents": [...]
}
```

**预制时需要内置的默认值：**
- `executionStrategy`: "CONCURRENT"
- `concurrencyLevel`: 从执行配置中读取或默认为 3
- `dataSplitting.maxChunkSize`: 5000（从子智能体配置推导）
- `dataSplitting.splitStrategy`: "AUTO_DETECT"
- 基本的容错和监控参数

**运行时动态配置：**
- `subAgents`: 从配置中读取
- `stepRequirement`: 作为任务描述传递给子智能体
- 具体的输入输出路径
