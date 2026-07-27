import { request } from '@/request';
import { IApiResponse } from '@/types/common';
import type {
  IListSkillsParams,
  IPagingList,
  ISkill,
  ISkillFileContent,
  ISkillFileNode,
} from '@/types/skill';

export async function listSkills(
  params: IListSkillsParams,
): Promise<IApiResponse<IPagingList<ISkill>>> {
  const response = await request({
    url: '/console/v1/skills',
    method: 'GET',
    params,
  });
  return response.data as IApiResponse<IPagingList<ISkill>>;
}

export async function getSkill(
  skillId: string,
): Promise<IApiResponse<ISkill>> {
  const response = await request({
    url: `/console/v1/skills/${skillId}`,
    method: 'GET',
  });
  return response.data as IApiResponse<ISkill>;
}

export async function listSkillFiles(
  skillId: string,
): Promise<IApiResponse<ISkillFileNode[]>> {
  const response = await request({
    url: `/console/v1/skills/${skillId}/files`,
    method: 'GET',
  });
  return response.data as IApiResponse<ISkillFileNode[]>;
}

export async function readSkillFile(
  skillId: string,
  path: string,
): Promise<IApiResponse<ISkillFileContent>> {
  const response = await request({
    url: `/console/v1/skills/${skillId}/file`,
    method: 'GET',
    params: { path },
  });
  return response.data as IApiResponse<ISkillFileContent>;
}

export async function deleteSkill(
  skillId: string,
): Promise<IApiResponse<null>> {
  const response = await request({
    url: `/console/v1/skills/${skillId}`,
    method: 'DELETE',
  });
  return response.data as IApiResponse<null>;
}

export async function listSkillsByIds(
  skillIds: string[],
): Promise<IApiResponse<ISkill[]>> {
  const response = await request({
    url: '/console/v1/skills/query-by-ids',
    method: 'POST',
    data: skillIds,
  });
  return response.data as IApiResponse<ISkill[]>;
}

export async function createSkill(params: {
  file: File;
  name?: string;
  description?: string;
}): Promise<IApiResponse<string>> {
  const formData = new FormData();
  formData.append('file', params.file);
  if (params.name) formData.append('name', params.name);
  if (params.description) formData.append('description', params.description);

  const response = await request({
    url: '/console/v1/skills',
    method: 'POST',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data as IApiResponse<string>;
}

export async function updateSkill(params: {
  skillId: string;
  file?: File;
  name?: string;
  description?: string;
}): Promise<IApiResponse<null>> {
  const formData = new FormData();
  if (params.file) formData.append('file', params.file);
  if (params.name) formData.append('name', params.name);
  if (params.description !== undefined)
    formData.append('description', params.description || '');

  const response = await request({
    url: `/console/v1/skills/${params.skillId}`,
    method: 'PUT',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data as IApiResponse<null>;
}
