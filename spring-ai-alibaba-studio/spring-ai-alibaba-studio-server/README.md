1. 进入 docker compose 文件目录
```shell
cd docker/middleware
```

2. 运行以下脚本，启动 docker compose

```shell
chmod a+x ./run.sh
./run.sh
```

3. 运行 studio admin

```shell
mvn spring-boot:run
# 如遇编译或依赖问题，请在根目录运行 mvn clean install 安装相关组件后重试
```
