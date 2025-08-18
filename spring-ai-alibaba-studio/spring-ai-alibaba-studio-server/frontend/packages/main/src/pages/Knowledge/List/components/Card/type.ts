export interface IKnowledgeCard {
  /**
   * Knowledge base ID
   */
  kb_id: string;
  /**
   * Knowledge base name
   */
  name: string;
  /**
   * Knowledge base description
   */
  description: string;
  /**
   * Update time
   */
  gmt_modified: string;
  /**
   * Number of documents
   */
  total_docs: number;
  /**
   * id
   */
  id?: string;
}

export interface KnowledgeCardProps extends IKnowledgeCard {
  /**
   * Card click callback
   */
  onClick?: () => void;

  /**
   * View click callback
   */
  onView?: () => void;
  /**
   * Hit test click callback
   */
  onHitTest?: () => void;
  /**
   * Edit click callback
   */
  onEdit?: () => void;
  /**
   * More click callback
   */
  onMore?: () => void;
}
