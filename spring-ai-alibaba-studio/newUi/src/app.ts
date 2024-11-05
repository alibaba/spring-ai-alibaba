import { defineAppConfig } from "ice";
import { defineRequestConfig } from "@ice/plugin-request/types";
import { message } from 'antd';

// App config, see https://v3.ice.work/docs/guide/basic/app
export default defineAppConfig(() => ({}));

export const requestConfig = defineRequestConfig({
  baseURL: process.env.ICE_BASE_URL,

  // 拦截器
  interceptors: {
    request: {
      onConfig: (config) => {
        // 发送请求前：可以对 RequestConfig 做一些统一处理
        // config.headers = {a: 1};
        return config;
      },
      onError: (error) => {
        return Promise.reject(error);
      },
    },
    response: {
      onConfig: (response) => {
        if (response.data.code != 200) {
          message.error(response.data.msg);
        }
        return response.data;
      },
      onError: (error) => {
        console.log(error);
        // 请求出错：服务端返回错误状态码
        console.log(error.response);
        return Promise.reject(error);
      },
    },
  },
});
