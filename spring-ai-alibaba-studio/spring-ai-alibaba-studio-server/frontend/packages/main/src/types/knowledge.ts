// Parameters for getting knowledge base list
export interface IGetKnowledgeListParams {
  keyword?: string;
  current: number;
  size: number;
}

// Interface for knowledge base list item
export interface IKnowledgeListItem {
  kb_id: string;
  name: string;
  description: string;
  gmt_modified: string;
  total_docs: number;
}

// Interface for document processing configuration
export interface IProcessConfig {
  chunk_type: string; // Currently defaults to "basic"
  chunk_size: number; // Chunk size
  chunk_overlap: number; // Chunk overlap size
}

// Interface for index configuration
export interface IndexConfig {
  embedding_provider: string; // Embedding model provider
  embedding_model: string; // Embedding model name
}

// Interface for search configuration
export interface ISearchConfig {
  top_k: number; // Number of chunks to retrieve
  similarity_threshold: number; // Similarity threshold, default 0.3
  search_type?: 'hybrid' | 'semantic' | 'full_text'; // Search type, default hybrid
  hybrid_weight?: number; // Hybrid search weight, default 0.7
  enable_rerank?: boolean; // Whether to enable rerank
  rerank_provider: string; // Rerank model provider
  rerank_model: string; // Rerank model
}

// Parameters for creating knowledge base
export interface ICreateKnowledgeListParams {
  name: string; // Knowledge base name
  description?: string; // Knowledge base description
  type?: 'structured' | 'unstructured'; // Knowledge base type
  kb_id?: string; // Knowledge base ID
  process_config?: IProcessConfig; // Document processing configuration
  index_config: IndexConfig; // Document indexing configuration
  search_config: ISearchConfig; // Document search configuration
}

// Generic paginated list response
export interface IPagingList<T> {
  current: number;
  size: number;
  total: number;
  records: T[];
}

// Knowledge base detail response
export interface IKnowledgeDetail {
  kb_id: string;
  name: string;
  description: string;
  type: 'structured' | 'unstructured';
  process_config: IProcessConfig;
  index_config: IndexConfig;
  search_config: ISearchConfig;
}

// Document upload policy
export interface IUploadPolicy {
  name: string; // File name
  path: string; // File upload path
  extension: string; // Document processing configuration rule
  size: number; // File size
}

// Parameters for creating document
export interface ICreateDocumentParams {
  kb_id: string; // Knowledge base ID
  type: 'file' | 'url'; // Document type, currently only file format is supported
  files: IUploadPolicy[]; // File list
  process_config?: IProcessConfig; // If not provided, uses knowledge base's default parsing and splitting rules
}

// Knowledge retrieval test response
export interface IKnowledgeRetrieve {
  chunk_id: string;
  doc_id: string;
  doc_name: string;
  page_number: number;
  score: number;
  text: string;
  title: string;
}

// Document chunk interface
export interface IDocumentChunk {
  chunk_id: string; // Chunk ID
  doc_id: string; // Document ID
  doc_name: string; // Document name
  title: string; // Chunk title
  text: string; // Chunk text content
  page_number: string; // Page number of the chunk
}

// Chunk list item response
export interface IChunksListItem {
  code: string; // Response code
  message: string; // Response message
  records: IDocumentChunk[]; // Paginated list
  request_id: string; // Request ID
  total: number; // Total number of chunks
}

// Parameters for updating chunk content
export interface IUpdateChunksContentParams {
  doc_id: string;
  chunk_id: string;
  title?: string;
  text: string;
  doc_name?: string;
}

// Parameters for updating chunk settings
export interface IUpdateChunksParams {
  doc_id: string;
  kb_id: string;
  process_config: {
    chunk_type: string;
    delimiter?: string;
    chunk_size: string;
    chunk_overlap: string;
  };
}

// Parameters for enabling/disabling chunks
export interface IUpdateStatusChunksParams {
  doc_id: string;
  chunk_ids: string[];
  enabled: boolean;
}
