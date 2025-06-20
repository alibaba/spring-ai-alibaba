# 自然语言转SQL模块 (NL2SQL)

## 模块简介

![img.png](img.png)

随着大模型技术的快速发展，自然语言到 SQL（NL2SQL）能力在数据分析领域的落地日益广泛。然而，传统 NL2SQL 方案存在Schema 理解偏差、复杂查询生成效率低、执行结果不可控等问题，导致业务场景中频繁出现“答非所问”或“生成失败”的窘境。为了让更多开发者能够便捷地使用这一能力，我们决定将[阿里云析言GBI](https://bailian.console.aliyun.com/xiyan#/home)中“Schema 召回 + SQL 生成 + SQL 执行”的核心链路模块化、组件化，并以开源的形式回馈社区。

本模块旨在提供一个轻量级的 **自然语言查询转 SQL 语句** 的服务，基于用户输入的自然语言问题，结合数据库 Schema 和业务逻辑解释（evidence），通过大模型推理生成对应的 SQL 查询语句，并支持执行该 SQL 返回结果。

该模块被设计为可复用的 Service 层组件，**仅提供核心功能实现，不包含 RESTful 接口及独立启动能力**。适用于集成到其他 Spring Boot 项目中使用。

---

## 功能特性

- **自然语言理解与关键词提取**
  - 支持从用户提问中提取关键词和时间表达式
  - 基于 Prompt 工程引导大模型准确识别意图

- **Schema 精准匹配**
  - 结合向量库召回相关表结构信息
  - 根据关键词和上下文筛选最相关的数据库表结构

- **SQL 生成**
  - 利用大模型生成符合语义的 SQL 查询语句
  - 支持嵌入业务逻辑解释（evidence）以提高准确性

- **SQL 执行与结果展示**
  - 支持直接执行生成的 SQL 并返回格式化结果（Markdown 表格）

---

## 技术栈

- **后端**: Java 17+ (Spring Boot)
- **依赖模块**: `com.alibaba.cloud.ai:common:${spring-ai-alibaba.version}`
- **大模型服务**: LLM（如 Qwen、DashScope）
- **数据库连接器**: MySQL / PostgreSQL
- **辅助工具**: Gson、Jackson、Markdown 解析器

---

## 使用说明

### 前置依赖

- [Java](https://www.oracle.com/java/technologies/javase-jdk17-downloads.html) >= 17
- [PostgreSQL](https://www.postgresql.org/) 或 [MySQL](https://www.mysql.com/)
- [Gradle](https://gradle.org/) 或 [Maven](https://maven.apache.org/) 构建工具（用于主项目的构建）

### 引入方式

将本模块作为依赖引入到你的 Spring Boot 项目中：

#### Maven 示例：

```xml
<dependency>
  <groupId>com.alibaba.cloud.ai</groupId>
  <artifactId>spring-ai-alibaba-starter-nl2sql</artifactId>
  <version>${spring-ai-alibaba.version}</version>
</dependency>
```

#### Gradle 示例：

```groovy
implementation 'com.alibaba.cloud.ai:spring-ai-alibaba-starter-nl2sql:${spring-ai-alibaba.version}'
```

---

## 配置说明

目前支持两种向量存储方式：
- **AnalyticDB**（推荐生产环境，支持大规模数据和高性能检索）
- **SimpleVector**（适合本地开发、测试或小规模场景，无需依赖外部数据库）

### AnalyticDB 配置示例

```yaml
spring:
  ai:
    vectorstore:
      analytic:
        collectName: chatbi
        regionId: cn-hangzhou
        dbInstanceId: gp-bp11vjucxhw757v9p
        managerAccount: 
        managerAccountPassword: 
        namespace: 
        namespacePassword: 
        defaultTopK: 10
        defaultSimilarityThreshold: 0.01
        accessKeyId: 
        accessKeySecret: 
```

> ⚠️ 注意：AnalyticDB 需提前开启向量引擎优化，详见[官方文档](https://help.aliyun.com/zh/analyticdb/analyticdb-for-postgresql/getting-started/create-an-instance-instances-with-vector-engine-optimization-enabled)。SimpleVector 适合本地开发和小数据量测试，不建议用于生产环境。

### SimpleVector 配置示例

无需配置，默认启动 SimpleVector

### 数据库连接配置 (`application.yml`)

```yaml
spring:
  ai:
    openai:
      base-url: https://dashscope.aliyuncs.com/compatible-mode #类似 OpenAI 接口风格的兼容地址，这里指向的是阿里云 DashScope 的兼容接口。
      api-key: sk
      model: qwen-max #使用的模型名称，推荐使用：qwen-max: 适合复杂任务（如 NL2SQL）qwen-plus: 平衡性能与成本
    dashscope:
      api-key: sk  #DashScope 平台的 API Key，用于调用 Qwen 等模型。获取方式：登录 DashScope 控制台 → 查看或创建 API Key。
    vectorstore:
      analytic:
        collectName: chatbi #向量集合名称，即你要写入数据的“collection”名，例如 chatbi
        regionId: cn-hangzhou #实例所在的区域 ID，比如 cn-hangzhou（杭州）、cn-beijing（北京）等。
        dbInstanceId: gp-bp11vjucxhw757v9p #AnalyticDB PostgreSQL 实例 ID，例如 gp-bp11vjucxhw757v9p
        managerAccount: #实例的管理员账号。
        managerAccountPassword: #实例的管理员密码。
        namespace: #命名空间信息，用于隔离不同用户的向量数据
        namespacePassword: 
        defaultTopK: 10 #默认返回的相似向量数量。
        defaultSimilarityThreshold: 0.01 #通常设为 0.01 到 0.75 之间，根据实际效果调整。
        accessKeyId: #阿里云主账号或 RAM 用户的 AK 信息
        accessKeySecret: 
chatbi:
  dbconfig:
    url: jdbc:mysql://host:port/database #数据库 JDBC 连接地址，示例：MySQL: jdbc:mysql://host:port/databasePostgreSQL: jdbc:postgresql://host:port/database
    username: #数据库用户名
    password: #数据库用户密码
    connectiontype: jdbc
    dialecttype: mysql #数据库类型，可选：postgresql、mysql
    schema: #postgresql类型所需要的schema名称

```

### 大模型服务配置（LLM）

请确保 `LlmService` 实现类已注入容器，并支持调用大模型 API。

### 向量服务配置

确保 `VectorStoreService` 及其依赖项（如 `AnalyticDbVectorStoreProperties`）已正确配置并可用。

---

## 核心类说明

- **`BaseNl2SqlService`**
  - 主要对外接口服务类，提供以下方法：
    - `nl2sql(String query)`：入口方法，接收自然语言问题，返回格式化结果

---

## NL2SQl Graph

### 设计

```plantuml
@startuml
skinparam usecaseFontSize 14
skinparam usecaseStereotypeFontSize 12
skinparam hexagonFontSize 14
skinparam hexagonStereotypeFontSize 12
title "workflow graph"
footer

powered by spring-ai-alibaba
end footer
circle start<<input>> as __START__
circle stop as __END__
usecase "QUERY_REWRITE_NODE"<<Node>>
usecase "KEYWORD_EXTRACT_NODE"<<Node>>
usecase "SCHEMA_RECALL_NODE"<<Node>>
usecase "TABLE_RELATION_NODE"<<Node>>
usecase "SQL_GENERATE_NODE"<<Node>>
usecase "SQL_VALIDATE_NODE"<<Node>>
usecase "SEMANTIC_CONSISTENC_NODE"<<Node>>
hexagon "check state" as condition1<<Condition>>
hexagon "check state" as condition2<<Condition>>
hexagon "check state" as condition3<<Condition>>
hexagon "check state" as condition4<<Condition>>
"__START__" -down-> "QUERY_REWRITE_NODE"
"QUERY_REWRITE_NODE" .down.> "condition1"
"condition1" .down.> "__END__"
'"QUERY_REWRITE_NODE" .down.> "__END__"
"condition1" .down.> "KEYWORD_EXTRACT_NODE"
'"QUERY_REWRITE_NODE" .down.> "KEYWORD_EXTRACT_NODE"
"KEYWORD_EXTRACT_NODE" -down-> "SCHEMA_RECALL_NODE"
"SCHEMA_RECALL_NODE" -down-> "TABLE_RELATION_NODE"
"TABLE_RELATION_NODE" -down-> "SQL_GENERATE_NODE"
"SQL_GENERATE_NODE" .down.> "condition2"
"condition2" .down.> "__END__"
'"SQL_GENERATE_NODE" .down.> "__END__"
"condition2" .down.> "SQL_VALIDATE_NODE"
'"SQL_GENERATE_NODE" .down.> "SQL_VALIDATE_NODE"
"condition2" .down.> "KEYWORD_EXTRACT_NODE"
'"SQL_GENERATE_NODE" .down.> "KEYWORD_EXTRACT_NODE"
"SQL_VALIDATE_NODE" .down.> "condition3"
"condition3" .down.> "SEMANTIC_CONSISTENC_NODE"
'"SQL_VALIDATE_NODE" .down.> "SEMANTIC_CONSISTENC_NODE"
"condition3" .down.> "SQL_GENERATE_NODE"
'"SQL_VALIDATE_NODE" .down.> "SQL_GENERATE_NODE"
"SEMANTIC_CONSISTENC_NODE" .down.> "condition4"
"condition4" .down.> "SQL_GENERATE_NODE"
'"SEMANTIC_CONSISTENC_NODE" .down.> "SQL_GENERATE_NODE"
"condition4" .down.> "__END__"
'"SEMANTIC_CONSISTENC_NODE" .down.> "__END__"
@enduml
```

### 调用方法

```java

import com.alibaba.cloud.ai.dbconnector.DbConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.request.SchemaInitRequest;
import com.alibaba.cloud.ai.service.simple.SimpleVectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static com.alibaba.cloud.ai.constant.Constant.INPUT_KEY;
import static com.alibaba.cloud.ai.constant.Constant.RESULT;

/**
 * @author zhangshenghang
 */
@RestController
@RequestMapping("nl2sql")
public class Nl2sqlController {

	private static final Logger logger = LoggerFactory.getLogger(Nl2sqlController.class);

	private final CompiledGraph compiledGraph;

	@Autowired
	private SimpleVectorStoreService simpleVectorStoreService;

	@Autowired
	private DbConfig dbConfig;

	@Autowired
	public Nl2sqlController(@Qualifier("nl2sqlGraph") StateGraph stateGraph) throws GraphStateException {
		this.compiledGraph = stateGraph.compile();
		this.compiledGraph.setMaxIterations(100);
	}

	@GetMapping("/search")
	public String search(@RequestParam String query) throws Exception {
		SchemaInitRequest schemaInitRequest = new SchemaInitRequest();
		schemaInitRequest.setDbConfig(dbConfig);
		schemaInitRequest
			.setTables(Arrays.asList("categories", "order_items", "orders", "products", "users", "product_categories"));
		simpleVectorStoreService.schema(schemaInitRequest);

		Optional<OverAllState> invoke = compiledGraph.invoke(Map.of(INPUT_KEY, query));
		OverAllState overAllState = invoke.get();
		return overAllState.value(RESULT).get().toString();
	}

}
```

### 效果

> 目前只有后台日志输出，暂不支持前端展示。

```text
2025-06-18T23:34:38.463+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.QueryRewriteNode       : 进入 QueryRewriteNode 节点
2025-06-18T23:34:38.463+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.QueryRewriteNode       : [QueryRewriteNode] 处理用户输入: 查询每个分类下已经成交且销量最高的商品及其销售总量，每个分类只返回销量最高的商品。
2025-06-18T23:34:46.044+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.QueryRewriteNode       : [QueryRewriteNode] 问题重写结果: 查询每个分类下已经成交且销量最高的商品及其销售总量，每个分类只返回销量最高的商品。
2025-06-18T23:34:46.047+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.c.a.d.QueryRewriteDispatcher         : 【QueryRewriteDispatcher】进入KEYWORD_EXTRACT_NODE节点
2025-06-18T23:34:46.050+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.KeywordExtractNode     : 进入 KeywordExtractNode 节点
2025-06-18T23:34:47.461+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.KeywordExtractNode     : evidences：[] , keywords: [每个分类, 已成交, 销量最高, 商品, 销售总量]
2025-06-18T23:34:47.462+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.KeywordExtractNode     : KeywordExtractNode 节点输出 evidences：[] , keywords: [每个分类, 已成交, 销量最高, 商品, 销售总量]
2025-06-18T23:34:47.462+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.SchemaRecallNode       : 进入 SchemaRecallNode 节点
2025-06-18T23:34:48.346+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.SchemaRecallNode       : [SchemaRecallNode] Schema召回结果 - 表文档数量: 6, 关键词相关列文档组数: 5
2025-06-18T23:34:48.359+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.TableRelationNode      : 进入 TableRelationNode 节点
2025-06-18T23:34:48.362+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.TableRelationNode      : [TableRelationNode] 执行常规Schema选择
2025-06-18T23:34:49.817+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.TableRelationNode      : [TableRelationNode] Schema处理结果: SchemaDTO(name=nl2sql, description=null, tableCount=null, table=[TableDTO(name=categories, description=商品分类表, column=[ColumnDTO(name=name, description=分类名称, enumeration=0, range=null, type=text, samples=null, data=null, mapping=null), ColumnDTO(name=id, description=分类ID，主键自增, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null)], primaryKeys=[id]), TableDTO(name=product_categories, description=商品与分类关联表, column=[ColumnDTO(name=product_id, description=商品ID, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=category_id, description=分类ID, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null)], primaryKeys=[product_id]), TableDTO(name=products, description=商品表, column=[ColumnDTO(name=id, description=商品ID，主键自增, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=created_at, description=商品上架时间, enumeration=0, range=null, type=datetime, samples=null, data=null, mapping=null), ColumnDTO(name=price, description=商品单价, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=stock, description=商品库存数量, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=name, description=商品名称, enumeration=0, range=null, type=text, samples=null, data=null, mapping=null)], primaryKeys=[id]), TableDTO(name=order_items, description=订单明细表, column=[ColumnDTO(name=id, description=订单明细ID，主键自增, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=quantity, description=购买数量, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=unit_price, description=下单时商品单价, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=product_id, description=商品ID, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=order_id, description=订单ID, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null)], primaryKeys=[id]), TableDTO(name=orders, description=订单表, column=[ColumnDTO(name=user_id, description=下单用户ID, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=id, description=订单ID，主键自增, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=order_date, description=下单时间, enumeration=0, range=null, type=datetime, samples=null, data=null, mapping=null), ColumnDTO(name=total_amount, description=订单总金额, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=status, description=订单状态（pending/completed/cancelled等）, enumeration=0, range=null, type=text, samples=null, data=null, mapping=null)], primaryKeys=[id])], foreignKeys=[[order_items.order_id=orders.id, product_categories.category_id=categories.id, orders.user_id=users.id, product_categories.product_id=products.id, order_items.product_id=products.id]])
2025-06-18T23:34:49.829+08:00  INFO 8496 --- [nio-8080-exec-1] c.alibaba.cloud.ai.node.SqlGenerateNode  : 进入 SqlGenerateNode 节点
2025-06-18T23:34:51.941+08:00  INFO 8496 --- [nio-8080-exec-1] c.alibaba.cloud.ai.node.SqlGenerateNode  : 召回信息是否满足需求：否，原因：问题中涉及的“销量”字段未直接存在于schema中，且无法通过现有字段推导出销量定义。
2025-06-18T23:34:51.943+08:00  INFO 8496 --- [nio-8080-exec-1] c.alibaba.cloud.ai.node.SqlGenerateNode  : 首次生成SQL
2025-06-18T23:34:51.944+08:00  INFO 8496 --- [nio-8080-exec-1] c.alibaba.cloud.ai.node.SqlGenerateNode  : 召回信息不满足需求，开始重新生成SQL
2025-06-18T23:34:51.946+08:00  INFO 8496 --- [nio-8080-exec-1] c.alibaba.cloud.ai.node.SqlGenerateNode  : 召回信息不满足需求，需要补充Schema信息
2025-06-18T23:34:51.947+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.c.a.d.SqlGenerateDispatcher          : SQL 生成结果: SQL_GENERATE_SCHEMA_MISSING
2025-06-18T23:34:51.947+08:00  WARN 8496 --- [nio-8080-exec-1] c.a.c.a.d.SqlGenerateDispatcher          : SQL生成缺少Schema，跳转到KEYWORD_EXTRACT_NODE节点
2025-06-18T23:34:51.951+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.KeywordExtractNode     : 进入 KeywordExtractNode 节点
2025-06-18T23:34:53.383+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.KeywordExtractNode     : evidences：[] , keywords: [每个分类, 已成交, 销量最高, 商品, 销售总量, 只返回销量最高的商品]
2025-06-18T23:34:53.384+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.KeywordExtractNode     : Schema 召回缺失补充
2025-06-18T23:34:54.762+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.KeywordExtractNode     : Schema 召回缺失补充 keywords: [销量, 字段, schema, 推导, 销量定义]
2025-06-18T23:34:54.762+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.KeywordExtractNode     : KeywordExtractNode 节点输出 evidences：[] , keywords: [每个分类, 已成交, 销量最高, 商品, 销售总量, 只返回销量最高的商品, 销量, 字段, schema, 推导, 销量定义]
2025-06-18T23:34:54.764+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.SchemaRecallNode       : 进入 SchemaRecallNode 节点
2025-06-18T23:34:56.350+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.SchemaRecallNode       : [SchemaRecallNode] Schema召回结果 - 表文档数量: 6, 关键词相关列文档组数: 11
2025-06-18T23:34:56.361+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.TableRelationNode      : 进入 TableRelationNode 节点
2025-06-18T23:34:56.363+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.TableRelationNode      : [TableRelationNode] 使用Schema补充建议处理: 否，原因：问题中涉及的“销量”字段未直接存在于schema中，且无法通过现有字段推导出销量定义。
2025-06-18T23:34:58.696+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.TableRelationNode      : [TableRelationNode] Schema处理结果: SchemaDTO(name=nl2sql, description=null, tableCount=null, table=[TableDTO(name=categories, description=商品分类表, column=[ColumnDTO(name=name, description=分类名称, enumeration=0, range=null, type=text, samples=null, data=null, mapping=null), ColumnDTO(name=id, description=分类ID，主键自增, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null)], primaryKeys=[id]), TableDTO(name=product_categories, description=商品与分类关联表, column=[ColumnDTO(name=product_id, description=商品ID, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=category_id, description=分类ID, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null)], primaryKeys=[product_id]), TableDTO(name=products, description=商品表, column=[ColumnDTO(name=id, description=商品ID，主键自增, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=created_at, description=商品上架时间, enumeration=0, range=null, type=datetime, samples=null, data=null, mapping=null), ColumnDTO(name=price, description=商品单价, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=stock, description=商品库存数量, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=name, description=商品名称, enumeration=0, range=null, type=text, samples=null, data=null, mapping=null)], primaryKeys=[id]), TableDTO(name=order_items, description=订单明细表, column=[ColumnDTO(name=id, description=订单明细ID，主键自增, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=quantity, description=购买数量, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=unit_price, description=下单时商品单价, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=product_id, description=商品ID, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=order_id, description=订单ID, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null)], primaryKeys=[id]), TableDTO(name=orders, description=订单表, column=[ColumnDTO(name=user_id, description=下单用户ID, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=id, description=订单ID，主键自增, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=order_date, description=下单时间, enumeration=0, range=null, type=datetime, samples=null, data=null, mapping=null), ColumnDTO(name=total_amount, description=订单总金额, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=status, description=订单状态（pending/completed/cancelled等）, enumeration=0, range=null, type=text, samples=null, data=null, mapping=null)], primaryKeys=[id])], foreignKeys=[[order_items.order_id=orders.id, product_categories.category_id=categories.id, orders.user_id=users.id, product_categories.product_id=products.id, order_items.product_id=products.id]])
2025-06-18T23:34:58.698+08:00  INFO 8496 --- [nio-8080-exec-1] c.alibaba.cloud.ai.node.SqlGenerateNode  : 进入 SqlGenerateNode 节点
2025-06-18T23:35:00.761+08:00  INFO 8496 --- [nio-8080-exec-1] c.alibaba.cloud.ai.node.SqlGenerateNode  : 召回信息是否满足需求：否，因为问题中需要的“users”表在schema中未定义，且多表查询中的连接逻辑无法完全推导。
2025-06-18T23:35:00.762+08:00  INFO 8496 --- [nio-8080-exec-1] c.alibaba.cloud.ai.node.SqlGenerateNode  : SQL生成次数增加到: 2
2025-06-18T23:35:00.763+08:00  INFO 8496 --- [nio-8080-exec-1] c.alibaba.cloud.ai.node.SqlGenerateNode  : 召回信息不满足需求，开始重新生成SQL
2025-06-18T23:35:00.763+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.c.a.d.SqlGenerateDispatcher          : SQL 生成结果: SQL_GENERATE_SCHEMA_MISSING
2025-06-18T23:35:00.763+08:00  WARN 8496 --- [nio-8080-exec-1] c.a.c.a.d.SqlGenerateDispatcher          : SQL生成缺少Schema，跳转到KEYWORD_EXTRACT_NODE节点
2025-06-18T23:35:00.766+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.KeywordExtractNode     : 进入 KeywordExtractNode 节点
2025-06-18T23:35:02.204+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.KeywordExtractNode     : evidences：[] , keywords: [每个分类, 已成交, 销量最高, 商品, 销售总量, 只返回销量最高的商品]
2025-06-18T23:35:02.206+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.KeywordExtractNode     : Schema 召回缺失补充
2025-06-18T23:35:04.119+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.KeywordExtractNode     : Schema 召回缺失补充 keywords: [销量, schema, users表, 多表查询, 连接逻辑]
2025-06-18T23:35:04.120+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.KeywordExtractNode     : KeywordExtractNode 节点输出 evidences：[] , keywords: [每个分类, 已成交, 销量最高, 商品, 销售总量, 只返回销量最高的商品, 销量, schema, users表, 多表查询, 连接逻辑]
2025-06-18T23:35:04.123+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.SchemaRecallNode       : 进入 SchemaRecallNode 节点
2025-06-18T23:35:05.816+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.SchemaRecallNode       : [SchemaRecallNode] Schema召回结果 - 表文档数量: 6, 关键词相关列文档组数: 11
2025-06-18T23:35:05.825+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.TableRelationNode      : 进入 TableRelationNode 节点
2025-06-18T23:35:05.826+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.TableRelationNode      : [TableRelationNode] 使用Schema补充建议处理: 否，原因：问题中涉及的“销量”字段未直接存在于schema中，且无法通过现有字段推导出销量定义。
否，因为问题中需要的“users”表在schema中未定义，且多表查询中的连接逻辑无法完全推导。
2025-06-18T23:35:07.764+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.cloud.ai.node.TableRelationNode      : [TableRelationNode] Schema处理结果: SchemaDTO(name=nl2sql, description=null, tableCount=null, table=[TableDTO(name=categories, description=商品分类表, column=[ColumnDTO(name=name, description=分类名称, enumeration=0, range=null, type=text, samples=null, data=null, mapping=null), ColumnDTO(name=id, description=分类ID，主键自增, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null)], primaryKeys=[id]), TableDTO(name=product_categories, description=商品与分类关联表, column=[ColumnDTO(name=product_id, description=商品ID, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=category_id, description=分类ID, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null)], primaryKeys=[product_id]), TableDTO(name=products, description=商品表, column=[ColumnDTO(name=id, description=商品ID，主键自增, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=created_at, description=商品上架时间, enumeration=0, range=null, type=datetime, samples=null, data=null, mapping=null), ColumnDTO(name=price, description=商品单价, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=stock, description=商品库存数量, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=name, description=商品名称, enumeration=0, range=null, type=text, samples=null, data=null, mapping=null)], primaryKeys=[id]), TableDTO(name=order_items, description=订单明细表, column=[ColumnDTO(name=id, description=订单明细ID，主键自增, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=quantity, description=购买数量, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=unit_price, description=下单时商品单价, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=product_id, description=商品ID, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=order_id, description=订单ID, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null)], primaryKeys=[id]), TableDTO(name=orders, description=订单表, column=[ColumnDTO(name=user_id, description=下单用户ID, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=id, description=订单ID，主键自增, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=order_date, description=下单时间, enumeration=0, range=null, type=datetime, samples=null, data=null, mapping=null), ColumnDTO(name=total_amount, description=订单总金额, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=status, description=订单状态（pending/completed/cancelled等）, enumeration=0, range=null, type=text, samples=null, data=null, mapping=null)], primaryKeys=[id]), TableDTO(name=users, description=用户表, column=[ColumnDTO(name=email, description=用户邮箱, enumeration=0, range=null, type=text, samples=null, data=null, mapping=null), ColumnDTO(name=username, description=用户名, enumeration=0, range=null, type=text, samples=null, data=null, mapping=null), ColumnDTO(name=id, description=用户ID，主键自增, enumeration=0, range=null, type=number, samples=null, data=null, mapping=null), ColumnDTO(name=created_at, description=用户注册时间, enumeration=0, range=null, type=datetime, samples=null, data=null, mapping=null)], primaryKeys=[id])], foreignKeys=[[order_items.order_id=orders.id, product_categories.category_id=categories.id, orders.user_id=users.id, product_categories.product_id=products.id, order_items.product_id=products.id]])
2025-06-18T23:35:07.785+08:00  INFO 8496 --- [nio-8080-exec-1] c.alibaba.cloud.ai.node.SqlGenerateNode  : 进入 SqlGenerateNode 节点
2025-06-18T23:35:08.612+08:00  INFO 8496 --- [nio-8080-exec-1] c.alibaba.cloud.ai.node.SqlGenerateNode  : 召回信息是否满足需求：是
2025-06-18T23:35:08.612+08:00  INFO 8496 --- [nio-8080-exec-1] c.alibaba.cloud.ai.node.SqlGenerateNode  : 开始生成SQL
2025-06-18T23:35:17.558+08:00  INFO 8496 --- [nio-8080-exec-1] c.alibaba.cloud.ai.node.SqlGenerateNode  : 生成的SQL为：WITH CategorySales AS (
    SELECT
        c.id AS category_id,
        c.name AS category_name,
        p.id AS product_id,
        p.name AS product_name,
        SUM(oi.quantity) AS total_sales
    FROM
        categories c
    JOIN
        product_categories pc ON c.id = pc.category_id
    JOIN
        products p ON pc.product_id = p.id
    JOIN
        order_items oi ON p.id = oi.product_id
    JOIN
        orders o ON oi.order_id = o.id
    WHERE
        o.status = 'completed'
    GROUP BY
        c.id, c.name, p.id, p.name
),
MaxSalesPerCategory AS (
    SELECT
        category_id,
        MAX(total_sales) AS max_sales
    FROM
        CategorySales
    GROUP BY
        category_id
)
SELECT
    cs.category_id,
    cs.category_name,
    cs.product_id,
    cs.product_name,
    cs.total_sales
FROM
    CategorySales cs
JOIN
    MaxSalesPerCategory ms ON cs.category_id = ms.category_id AND cs.total_sales = ms.max_sales
2025-06-18T23:35:17.558+08:00  INFO 8496 --- [nio-8080-exec-1] c.alibaba.cloud.ai.node.SqlGenerateNode  : SqlGenerateNode 节点执行完成
2025-06-18T23:35:17.560+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.c.a.d.SqlGenerateDispatcher          : SQL 生成结果: WITH CategorySales AS (
    SELECT
        c.id AS category_id,
        c.name AS category_name,
        p.id AS product_id,
        p.name AS product_name,
        SUM(oi.quantity) AS total_sales
    FROM
        categories c
    JOIN
        product_categories pc ON c.id = pc.category_id
    JOIN
        products p ON pc.product_id = p.id
    JOIN
        order_items oi ON p.id = oi.product_id
    JOIN
        orders o ON oi.order_id = o.id
    WHERE
        o.status = 'completed'
    GROUP BY
        c.id, c.name, p.id, p.name
),
MaxSalesPerCategory AS (
    SELECT
        category_id,
        MAX(total_sales) AS max_sales
    FROM
        CategorySales
    GROUP BY
        category_id
)
SELECT
    cs.category_id,
    cs.category_name,
    cs.product_id,
    cs.product_name,
    cs.total_sales
FROM
    CategorySales cs
JOIN
    MaxSalesPerCategory ms ON cs.category_id = ms.category_id AND cs.total_sales = ms.max_sales
2025-06-18T23:35:17.560+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.c.a.d.SqlGenerateDispatcher          : SQL生成成功，进入SQL校验节点: SQL_VALIDATE_NODE
2025-06-18T23:35:17.562+08:00  INFO 8496 --- [nio-8080-exec-1] c.alibaba.cloud.ai.node.SqlValidateNode  : 进入 SqlValidateNode 节点
2025-06-18T23:35:17.562+08:00  INFO 8496 --- [nio-8080-exec-1] c.alibaba.cloud.ai.node.SqlValidateNode  : [SqlValidateNode] 开始验证SQL语句: WITH CategorySales AS (
    SELECT
        c.id AS category_id,
        c.name AS category_name,
        p.id AS product_id,
        p.name AS product_name,
        SUM(oi.quantity) AS total_sales
    FROM
        categories c
    JOIN
        product_categories pc ON c.id = pc.category_id
    JOIN
        products p ON pc.product_id = p.id
    JOIN
        order_items oi ON p.id = oi.product_id
    JOIN
        orders o ON oi.order_id = o.id
    WHERE
        o.status = 'completed'
    GROUP BY
        c.id, c.name, p.id, p.name
),
MaxSalesPerCategory AS (
    SELECT
        category_id,
        MAX(total_sales) AS max_sales
    FROM
        CategorySales
    GROUP BY
        category_id
)
SELECT
    cs.category_id,
    cs.category_name,
    cs.product_id,
    cs.product_name,
    cs.total_sales
FROM
    CategorySales cs
JOIN
    MaxSalesPerCategory ms ON cs.category_id = ms.category_id AND cs.total_sales = ms.max_sales
2025-06-18T23:35:17.584+08:00  INFO 8496 --- [nio-8080-exec-1] com.alibaba.druid.pool.DruidDataSource   : {dataSource-38} inited
2025-06-18T23:35:17.601+08:00  INFO 8496 --- [nio-8080-exec-1] c.alibaba.cloud.ai.node.SqlValidateNode  : [SqlValidateNode] SQL语法验证通过
2025-06-18T23:35:17.601+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.c.a.d.SqlValidateDispatcher          : SQL语法校验是否通过: true
2025-06-18T23:35:17.601+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.c.a.d.SqlValidateDispatcher          : [SqlValidateDispatcher] SQL语法校验通过，跳转到节点: SEMANTIC_CONSISTENC_NODE
2025-06-18T23:35:17.604+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.c.ai.node.SemanticConsistencNode     : 进入 SemanticConsistencNode 节点
2025-06-18T23:35:18.267+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.c.ai.node.SemanticConsistencNode     : 语义一致性校验结果详情: 通过
2025-06-18T23:35:18.267+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.c.ai.node.SemanticConsistencNode     : 语义一致性校验结果: true
2025-06-18T23:35:18.268+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.c.a.d.SemanticConsistenceDispatcher  : 语义一致性校验结果: true，跳转节点配置
2025-06-18T23:35:18.268+08:00  INFO 8496 --- [nio-8080-exec-1] c.a.c.a.d.SemanticConsistenceDispatcher  : 语义一致性校验通过，跳转到结束节点。
```

## 典型使用流程

1. 用户输入自然语言问题，例如：“最近一周销售额最高的产品是哪些？”
2. 模块自动提取关键词“销售额”、“产品”、“最近一周”
3. 结合数据库 Schema 和 evidence 进行表结构筛选
4. 生成对应的 SQL 查询语句
5. 执行 SQL 并返回 Markdown 格式的表格结果

---

## 贡献指南

欢迎参与本模块的开发与优化！请参考 [Spring AI Alibaba 贡献指南](https://github.com/alibaba/spring-ai-alibaba/blob/main/CONTRIBUTING.md) 了解如何参与开源项目的开发。

---

## 许可证

本项目采用 [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) 开源协议。

---

## 联系方式

如有任何问题，请联系：

- 邮箱: kunan.lw@alibaba-inc.com
- GitHub: [willyomg](https://github.com/willyomg)

- 邮箱: xuqirui.xqr@alibaba-inc.com
- GitHub: [littleahri](https://github.com/littleahri)
