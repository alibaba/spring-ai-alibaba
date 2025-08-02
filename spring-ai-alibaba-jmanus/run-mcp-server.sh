#!/bin/bash

# Spring AI Alibaba MCP 服务器启动脚本

echo "=========================================="
echo "Spring AI Alibaba MCP 服务器"
echo "=========================================="

# 检查 Java 版本
echo "检查 Java 版本..."
java -version
if [ $? -ne 0 ]; then
    echo "错误: 未找到 Java 运行时环境"
    exit 1
fi

# 检查 Maven
echo "检查 Maven..."
mvn -version
if [ $? -ne 0 ]; then
    echo "错误: 未找到 Maven"
    exit 1
fi

# 编译项目
echo "编译项目..."
mvn clean compile -q
if [ $? -ne 0 ]; then
    echo "错误: 项目编译失败"
    exit 1
fi

echo "编译完成！"

# 启动 MCP 服务器
echo "启动 MCP 服务器..."
echo "服务器地址: http://localhost:8080/mcp/message"
echo "按 Ctrl+C 停止服务器"
echo ""

java -cp target/classes com.alibaba.cloud.ai.example.manus.inhouse.WebFluxStreamableServerApplication 