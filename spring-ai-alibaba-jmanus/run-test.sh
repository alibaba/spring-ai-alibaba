#!/bin/bash

echo "=== McpPlan 组件测试脚本 ==="
echo "正在编译和运行测试..."

# 设置Java路径
JAVA_HOME=${JAVA_HOME:-$(dirname $(dirname $(readlink -f $(which java))))}
echo "使用Java: $JAVA_HOME"

# 编译所有Java文件
echo "编译Java文件..."
javac -cp "src/main/java" src/main/java/com/alibaba/cloud/ai/example/manus/inhouse/mcp/*.java

if [ $? -eq 0 ]; then
    echo "编译成功！"
    echo ""
    echo "运行最终测试..."
    echo "=================================================="
    
    # 运行最终测试
    java -cp "src/main/java" com.alibaba.cloud.ai.example.manus.inhouse.mcp.FinalTest
    
    echo ""
    echo "=================================================="
    echo "测试完成！"
else
    echo "编译失败！"
    exit 1
fi 