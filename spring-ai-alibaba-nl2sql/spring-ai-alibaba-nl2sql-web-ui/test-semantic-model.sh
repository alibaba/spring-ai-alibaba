#!/bin/bash
# 语义模型配置功能测试脚本

echo "=== Spring AI Alibaba NL2SQL 语义模型配置功能测试 ==="
echo ""

# 检查后台服务是否运行
echo "1. 检查后台服务状态..."
response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8065/api/semantic-model 2>/dev/null)
if [ "$response" = "200" ]; then
    echo "✅ 后台服务运行正常"
else
    echo "❌ 后台服务未运行，请先启动后台服务"
    echo "   启动命令: cd spring-ai-alibaba-nl2sql-management && mvn spring-boot:run"
    exit 1
fi

echo ""

# 测试API接口
echo "2. 测试API接口..."

# 测试获取列表
echo "   - 测试获取语义模型列表..."
list_response=$(curl -s http://localhost:8065/api/semantic-model)
if [ $? -eq 0 ]; then
    echo "   ✅ 获取列表接口正常"
else
    echo "   ❌ 获取列表接口异常"
fi

# 测试搜索功能
echo "   - 测试搜索功能..."
search_response=$(curl -s "http://localhost:8065/api/semantic-model?keyword=sales")
if [ $? -eq 0 ]; then
    echo "   ✅ 搜索接口正常"
else
    echo "   ❌ 搜索接口异常"
fi

echo ""

# 检查前端服务
echo "3. 检查前端服务状态..."
frontend_response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:3000 2>/dev/null)
if [ "$frontend_response" = "200" ]; then
    echo "✅ 前端服务运行正常"
    echo "   可访问: http://localhost:3000"
else
    echo "❌ 前端服务未运行，请先启动前端服务"
    echo "   启动命令: cd spring-ai-alibaba-nl2sql-web-ui && npm run dev"
fi

echo ""
echo "=== 测试完成 ==="
echo ""
echo "如果所有服务都正常运行，可以通过以下链接访问语义模型配置页面："
echo "http://localhost:3000/#/semantic-model"
echo ""
echo "功能验证项目："
echo "□ 页面正常加载"
echo "□ 数据列表显示"
echo "□ 搜索功能正常"
echo "□ 新增配置功能"
echo "□ 编辑配置功能"
echo "□ 删除配置功能"
echo "□ 批量操作功能"
echo "□ 数据集筛选功能"
