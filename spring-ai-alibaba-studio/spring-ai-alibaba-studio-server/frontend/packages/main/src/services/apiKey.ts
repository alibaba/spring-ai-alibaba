import { request } from '@/request';
import type { IApiKey, ICreateApiKeyParams, IPagingList } from '@/types/apiKey';
import { IApiResponse } from '@/types/common';

/**
 * Create API Key
 * @param params Creation parameters
 * @returns Promise<IApiResponse<string>> Creation result
 */
export async function createApiKey(
  params: ICreateApiKeyParams,
): Promise<IApiResponse<string>> {
  const response = await request({
    url: '/console/v1/api-keys',
    method: 'POST',
    data: params,
  });
  return response.data;
}

/**
 * Delete API Key
 * @param id API Key ID
 * @returns Promise<IApiResponse<null>> Deletion result
 */
export async function deleteApiKey(
  id: string | number,
): Promise<IApiResponse<null>> {
  const response = await request({
    url: `/console/v1/api-keys/${id}`,
    method: 'DELETE',
  });
  return response.data;
}

/**
 * Get single API Key
 * @param id API Key ID
 * @returns Promise<IApiResponse<IApiKey>> API Key details
 */
export async function getApiKey(
  id: string | number,
): Promise<IApiResponse<IApiKey>> {
  const response = await request({
    url: `/console/v1/api-keys/${id}`,
    method: 'GET',
  });
  return response.data;
}

/**
 * Get API Key list
 * @param params Query parameters
 * @returns Promise<IApiResponse<IPagingList<IApiKey>>> API Key list
 */
export async function listApiKeys(params?: {
  size?: number;
  current?: number;
}): Promise<IApiResponse<IPagingList<IApiKey>>> {
  const response = await request({
    url: '/console/v1/api-keys',
    method: 'GET',
    params,
  });
  return response.data;
}
