import type {
  AxiosInstance,
  AxiosInterceptorManager,
  AxiosRequestHeaders,
  AxiosResponse,
  InternalAxiosRequestConfig,
} from 'axios'
import axios from 'axios'

const service: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 30 * 1000,
})
const request: AxiosInterceptorManager<InternalAxiosRequestConfig> = service.interceptors.request
const response: AxiosInterceptorManager<AxiosResponse> = service.interceptors.response

request.use(
  config => {
    config.data = JSON.stringify(config.data)
    config.headers = <AxiosRequestHeaders>{
      'Content-Type': 'application/json',
    }
    return config
  },
  error => {
    Promise.reject(error)
  }
)

response.use(
  response => {
    if (
      response.status === 200 &&
      (response.data.code === 200 || response.data.status === 'success')
    ) {
      return Promise.resolve(response.data)
    }
    console.error(response.data)
    return Promise.reject(response.data)
  },
  error => {
    if (error) {
      console.error(error)
    }
    return Promise.reject(error.response.data)
  }
)
export default service
