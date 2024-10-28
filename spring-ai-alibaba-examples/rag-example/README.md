### Spring Ai Alibaba Rag Example ###

This section will describe how to create example and call rag service. 

##### Local Rag Example #####

Local rag example includes two main flows, import document and rag.

Import document includes the following steps:
1. Parse document.
2. Split document to chunks with proper chunk size and delimiters.
3. Convert chunk text to embed vector.
4. Save text and embed vector to vector db including metadata if needed.

Rag includes the following steps:
1. Retrieval trunks from vector db with query.
2. Rerank the retrieved trunks to get score of relevance between query and retrieved trunks.
3. Generate result based on filtered trunks.

For how to run and test local rag example, please refer to the following instructions:
```
1. start application.
2. import document by using curl http request.
curl -X GET http://127.0.0.1:8080/ai/rag/importDocument

3. retrieval and generation
curl -G 'http://127.0.0.1:8080/ai/rag' --data-urlencode 'message=如何快速开始spring ai alibaba'
```

##### Cloud Rag Example #####

Cloud rag example includes two main flows, import document and rag.
Import document includes the following steps:
1. Parse document and split document to chunks with proper chunk size and delimiters on cloud side.
2. Add chunks to cloud vector db(Chunks will be converted to embed vector on cloud side).

Rag includes the following steps:
1. Retrieval trunks from vector db with query.
2. Generate result based on filtered trunks.

For how to run and test cloud rag example, please refer to the following instructions:
```
1. start application.
2. import document by using curl http request.
curl -X GET http://127.0.0.1:8080/ai/cloud/rag/importDocument

3. retrieval and generation
curl -G 'http://127.0.0.1:8080/ai/cloud/rag' --data-urlencode 'message=如何快速开始spring ai alibaba'
```
