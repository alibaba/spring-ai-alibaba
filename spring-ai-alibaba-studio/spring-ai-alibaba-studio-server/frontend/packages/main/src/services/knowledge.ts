import { IChunkItem } from '@/pages/Knowledge/Detail/type';
import { request } from '@/request';
import { IApiResponse } from '@/types/common';
import {
  IChunksListItem,
  ICreateDocumentParams,
  ICreateKnowledgeListParams,
  IGetKnowledgeListParams,
  IKnowledgeDetail,
  IKnowledgeListItem,
  IPagingList,
  IUpdateChunksContentParams,
  IUpdateChunksParams,
  IUpdateStatusChunksParams,
} from '@/types/knowledge';

/**
 * Get knowledge base list
 * @param params Query parameters including pagination and filters
 * @returns Promise containing paginated list of knowledge bases
 */
export const getKnowledgeList = (params: IGetKnowledgeListParams) => {
  return request({
    url: '/console/v1/knowledge-bases',
    method: 'GET',
    params,
  }).then((res) => res.data.data as IPagingList<IKnowledgeListItem>);
};

/**
 * Create new knowledge base
 * @param params Knowledge base creation parameters
 * @returns Promise containing API response with knowledge base ID
 */
export const createKnowledge = (params: ICreateKnowledgeListParams) => {
  return request({
    url: '/console/v1/knowledge-bases',
    method: 'POST',
    data: params,
  }).then((res) => res.data.data as string);
};

/**
 * Update knowledge base
 * @param params Knowledge base update parameters
 * @returns Promise containing API response
 */
export const updateKnowledge = (params: ICreateKnowledgeListParams) => {
  const { kb_id, ...rest } = params;
  return request({
    url: `/console/v1/knowledge-bases/${kb_id}`,
    method: 'PUT',
    data: rest,
  }).then((res) => res.data as IApiResponse<string>);
};

/**
 * Delete knowledge base
 * @param kb_id Knowledge base ID to delete
 * @returns Promise containing API response
 */
export const deleteKnowledge = (kb_id: string) => {
  return request({
    url: `/console/v1/knowledge-bases/${kb_id}`,
    method: 'DELETE',
  }).then((res) => res.data as IApiResponse<string>);
};

/**
 * Get knowledge base details
 * @param kb_id Knowledge base ID
 * @returns Promise containing knowledge base details
 */
export const getKnowledgeDetail = (kb_id: string) => {
  return request({
    url: `/console/v1/knowledge-bases/${kb_id}`,
    method: 'GET',
  }).then((res) => res.data.data as IKnowledgeDetail);
};

/**
 * Test knowledge base retrieval
 * @param params Query parameters including search options
 * @returns Promise containing retrieval test results
 */
export const getKnowledgeRetrieve = (params: {
  query: string;
  search_options: {
    kb_ids: string[];
    similarity_threshold: number;
  };
}) => {
  return request({
    url: `/console/v1/knowledge-bases/retrieve`,
    method: 'POST',
    data: params,
  }).then((res) => {
    if (res) {
      return res.data.data;
    }
  });
};
/**
 * Get document list for a knowledge base
 * @param params Query parameters including pagination and filters
 * @returns Promise containing paginated list of documents
 */
export const getDocumentsList = (params: {
  size: number;
  current: number;
  kb_id: string;
  name?: string;
  index_status?: string;
}) => {
  return request({
    url: `/console/v1/knowledge-bases/${params.kb_id}/documents`,
    method: 'GET',
    params,
  }).then(
    (res) => res.data.data as IApiResponse<IPagingList<IKnowledgeListItem>>,
  );
};

/**
 * Create new document in knowledge base
 * @param params Document creation parameters
 * @returns Promise containing API response with document ID
 */
export const createDocuments = (params: ICreateDocumentParams) => {
  return request({
    url: `/console/v1/knowledge-bases/${params.kb_id}/documents`,
    method: 'POST',
    data: params,
  }).then((res) => res.data.data as IApiResponse<string>);
};

/**
 * Delete document from knowledge base
 * @param kb_id Knowledge base ID
 * @param doc_id Document ID to delete
 * @returns Promise containing API response
 */
export const deleteDocuments = (kb_id: string, doc_id: string) => {
  return request({
    url: `/console/v1/knowledge-bases/${kb_id}/documents/${doc_id}`,
    method: 'DELETE',
  }).then((res) => res.data.data as IApiResponse<string>);
};

/**
 * Batch delete documents from knowledge base
 * @param params Object containing knowledge base ID and document IDs
 * @returns Promise containing API response
 */
export const batchDeleteDocuments = (params: {
  kb_id: string;
  doc_ids: string[];
}) => {
  return request({
    url: `/console/v1/knowledge-bases/${params?.kb_id}/documents/batch-delete`,
    method: 'DELETE',
    data: params,
  }).then((res) => res.data.data as IApiResponse<string>);
};

/**
 * Get chunk list for a document
 * @param param0 Object containing document ID and pagination parameters
 * @returns Promise containing paginated list of chunks
 */
export const getChunksList = ({
  doc_id,
  current,
  size,
}: {
  doc_id: string;
  current: number;
  size: number;
}) => {
  return request({
    url: `/console/v1/documents/${doc_id}/chunks`,
    method: 'GET',
    params: {
      current,
      size,
    },
  }).then((res) => res.data.data as IApiResponse<IPagingList<IChunksListItem>>);
};

/**
 * Delete chunk from document
 * @param param0 Object containing document ID and chunk ID
 * @returns Promise containing API response
 */
export const deleteChunks = ({
  doc_id,
  chunk_id,
}: {
  doc_id: string;
  chunk_id: string;
}) => {
  return request({
    url: `/console/v1/documents/${doc_id}/chunks/${chunk_id}`,
    method: 'DELETE',
  }).then((res) => res.data.data as IApiResponse<string>);
};

/**
 * Update chunk content
 * @param params Object containing document ID, chunk ID and new content
 * @returns Promise containing API response
 */
export const updateChunksContent = (params: IUpdateChunksContentParams) => {
  const { doc_id, chunk_id, ...rest } = params;
  return request({
    url: `/console/v1/documents/${doc_id}/chunks/${chunk_id}`,
    method: 'PUT',
    data: rest,
  }).then((res) => res.data.data as IApiResponse<string>);
};

/**
 * Update chunk settings and re-index document
 * @param params Object containing document ID, knowledge base ID and new settings
 * @returns Promise containing API response
 */
export const updateChunks = (params: IUpdateChunksParams) => {
  const { doc_id, kb_id, ...rest } = params;
  return request({
    url: `/console/v1/knowledge-bases/${kb_id}/documents/${doc_id}/re-index`,
    method: 'PUT',
    data: rest,
  }).then((res) => res.data.data as IApiResponse<string>);
};

/**
 * Preview chunk settings changes
 * @param params Object containing document ID and new settings
 * @returns Promise containing API response
 */
export const previewChunks = (params: IUpdateChunksParams) => {
  const { doc_id, ...rest } = params;
  return request({
    url: `/console/v1/documents/${doc_id}/chunks/preview`,
    method: 'POST',
    data: rest,
  }).then((res) => res.data.data as IChunkItem[]);
};

/**
 * Enable/disable chunks
 * @param params Object containing document ID and chunk status
 * @returns Promise containing API response
 */
export const updateStatusChunks = (params: IUpdateStatusChunksParams) => {
  const { doc_id, ...rest } = params;
  return request({
    url: `/console/v1/documents/${doc_id}/chunks/update-status`,
    method: 'PUT',
    data: rest,
  }).then((res) => res.data.data as IApiResponse<string>);
};

/**
 * Get knowledge bases by their codes
 * @param kb_ids Array of knowledge base IDs
 * @returns Promise containing list of knowledge base items
 */
export const getKnowledgeListByCodes = (kb_ids: string[]) => {
  return request({
    url: '/console/v1/knowledge-bases/query-by-codes',
    method: 'POST',
    data: {
      kb_ids,
    },
  }).then((res) => res.data.data as IKnowledgeListItem[]);
};
