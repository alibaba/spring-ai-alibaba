#!/bin/bash

# 测试注解驱动 MCP 工具注册
# 作者: AI Assistant
# 日期: 2025-01-02

set -e

echo "=========================================="
echo "测试注解驱动 MCP 工具注册"
echo "=========================================="

# 检查 Java 环境
if ! command -v java &> /dev/null; then
    echo "错误: 未找到 Java 环境"
    exit 1
fi

echo "Java 版本:"
java -version

# 检查 Maven 环境
if ! command -v mvn &> /dev/null; then
    echo "错误: 未找到 Maven 环境"
    exit 1
fi

echo "Maven 版本:"
mvn -version

# 编译项目
echo ""
echo "编译项目..."
mvn clean compile -q

if [ $? -eq 0 ]; then
    echo "✅ 编译成功"
else
    echo "❌ 编译失败"
    exit 1
fi

# 检查注解类是否存在
echo ""
echo "检查注解类..."
ANNOTATION_CLASSES=(
    "com.alibaba.cloud.ai.example.manus.inhouse.annotation.McpTool"
    "com.alibaba.cloud.ai.example.manus.inhouse.annotation.McpToolSchema"
)

for class in "${ANNOTATION_CLASSES[@]}"; do
    if mvn exec:java -Dexec.mainClass="java.lang.Class" -Dexec.args="$class" -q 2>/dev/null; then
        echo "✅ $class 存在"
    else
        echo "❌ $class 不存在"
        exit 1
    fi
done

# 检查注册器类是否存在
echo ""
echo "检查注册器类..."
REGISTRY_CLASS="com.alibaba.cloud.ai.example.manus.inhouse.registry.McpToolRegistry"

if mvn exec:java -Dexec.mainClass="java.lang.Class" -Dexec.args="$REGISTRY_CLASS" -q 2>/dev/null; then
    echo "✅ $REGISTRY_CLASS 存在"
else
    echo "❌ $REGISTRY_CLASS 不存在"
    exit 1
fi

# 检查示例工具类是否存在
echo ""
echo "检查示例工具类..."
TOOL_CLASSES=(
    "com.alibaba.cloud.ai.example.manus.tool.database.DatabaseUseTool"
    "com.alibaba.cloud.ai.example.manus.inhouse.tool.calculator.CalculatorTool"
    "com.alibaba.cloud.ai.example.manus.inhouse.tool.echo.EchoTool"
    "com.alibaba.cloud.ai.example.manus.inhouse.tool.ping.PingTool"
)

for class in "${TOOL_CLASSES[@]}"; do
    if mvn exec:java -Dexec.mainClass="java.lang.Class" -Dexec.args="$class" -q 2>/dev/null; then
        echo "✅ $class 存在"
    else
        echo "❌ $class 不存在"
        exit 1
    fi
done

# 检查配置文件是否存在
echo ""
echo "检查配置文件..."
CONFIG_FILES=(
    "src/main/resources/application-mcp-tools.yml"
    "src/main/java/com/alibaba.cloud.ai.example.manus/config/McpToolProperties.java"
)

for file in "${CONFIG_FILES[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ $file 存在"
    else
        echo "❌ $file 不存在"
        exit 1
    fi
done

# 检查文档是否存在
echo ""
echo "检查文档..."
DOC_FILES=(
    "src/main/java/com/alibaba/cloud/ai/example/manus/tool/README-注解驱动工具注册.md"
)

for file in "${DOC_FILES[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ $file 存在"
    else
        echo "❌ $file 不存在"
        exit 1
    fi
done

echo ""
echo "=========================================="
echo "✅ 所有检查通过！注解驱动 MCP 工具注册系统已就绪"
echo "=========================================="
echo ""
echo "下一步："
echo "1. 启动应用程序: mvn spring-boot:run"
echo "2. 检查日志中的工具注册信息"
echo "3. 使用 MCP 客户端测试工具调用"
echo ""
echo "示例工具："
echo "- database: 数据库操作工具"
echo "- calculator: 数学计算工具"
echo "- echo: 回显工具"
echo "- ping: 连接测试工具"
echo ""
echo "更多信息请查看: src/main/java/com/alibaba/cloud/ai/example/manus/tool/README-注解驱动工具注册.md" 