1. 进入 docker compose 文件目录
```shell
cd docker/middleware
```

2. 运行以下脚本，启动 docker compose

```shell
chmod a+x ./run.sh
sudo ./run.sh
```
等待 60秒， `rmq-init-topic` 脚本会自动创建 RocketMQ 相关的 Topic。

3. 进入 spring-ai-alibaba-studio-server-admin 目录，启动后端应用

```shell
cd spring-ai-alibaba-studio-server-admin
mvn spring-boot:run
# 如遇编译或依赖问题，请在根目录运行 mvn clean install 安装相关组件后重试
```

4. 启动前端组件

进入 frontend 目录，参考 README 运行并打开浏览器访问前端页面。
