import { FileType, TaskStatus } from '@/types/base';

export interface IFileItem {
  /**
   * Data ID
   */
  kb_id: string;
  /**
   * File name
   */
  name: string;
  /**
   * Status
   */
  index_status: TaskStatus;
  /**
   * Format
   */
  format: FileType;
  /**
   * Size
   */
  size: number;
  /**
   * Whether enabled
   */
  enabled: boolean;

  /**
   * Document metadata
   */
  metadata: IMetadata;
  /**
   * Document ID
   */
  doc_id: string;
}

export interface IMetadata {
  content_type: string;
  size: number;
}

export interface IChunkItem {
  /**
   * Data ID
   */
  chunk_id?: string;
  /**
   * Whether web search is enabled
   */
  webSearch?: boolean;
  /**
   * Whether to display
   */
  display?: boolean;
  /**
   * Whether to enable specified model
   */
  enableSpecifiedModel?: boolean;
  /**
   * Rejection status
   */
  rejectStatus?: boolean;
  /**
   * Document name
   */
  doc_name?: string;
  /**
   * Knowledge base ID
   */
  knowledgeBaseId?: string;
  /**
   * ID
   */
  doc_id?: string;
  /**
   * Title
   */
  title: string;
  /**
   * Content
   */
  content: string;
  /**
   * Similarity score
   */
  score?: number;
  /**
   * Toggle status
   */
  enabled?: boolean;
  /**
   * ID
   */
  id?: string;
  /**
   * Content text
   */
  text?: string;
}

export type KnowledgeType = 'unstructured' | 'structured';

export interface IKnowledge {
  /**
   * Type
   */
  type: KnowledgeType;
  /**
   * Name
   */
  name: string;
  /**
   * Description
   */
  description: string;
  /**
   * Processing configuration
   */
  process_config: any;
  /**
   * Index configuration
   */
  index_config: any;
  /**
   * Search configuration
   */
  search_config: any;
}
