# Spark Flow
## 项目介绍
Spark Flow是供给主包`packages/main`的画布编辑基础业务组件，主要技术栈包含React、React Flow、Antd、zustand等。

### 主要功能
+ 节点检查清单列表；
+ 节点管理器；
+ 画布执行状态管理；
+ 支持回退、撤销；
+ 支持全生命周期快速定制业务节点；
+ 支持复杂节点交互；

### 项目主要结构
```plain
spark-flow/
├── docs/
├── public/                # 静态资源文件
├── src/               
│   ├── components/        # 基础组件
│   ├── constant/          # 可视化工作流编辑器
│   ├── demos/             # 国际化支持
│   ├── flow/              # 画布组件主入口
│   ├── hooks/             # 画布类操作工具库
│   ├── i18n/              # 国际化
│   ├── store/             # 全局状态管理
│   ├── types/             # 类型定义
|		├── utils/             # 函数工具库 
|		├── index.less/        # less变量定义
|		└── index.ts/          # 项目主入口文件
└── package.json           # 项目配置
```

#### 节点结构
```plain
[业务节点名]
├── node                 # 画布节点渲染
├── panel                # 节点配置面板
└── schema               # 节点Schema协议配置
```

## 如何开发
### 快速启动
```shell
npm run re-install && cd packages/spark-flow && npm start
```

+ **安装依赖** 在根目录执行`npm run re-install`
+ **运行** cd`packages/spark-flow`且执行`npm start`

### 开发
> [!NOTE]  
> **注意：** 前提是你完成了快速启动的 **安装依赖** 操作

+ 开发完之后根目录下执行`npm run fresh:flow`能够快速清除主包`packages/main`的依赖；
+ 进入主包`packages/main`执行`npm start`
+ 进行测试 & 验证；