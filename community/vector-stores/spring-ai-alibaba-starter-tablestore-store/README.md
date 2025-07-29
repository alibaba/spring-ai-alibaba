# TablestoreVectorStore

底层依托 [alibabacloud-tablestore-for-agent-memory](https://github.com/aliyun/alibabacloud-tablestore-for-agent-memory) 进行知识库文档管理和相似性搜索, 常用在RAG、AI搜索、多模态搜索等领域。 

# 使用文档

[TablestoreVectorStore 示例代码](src/test/java/com/alibaba/cloud/ai/vectorstore/tablestore/example/TablestoreVectorStoreExample.java)


## 主要特性

- **Tablestore 存储**：高性能、低成本、高召回率
- **多租户设计**: 支持多租户场景的专属优化，每个租户可以独立管理自己的知识库，提高性能和召回率。
- **与 Spring 生态无缝集成**：完美兼容 Spring 框架和 Spring Boot 应用
- **更完善的 VectorStore ( Knowledge ) 接口设计**: 可以查看 Github [alibabacloud-tablestore-for-agent-memory](https://github.com/aliyun/alibabacloud-tablestore-for-agent-memory) 查看更丰富的 Knowledge 能力和使用文档
