import { request } from '@/request';
import { session } from '@/request/session';
import type {
  IAccount,
  IChangePasswordParams,
  ICreateAccountParams,
  IGetAccountListParams,
  IPagingList,
  IUpdateAccountParams,
} from '@/types/account';
import { IApiResponse } from '@/types/common';

/**
 * Create an account
 * @param params - Contains username and password
 * @returns Promise<IApiResponse<any>> - Returns the data part of API response
 */
export async function createAccount(
  params: ICreateAccountParams,
): Promise<IApiResponse<any>> {
  const response = await request({
    url: '/console/v1/accounts',
    method: 'POST',
    data: params,
  });

  return response.data as IApiResponse<any>;
}

/**
 * Update account information
 * @param accountId - Account ID to update
 * @param params - Contains nickname and optional email
 * @returns Promise<IApiResponse<string>> - Data appears to be empty string based on docs
 */
export async function updateAccount(
  accountId: string,
  params: IUpdateAccountParams,
): Promise<IApiResponse<string>> {
  const response = await request({
    url: `/console/v1/accounts/${accountId}`,
    method: 'PUT',
    data: params,
  });
  return response.data as IApiResponse<string>;
}

/**
 * Delete an account
 * @param accountId - Account ID to delete
 * @returns Promise<IApiResponse<string>> - Data appears to be empty string based on docs
 */
export async function deleteAccount(
  accountId: string,
): Promise<IApiResponse<string>> {
  const response = await request({
    url: `/console/v1/accounts/${accountId}`,
    method: 'DELETE',
  });
  return response.data as IApiResponse<string>;
}

/**
 * Get account information
 * @param accountId - Account ID to retrieve
 * @returns Promise<IApiResponse<IAccount>> - Returns specified account information
 */
export async function getAccountInfo(): Promise<IApiResponse<IAccount>> {
  const url = new URL(window.location.href);
  const access_token = url.searchParams.get('access_token');
  const refresh_token = url.searchParams.get('refresh_token');
  const expires_in = url.searchParams.get('expires_in');

  if (access_token && refresh_token && expires_in) {
    session.set({
      access_token,
      refresh_token,
      expires_in: parseInt(expires_in),
    });
  }

  const response = await request({
    url: `/console/v1/accounts/profile`,
    method: 'GET',
    autoMsg: false,
  });
  return response.data as IApiResponse<IAccount>;
}

/**
 * Get account list
 * @param params - Contains pagination and filter parameters
 * @returns Promise<IApiResponse<IPagingList<IAccount>>>
 */
export async function getAccountList(
  params?: IGetAccountListParams,
): Promise<IApiResponse<IPagingList<IAccount>>> {
  const response = await request({
    url: '/console/v1/accounts',
    method: 'GET',
    params: params,
  });
  return response.data as IApiResponse<IPagingList<IAccount>>;
}

/**
 * Change password
 * @param params - Contains password (current) and new_password
 * @returns Promise<IApiResponse<string>> - Typically returns empty string in data part
 */
export async function changePassword(
  params: IChangePasswordParams,
): Promise<IApiResponse<string>> {
  const response = await request({
    url: '/console/v1/accounts/change-password',
    method: 'PUT',
    data: params,
  });
  return response.data as IApiResponse<string>;
}
