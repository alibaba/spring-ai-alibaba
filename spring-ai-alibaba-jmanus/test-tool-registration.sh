#!/bin/bash

# 测试注解驱动工具注册状态
# 作者: AI Assistant
# 日期: 2025-01-02

set -e

echo "=========================================="
echo "测试注解驱动工具注册状态"
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
else
    echo "❌ 无法获取工具列表"
    echo "响应: $TOOLS_RESPONSE"
fi

# 测试具体工具调用
echo ""
echo "测试工具调用..."

# 测试 ping 工具
echo "测试 ping 工具..."
PING_RESPONSE=$(curl -s -X POST http://localhost:20881/mcp/message \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 2,
    "method": "tools/call",
    "params": {
      "name": "ping",
      "arguments": {
        "message": "test"
      }
    }
  }' 2>/dev/null || echo "{}")

if echo "$PING_RESPONSE" | grep -q "result"; then
    echo "✅ ping 工具调用成功"
    echo "响应: $(echo "$PING_RESPONSE" | jq -r '.result.content[0].text' 2>/dev/null || echo "无法解析响应")"
else
    echo "❌ ping 工具调用失败"
    echo "响应: $PING_RESPONSE"
fi

# 测试 calculator 工具
echo ""
echo "测试 calculator 工具..."
CALC_RESPONSE=$(curl -s -X POST http://localhost:20881/mcp/message \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 3,
    "method": "tools/call",
    "params": {
      "name": "calculator",
      "arguments": {
        "expression": "2 + 3"
      }
    }
  }' 2>/dev/null || echo "{}")

if echo "$CALC_RESPONSE" | grep -q "result"; then
    echo "✅ calculator 工具调用成功"
    echo "响应: $(echo "$CALC_RESPONSE" | jq -r '.result.content[0].text' 2>/dev/null || echo "无法解析响应")"
else
    echo "❌ calculator 工具调用失败"
    echo "响应: $CALC_RESPONSE"
fi

# 测试 echo 工具
echo ""
echo "测试 echo 工具..."
ECHO_RESPONSE=$(curl -s -X POST http://localhost:20881/mcp/message \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "id": 4,
    "method": "tools/call",
    "params": {
      "name": "echo",
      "arguments": {
        "message": "Hello World"
      }
    }
  }' 2>/dev/null || echo "{}")

if echo "$ECHO_RESPONSE" | grep -q "result"; then
    echo "✅ echo 工具调用成功"
    echo "响应: $(echo "$ECHO_RESPONSE" | jq -r '.result.content[0].text' 2>/dev/null || echo "无法解析响应")"
else
    echo "❌ echo 工具调用失败"
    echo "响应: $ECHO_RESPONSE"
fi

echo ""
echo "=========================================="
echo "测试完成！"
echo "=========================================="
echo ""
echo "如果所有测试都通过，说明注解驱动工具注册系统工作正常！"
echo ""
echo "下一步："
echo "1. 查看应用日志: tail -f logs/info.log"
echo "2. 添加新工具: 只需添加 @McpTool 注解"
echo "3. 重启应用: 新工具会自动注册" 