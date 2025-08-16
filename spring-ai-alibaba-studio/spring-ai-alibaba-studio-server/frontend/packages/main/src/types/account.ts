import $i18n from '@/i18n';

// User types mapping
export const USER_TYPE = {
  admin: $i18n.get({
    id: 'main.types.account.administrator',
    dm: '管理员',
  }),
  user: $i18n.get({
    id: 'main.types.account.normalUser',
    dm: '普通用户',
  }),
};

// Parameters for creating an account
export interface ICreateAccountParams {
  username: string;
  password: string;
}

// Parameters for updating account information
export interface IUpdateAccountParams {
  nickname: string;
  email?: string;
  password?: string;
}

// Parameters for changing password
export interface IChangePasswordParams {
  password: string;
  new_password: string;
}

// Account information structure
export interface IAccount {
  account_id: string;
  username: string;
  email: string;
  type: keyof typeof USER_TYPE;
  logo?: string;
}

// Parameters for getting account list
export interface IGetAccountListParams {
  current?: number;
  size?: number;
  name?: string;
}

// Paginated list response structure
export interface IPagingList<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
}
