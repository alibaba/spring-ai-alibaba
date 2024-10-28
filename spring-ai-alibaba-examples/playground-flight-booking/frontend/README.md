## Startup

```shell
# add vite vue project
# https://vuejs.org/guide/scaling-up/tooling.html
# Vue CLI is the official webpack-based toolchain for Vue. 
# It is now in maintenance mode and we recommend starting new projects with Vite unless you rely on specific webpack-only features. 
# Vite will provide superior developer experience in most cases.

# note your env version
# node v18.0.0
# yarn 1.22.21

npm create vue@latest

yarn

yarn format

# run it
yarn dev


# main com
yarn add ant-design-vue@4.x


```

## Develop

1. todo
> if a function is not complete but show the entry in your page, 
> please use notification.todo to mark it
```shell
/**
 * this function is showing some tips about our Q&A
 * TODO
 */
function globalQuestion() {
  devTool.todo("show Q&A tips")
}
```
2. global css var 
   ```js
   // if you want use the global css var, such as primary color
   // create a reactive reference to a var by 'ref'
   export const PRIMARY_COLOR = ref('#17b392')
   
   // In js
   import {PRIMARY_COLOR} from '@/base/constants'
   
   // In CSS, use __null for explicit reference to prevent being cleared by code formatting.
   let __null = PRIMARY_COLOR
   
   ```

3. provide and inject

  

4. icon: https://icones.js.org/

   ```html
   <Icon icon="svg-spinners:pulse-ring" />
     
   ```

5. api

   > Agreement: if your api's path starts with mock, you will get a mock result

   1. Declear api

      ```ts
      
      import request from '@/base/http/request'
      
      export const getClusterInfo = (params: any):Promise<any> => {
          return request({
              url: '/metrics/cluster',
              method: 'get',
              params
          })
      }
      
      ```

      

   2. Declear mock api

      ```ts
      // define a mock api
      import Mock from 'mockjs'
      Mock.mock('/mock/metrics/cluster', 'get', {
          code: 200,
          message: '成功',
          data: {
              all: Mock.mock('@integer(100, 500)'),
              application: Mock.mock('@integer(80, 200)'),
              consumers: Mock.mock('@integer(80, 200)'),
              providers: Mock.mock('@integer(80, 200)'),
              services: Mock.mock('@integer(80, 200)'),
              versions: ["dubbo-golang-3.0.4"],
              protocols: ["tri"],
              rules: [],
              configCenter: "127.0.0.1:2181",
              registry: "127.0.0.1:2181",
              metadataCenter: "127.0.0.1:2181",
              grafana: "127.0.0.1:3000",
              prometheus: "127.0.0.1:9090"
          }
      })
      
      // import in main.ts
      import './api/mock/mockCluster.js'
      
      ```

   3. invoke api

      ```ts
      
      onMounted(async () => {
        let {data} = await getClusterInfo({})
      })
      ```

   4. decide where the request is to go : request.ts

      ```ts
      // request.ts
      
      const service: AxiosInstance = axios.create({
          //  change this to decide where to go
          baseURL: '/mock',
          timeout: 30 * 1000
      })
      ```


## Build and Deploy

1. Build

   ```shell
   yarn build
   ```

2. Deploy

   Copy the `dist/assets` folder to the path `src/main/resources/static`
   Copy the `dist/index.html` file to the path `src/main/resources/templates`

