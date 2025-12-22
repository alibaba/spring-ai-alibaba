import { request } from '@/request';

export interface DifyConvertParams {
  dependencies: string;
  appMode: string;
  dslDialectType: string;
  type: string;
  language: string;
  bootVersion: string;
  baseDir: string;
  groupId: string;
  artifactId: string;
  name: string;
  description: string;
  packageName: string;
  packaging: string;
  javaVersion: string;
  difyDsl: string;
}

export function convertDifyToSpringAI(data: DifyConvertParams) {
  const formData = new URLSearchParams();
  Object.entries(data).forEach(([key, value]) => {
    formData.append(key, value);
  });

  return request({
    method: 'POST',
    url: '/starter.zip',
    data: formData.toString(),
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    responseType: 'blob', // 期望接收 blob 数据（zip 文件）
  });
}
