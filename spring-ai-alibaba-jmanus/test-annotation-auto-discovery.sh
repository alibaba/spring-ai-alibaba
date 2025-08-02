#!/bin/bash

# 测试注解自动发现功能
# 作者: AI Assistant
# 日期: 2025-01-02

set -e

echo "=========================================="
echo "测试注解自动发现功能"
echo "=========================================="

# 检查应用是否正在运行
echo "检查应用状态..."
if curl -s http://localhost:18080/actuator/health > /dev/null 2>&1; then
    echo "✅ 应用正在运行 (端口 18080)"
else
    echo "❌ 应用未运行或无法访问"
    echo "请先启动应用: mvn spring-boot:run"
    exit 1
fi

# 检查 MCP 服务器状态
echo ""
echo "检查 MCP 服务器状态..."
if curl -s http://localhost:20881/mcp/message > /dev/null 2>&1; then
    echo "✅ MCP 服务器正在运行 (端口 20881)"
else
    echo "❌ MCP 服务器未运行或无法访问"
    echo "请检查 MCP 服务器启动日志"
    exit 1
fi

# 检查工具注册
echo ""
echo "检查工具注册状态..."

# 发送 MCP 工具列表请求
TOOLS_RESPONSE=$(curl -s -X POST http://localhost:20881/mcp/message \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 1,
    "method": "tools/list",
    "params": {}
  }' 2>/dev/null || echo "{}")

if echo "$TOOLS_RESPONSE" | grep -q "result"; then
    echo "✅ 成功获取工具列表"
    echo ""
    echo "注册的工具:"
    echo "$TOOLS_RESPONSE" | jq -r '.result.tools[]? | "  - \(.name): \(.description)"' 2>/dev/null || echo "  (无法解析工具列表)"
    
    # 检查 CalculatorTool 是否正确获取了名称和描述
    echo ""
    echo "检查 CalculatorTool 自动发现:"
    CALC_NAME=$(echo "$TOOLS_RESPONSE" | jq -r '.result.tools[]? | select(.name == "calculator") | .name' 2>/dev/null)
    CALC_DESC=$(echo "$TOOLS_RESPONSE" | jq -r '.result.tools[]? | select(.name == "calculator") | .description' 2>/dev/null)
    
    if [ "$CALC_NAME" = "calculator" ]; then
        echo "✅ CalculatorTool 名称自动发现成功: $CALC_NAME"
    else
        echo "❌ CalculatorTool 名称自动发现失败"
    fi
    
    if [ "$CALC_DESC" = "数学计算工具，支持基本数学运算" ]; then
        echo "✅ CalculatorTool 描述自动发现成功: $CALC_DESC"
    else
        echo "❌ CalculatorTool 描述自动发现失败: $CALC_DESC"
    fi
else
    echo "❌ 无法获取工具列表"
    echo "响应: $TOOLS_RESPONSE"
fi

# 测试 CalculatorTool 调用
echo ""
echo "测试 CalculatorTool 调用..."
CALC_RESPONSE=$(curl -s -X POST http://localhost:20881/mcp/message \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/call",
    "params": {
      "name": "calculator",
      "arguments": {
        "expression": "2 + 3"
      }
    }
  }' 2>/dev/null || echo "{}")

if echo "$CALC_RESPONSE" | grep -q "result"; then
    echo "✅ CalculatorTool 调用成功"
    echo "响应: $(echo "$CALC_RESPONSE" | jq -r '.result.content[0].text' 2>/dev/null || echo "无法解析响应")"
else
    echo "❌ CalculatorTool 调用失败"
    echo "响应: $CALC_RESPONSE"
fi

echo ""
echo "=========================================="
echo "测试完成！"
echo "=========================================="
echo ""
echo "注解自动发现功能说明："
echo "1. 当 @McpTool 注解的 name 为空时，自动调用工具对象的 getName() 方法"
echo "2. 当 @McpTool 注解的 description 为空时，自动调用工具对象的 getDescription() 方法"
echo "3. 如果工具对象实现了 ToolCallBiFunctionDef 接口，直接调用接口方法"
echo "4. 如果工具对象有 getName() 和 getDescription() 方法，通过反射调用"
echo "5. 如果都无法获取，使用默认值"
echo ""
echo "示例："
echo "@McpTool(name = \"\", description = \"\")  // 注解为空"
echo "public class MyTool extends AbstractBaseTool<Map<String, Object>> {"
echo "    @Override"
echo "    public String getName() { return \"my_tool\"; }  // 自动获取"
echo "    @Override"
echo "    public String getDescription() { return \"我的工具\"; }  // 自动获取"
echo "}" 