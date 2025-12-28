# 链路追踪页面

## 接口在 services/tracing/index.ts

### 接口目前没数据， 请先 mock

## 需求

1. 选择时间返回， 默认为当前的 24 小时

2. 概览， 包含一下数据展示
  - Trace 总数
  - Token 消耗
  - 模型调用次数

3. 筛选区域, 支持以下筛选

  - 来源类型（固定）： Prompt 调试、Agent 上报
  - 服务名称 getServices 接口获取
  - TraceId 
  - Span名称 getServices 接口获取 operations 所有的项
  - 高级筛选区域：
    - 属性：PromptKey、Prompt版本、模型
    - 属性值： 输入框

4. 列表区域， 包含以下列
  - TraceID
  - 来源类型
  - 服务/Span
  - 开始时间
  - 持续时间
  - Token 数
  - 模型
  - 状态
  - 操作
    - 查看详情


5. 查看详情，使用 Drawer 组件展示

 标题部分为： “Trace 详情” + traceId

 基本信息区域: 包含以下字段展示
  TraceId
  来源类型
  服务
  状态
  模型
  开始时间
  结束时间
  持续时间
  Token 数
  Prompt

Span 瀑布图区域
该区域需要调用 getTraceDetail 接口， 并根据 records 中的 parentId 将平铺的列表组装成 tree 数据结构, 注意同层级的节点需要按开始时间排序
并展示在页面上

Span详情区域
当点击 Span tree 的层级时， 展示该层级 span 的详细信息， 分为下面几个区域
基本信息区域， 包含 SpanId、类型、Kind、状态、开始时间、结束时间、持续时间

属性区域， 展示该 Span 的属性

事件区域， 使用纵向时间轴来展示， 包含名称、日期、属性