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

### 添加新菜单

以run nav下的侧边栏为例，如果想添加prompts 菜单，需要做以下几步：

1. 在run下面新建`prompts`文件夹
2. 在run下面的layout.tsx中添加菜单项

  ```tsx
    const [runMenu, setRunMenu] = useState<SubMenuItem[]>([
    {
      key: '/run/clients',
      label: 'Chat Client',
      children: [],
    },
    {
      key: '/run/models',
      label: 'Chat Model',
      children: [],
    },
    {
      key: '/run/prompts', // 路由，和文件夹路径映射上
      label: 'Prompts',
      children: [],
    }
  ]);
  ```

3. 在src/services/下新建prompts.ts，并添加Prompts相关的接口。可以参考chat_models.ts的实现。
  
4. 在run下面的layout.tsx中添加该菜单对应的subMenuItem接口

  ```tsx

  useEffect(() => {
    const fetchData = async () => {
      try {
        const results = await Promise.all([
          chatModelsService.getChatModels(),
          chatClientsService.getChatClients()
          // 引入prompts的全量查询接口
        ]);
        const [chatModelList, chatClientList] = results;

        // 更新runMenu的children
        setRunMenu((prevRunMenu) => {
          const updatedRunMenu = [...prevRunMenu];

          // 组装 ChatClient 目录
          updatedRunMenu[0].children = chatClientList.map((client) => ({
            key: `/run/clients/${client.name}`,
            label: client.name,
          }));

          // 组装 ChatModel 目录
          updatedRunMenu[1].children = chatModelList.map((model) => ({
            key: `/run/models/${model.name}`,
            label: model.name,
          }));

          // todo 组装prompts目录
          return updatedRunMenu;
        });
      } catch (error) {
        console.error('Failed to fetch chat models: ', error);
      }
    };
    fetchData();
  }, []);
  ```

5. 回到run/prompts文件夹下，新建$name.tsx，$name为具体的prompt名，是一种路径变量，可参考ChatModel的实现，比如/run/models/model1。

6. 在run/prompts/$name.tsx中，添加具体的业务逻辑，比如prompt的编辑、执行等。
