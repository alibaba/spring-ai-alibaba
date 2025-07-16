# graph-ui of SpringAiAlibaba

## Feature

`React` `UmiMax` `TypeScript` `ReactFlow` `i18n` `CssVar`

## Introduction

This project is the graph-ui of spring-ai-alibaba.

1. home
2. chatbot
3. agent
4. graph

## Startup

```shell
# init
pnpm i

# run it
pnpm run dev

```

## Develop

1. global css var

```js
// antd
eg. color: 'var(--ant-color-primary)'
// xyTheme
eg. background: var(--xy-theme-selected)
```

2. icon: https://icones.js.org/

   ```html
   <Icon icon="svg-spinners:pulse-ring" />
   ```

3. api

   > Agreement: if your api's path starts with mock, you will get a mock result

   1. Declear api

      ```ts
      import request from '@/base/http/request';

      export const getClusterInfo = (params: any): Promise<any> => {
        return request({
          url: '/metrics/cluster',
          method: 'get',
          params,
        });
      };
      ```

## i18n

1. In config

   ```html
   routes: [ { // this title will be replace with the locale title:
   'router.chatbot', path: '/chatbot', component: './Chatbot', }, ]
   ```

2. In tsx

   > Use FormattedMessage

   ```ts
   import { FormattedMessage } from '@@/exports';

   <FormattedMessage id={'page.graph.search'} />;
   ```

## Router

1. Define a router

   ```ts
   // in .umirc.ts
   routes: [
     {
       path: '/',
       redirect: '/graph',
     },
   ];
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

   > Note: /tab is a middle layer for left-menu-item: must use LayoutTab as component; meta.tab must be true

## Commit-pre

```sh
pnpm run format
```

## Build and Deploy

1. Build

   ```shell
   pnpm run build
   ```

2. Deploy

   Copy the `dist` folder to the server path
