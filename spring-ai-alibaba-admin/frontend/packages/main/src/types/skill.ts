export const SKILL_MAX_LIMIT = 10;

export interface ISkill {
  skill_id: string;
  name: string;
  description?: string;
  skill_name: string;
  storage_path?: string;
  source?: string;
  gmt_create?: string;
  gmt_modified?: string;
}

export interface ISkillFileNode {
  name: string;
  path: string;
  type: 'file' | 'directory';
  size?: number;
  children?: ISkillFileNode[];
}

export interface ISkillFileContent {
  path: string;
  content?: string;
  is_binary?: boolean;
  content_type?: string;
  size?: number;
}

export interface IListSkillsParams {
  current?: number;
  size?: number;
  name?: string;
}

export interface IPagingList<T> {
  current: number;
  size: number;
  total: number;
  records: T[];
}
