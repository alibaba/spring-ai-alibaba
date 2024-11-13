# spring-ai-alibaba-studio前端

## 技术栈

React + TypeScript + Ant Design + [icejs](https://v3.ice.work/)

## 本地运行

```bash
npm install

npm start
```

## 开发说明

1. 请求路径在 `.env.development` 目录下，开发者可配置本地后端地址或mock地址
2. 路由为声明式路由，具体可参考文档[icejs 路由](https://v3.ice.work/docs/guide/basic/router)
3. 请求封装在 `src/services/` 文件夹下
4. 全局类型在 `src/types` 文件夹下
5. 【运行】nav下的layout在`src/pages/run/layout.tsx`下，目录列表从接口中获取，可参考现有实现的代码
6. 代码格式化统一使用Prettier
