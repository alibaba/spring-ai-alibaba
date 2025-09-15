import { request } from '@/request';
import {
  IAppStatus,
  IAppVersion,
  IAssistantAppDetail,
  IAssistantConfig,
  IWorkFlowAppDetail,
  IWorkFlowConfig,
} from '@/types/appManage';
import { IApiResponse } from '@/types/common';
import { IAppType } from './appComponent';

/**
 * Parameters for querying application list
 */
export interface IGetAppListParams {
  /** Application name filter */
  name?: string;
  /** Current page number */
  current: number;
  /** Page size */
  size: number;
  /** Application status filter */
  status?: string;
  /** Application type filter */
  type?: IAppType;
}

/**
 * Get application list
 * @param params Query parameters
 * @returns Promise containing application list data
 */
export const getAppList = (params: IGetAppListParams) => {
  // 修复status为空字符串的bug
  if (params.status === null || params.status === '') {
    params.status = undefined;
  }
  return request({
    url: '/console/v1/apps',
    method: 'GET',
    params,
  }).then((res) => res.data.data);
};

/**
 * Parameters for creating application
 */
export interface ICreateAppParams {
  /** Application name */
  name: string;
  /** Application type */
  type: IAppType;
  /** Application configuration */
  config: IWorkFlowConfig | Record<string, any>;
}

/**
 * Create new application
 * @param params Creation parameters
 * @returns Promise containing created application ID
 */
export const createApp = (params: ICreateAppParams) => {
  return request({
    url: '/console/v1/apps',
    method: 'POST',
    data: params,
  }).then((res) => res.data.data as string);
};

/**
 * Delete application
 * @param app_id Application ID to delete
 * @returns Promise
 */
export const deleteApp = (app_id: string) => {
  return request({
    url: `/console/v1/apps/${app_id}`,
    method: 'DELETE',
  });
};

/**
 * Parameters for updating application
 */
export interface IUpdateAppParams {
  /** Application ID */
  app_id: string;
  /** New application name */
  name?: string;
  /** New description */
  description?: string;
  /** New application type */
  type?: IAppType;
  /** New configuration */
  config?: IWorkFlowConfig | IAssistantConfig;
}

/**
 * Update agent application
 * @param params Update parameters
 * @returns Promise
 */
export const updateApp = (params: IUpdateAppParams) => {
  const { app_id, ...rest } = params;
  return request({
    url: `/console/v1/apps/${app_id}`,
    method: 'PUT',
    data: rest,
  });
};

/**
 * Copy application
 * @param app_id Application ID to copy
 * @returns Promise
 */
export const copyApp = (app_id: string) => {
  return request({
    url: `/console/v1/apps/${app_id}/copy`,
    method: 'POST',
  });
};

/**
 * Get application details
 * @param app_id Application ID
 * @returns Promise containing application details
 */
export const getAppDetail = (
  app_id: string,
): Promise<IAssistantAppDetail | IWorkFlowAppDetail> => {
  return request({
    url: `/console/v1/apps/${app_id}`,
    method: 'GET',
  }).then((res) => res.data.data);
};

/**
 * Publish application
 * @param app_id Application ID to publish
 * @returns Promise
 */
export const publishApp = (app_id: string) => {
  return request({
    url: `/console/v1/apps/${app_id}/publish`,
    method: 'POST',
  });
};

/**
 * Get application version list
 * @param param0 Object containing query parameters
 * @returns Promise containing version list data
 */
export const getAppVersionList = ({
  app_id,
  current,
  size,
  status,
}: {
  app_id: string;
  current: number;
  size: number;
  status: IAppStatus;
}): Promise<
  IApiResponse<{
    current: number;
    size: number;
    total: number;
    records: IAppVersion[];
  }>
> => {
  return request({
    url: `/console/v1/apps/${app_id}/versions`,
    method: 'GET',
    params: {
      current,
      size,
      status,
    },
  }).then((res) => res.data);
};

/**
 * Get application version details
 * @param app_id Application ID
 * @param version Version number
 * @returns Promise containing version details
 */
export const getAppVersionDetail = (
  app_id: string,
  version: string,
): Promise<IApiResponse<Omit<IAppVersion, 'config'> & { config: string }>> => {
  return request({
    url: `/console/v1/apps/${app_id}/versions/${version}`,
    method: 'GET',
  }).then((res) => res.data);
};
