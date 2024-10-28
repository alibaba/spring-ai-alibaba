# dubbo-kube-ui

## Feature

`Vue3` `Vite` `TypeScript` `Ant Design Vue` `AntV|echarts` `lodash` `i18n` `colorful theme`

## Introduction

This project is the front-end of dubbo-kube.

1. home
2. services
3. traffic
4. test tools
5. metrics
6. kubernetes

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

   > must define in `ui-vue3/src/base/enums/ProvideInject.ts`

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

## i18n

1. In html template

   ```html
   <div>
     {{$t(stringVar)}}
   </div>
   ```

2. In ts 

   > if you want to make your var to i18n, make sure your var is a computed

   ```ts
   let label = 'foo'
   let labelWithI18n = computed(() => globalProperties.$t(label))
   ```

   

## Router

1. Define a router

   ```ts
   // router/defaultRoutes
   {
     path: '/home',
     name: 'homePage',
     component: () => import('../views/home/index.vue'),
     meta: {
       icon: 'carbon:web-services-cluster'
     }
   }
   ```

   

2. Config route meta

   ```ts
   // you can config your route with this struct
   export interface RouterMeta extends RouteMeta {
     icon?: string
     hidden?: boolean
     skip?: boolean
   
   
   // icon:  Show as your menu prefix
   // hidden:  Do not show as a menu include its children routes
   // skip: Do not show this route as a menu, but display its child routes.
   
   ```

3. Config a tab route

   > Note:  /tab is a middle layer for left-menu-item: must use LayoutTab as component; meta.tab must be true

   ```ts
   {
       path: '/tab',
       name: 'tabDemo',
       component: LayoutTab,
       redirect: 'index',
       meta: {
           tab_parent: true
       },
       children: [
           {
               path: '/index',
               name: 'applications_index',
               component: () => import('../views/resources/applications/index.vue'),
               meta: {
                   // hidden: true,
               }
           },
           {
               path: '/tab1',
               name: 'tab1',
               component: () => import('../views/common/tab_demo/tab1.vue'),
               meta: {
                   icon: 'simple-icons:podman',
                   tab: true,
               }
           },
   
       ]
   },
   ```

## Build and Deploy

1. Build

   ```shell
   yarn build
   ```

2. Deploy

   Copy the `dist` folder to the path `app/dubbo-ui/dist/`

   ```shell
   # run the following command in the root path of the project
   rm -rf app/dubbo-ui/dist
   cp -r dist/ app/dubbo-ui/
   ```
