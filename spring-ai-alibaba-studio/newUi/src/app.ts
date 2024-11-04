import { defineAppConfig } from 'ice';
import { defineRequestConfig } from '@ice/plugin-request/types';

// App config, see https://v3.ice.work/docs/guide/basic/app
export default defineAppConfig(() => ({
}));


export const requestConfig = defineRequestConfig({
    baseURL: process.env.ICE_BASE_URL,
});