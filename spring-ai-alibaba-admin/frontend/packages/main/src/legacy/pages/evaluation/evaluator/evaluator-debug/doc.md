# 调试页面

## 该页面分为两个区域

### 左侧展示评估器配置信息， 包括

 - 模型
 - promot
 - 参数

### 右侧为沟槽测试数据区域， 包含三个输入区域

 - input 必传
 - output 必传
 - reference_output 可选
支持清空和运行， 运行后需要展示评测结果, 运行调用 debugEvaluator 接口
