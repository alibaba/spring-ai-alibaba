import { request } from '@/request';

export async function getGlobalConfig() {
  const res = await request({
    method: 'GET',
    url: '/console/v1/system/global-config',
  });

  const globalConfig = res.data.data;
  window.g_config.config = globalConfig;
}
