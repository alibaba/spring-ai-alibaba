# Spring AI Alibaba 网站

简体中文 | [English](README.md)

## 🚀 项目结构

在您的Astro + Starlight项目中，您将看到以下文件夹和文件：

```
.
├── public/
├── src/
│   ├── assets/
│   ├── content/
│   │   ├── docs/
│   │   └── config.ts
│   └── env.d.ts
├── astro.config.mjs
├── package.json
└── tsconfig.json
```

Starlight会在`src/content/docs/`目录中查找`.md`或`.mdx`文件。每个文件根据其文件名暴露为一个路由。

图片可以添加到`src/assets/`中，并通过相对链接嵌入到Markdown中。

静态资源，如favicon，可以放在`public/`目录中。

## 本地开发

克隆源代码：

```shell
git clone https://github.com/springaialibaba/spring-ai-alibaba-website.git
cd spring-ai-alibaba-website
```

运行以下命令在本地启动服务器：

```shell
npm install
npm run dev
```

打开浏览器，访问`http://localhost:4321`。

## 🧞 命令

所有命令都在项目根目录的终端中运行：

| 命令                      | 操作                                           |
| :------------------------ | :----------------------------------------------- |
| `npm install`             | 安装依赖                                        |
| `npm run dev`             | 在`localhost:4321`启动本地开发服务器            |
| `npm run build`           | 将您的生产站点构建到`./dist/`                   |
| `npm run preview`         | 在部署前本地预览您的构建                        |
| `npm run astro ...`       | 运行CLI命令，如`astro add`、`astro check`       |
| `npm run astro -- --help` | 获取使用Astro CLI的帮助                         |

> 对于运行`npm install`时遇到问题的用户，请先运行以下命令，然后重试：
> ``` shell
>   brew install vips
>   npm install --unsafe-perm
> ```

## 👀 想了解更多？

查看[Starlight文档](https://starlight.astro.build/)，阅读[Astro文档](https://docs.astro.build)，或加入[Astro Discord服务器](https://astro.build/chat)。 