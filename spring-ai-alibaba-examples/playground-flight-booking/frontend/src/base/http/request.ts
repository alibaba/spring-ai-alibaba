import type {
  AxiosInstance,
  AxiosInterceptorManager,
  AxiosRequestHeaders,
  AxiosResponse,
  InternalAxiosRequestConfig,
} from "axios";
import axios from "axios";
import { message } from "ant-design-vue";
import NProgress from "nprogress";

const service: AxiosInstance = axios.create({
  //  change this to decide where to go
  // baseURL: '/mock',
  baseURL: "/api",
  timeout: 30 * 1000,
});
const request: AxiosInterceptorManager<InternalAxiosRequestConfig> =
  service.interceptors.request;
const response: AxiosInterceptorManager<AxiosResponse> =
  service.interceptors.response;

request.use(
  (config) => {
    config.data = JSON.stringify(config.data); //数据转化,也可以使用qs转换
    config.headers = <AxiosRequestHeaders>{
      "Content-Type": "application/json", //配置请求头
    };
    NProgress.start();
    console.log(config);
    return config;
  },
  (error) => {
    Promise.reject(error);
  },
);

response.use(
  (response) => {
    NProgress.done();

    if (response.status === 200) {
      return Promise.resolve(response.data);
    }
    return Promise.reject(response);
  },
  (error) => {
    NProgress.done();
    message.error(error.message);
    return Promise.resolve(error.response);
  },
);
export default service;
