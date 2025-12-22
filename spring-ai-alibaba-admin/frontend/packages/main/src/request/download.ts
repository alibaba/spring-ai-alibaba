import request, { baseURL, session } from './request';
import { UPLOAD_METHOD } from './upload';

export async function getPreviewUrl(filePath: string) {
  const upload_method = window.g_config.config.upload_method;

  if (upload_method === UPLOAD_METHOD.OSS) {
    const response = await request({
      url: `/console/v1/files/get-preview-url`,
      method: 'GET',
      params: {
        path: filePath,
      },
    });

    return response.data.data;
  }
  if (upload_method === UPLOAD_METHOD.FILE)
    return `${baseURL.get()}/console/v1/files/download?path=${encodeURIComponent(
      filePath,
    )}&preview=true&access_token=${session.get()}`;
}
