## 贡献指南

### 前端项目概述

- **项目名称**: graph-ui of SpringAiAlibaba
- **技术栈**: `React`, `UmiMax`, `TypeScript`, `ReactFlow`, `i18n`, `CssVar`

### 前端项目结构导航

- 进入项目根目录并进入前端工作目录：

```shell
  cd spring-ai-alibaba-graph/spring-ai-alibaba-graph-studio/graph-ui
```

### 启动项目

```shell
# 初始化
pnpm i

# 运行项目
pnpm run dev
```

### 开发指南

1. **全局 CSS 变量**

   ```js
   // antd
   eg. color: 'var(--ant-color-primary)'
   // xyTheme
   eg. background: var(--xy-theme-selected)
   ```

2. **图标使用**

   ```html
   <Icon icon="svg-spinners:pulse-ring" />
   ```

3. **API 约定**

   - 如果 API 路径以 `mock` 开头，将返回模拟结果。

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

### 国际化 (i18n)

1. **在配置中**

   ```html
   routes: [ { // 该标题将被替换为本地化标题:
   'router.chatbot', path: '/chatbot', component: './Chatbot', }, ]
   ```

2. **在 tsx 中使用**

   ```ts
   import { FormattedMessage } from '@@/exports';

   <FormattedMessage id={'page.graph.search'} />;
   ```

### 路由配置

1. **定义路由**

   ```ts
   // 在 .umirc.ts 中
   routes: [
     {
       path: '/',
       redirect: '/graph',
     },
   ];
   ```

2. **配置路由元数据**

   ```ts
   export interface RouterMeta extends RouteMeta {
     icon?: string
     hidden?: boolean
     skip?: boolean
   }
   ```

3. **配置标签路由**

   - 注意: `/tab` 是左侧菜单项的中间层，必须使用 `LayoutTab` 作为组件；`meta.tab` 必须为 `true`。

### 提交前格式化

```sh
pnpm run format
```

### 构建和部署

1. **构建**

```shell
   pnpm run build
```

2. **部署**

- 将 `dist` 文件夹复制到服务器路径