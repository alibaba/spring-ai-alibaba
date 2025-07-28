# 语义模型配置功能测试脚本 (PowerShell)

Write-Host "=== Spring AI Alibaba NL2SQL 语义模型配置功能测试 ===" -ForegroundColor Green
Write-Host ""

# 检查后台服务是否运行
Write-Host "1. 检查后台服务状态..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8065/api/semantic-model" -Method GET -TimeoutSec 5 -ErrorAction Stop
    if ($response.StatusCode -eq 200) {
        Write-Host "   ✅ 后台服务运行正常" -ForegroundColor Green
        $backendRunning = $true
    }
} catch {
    Write-Host "   ❌ 后台服务未运行，请先启动后台服务" -ForegroundColor Red
    Write-Host "   启动命令: cd spring-ai-alibaba-nl2sql-management && mvn spring-boot:run" -ForegroundColor Cyan
    $backendRunning = $false
}

Write-Host ""

if ($backendRunning) {
    # 测试API接口
    Write-Host "2. 测试API接口..." -ForegroundColor Yellow
    
    # 测试获取列表
    Write-Host "   - 测试获取语义模型列表..." -ForegroundColor White
    try {
        $listResponse = Invoke-WebRequest -Uri "http://localhost:8065/api/semantic-model" -Method GET -TimeoutSec 5
        Write-Host "   ✅ 获取列表接口正常" -ForegroundColor Green
    } catch {
        Write-Host "   ❌ 获取列表接口异常: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    # 测试搜索功能
    Write-Host "   - 测试搜索功能..." -ForegroundColor White
    try {
        $searchResponse = Invoke-WebRequest -Uri "http://localhost:8065/api/semantic-model?keyword=sales" -Method GET -TimeoutSec 5
        Write-Host "   ✅ 搜索接口正常" -ForegroundColor Green
    } catch {
        Write-Host "   ❌ 搜索接口异常: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""

# 检查前端服务
Write-Host "3. 检查前端服务状态..." -ForegroundColor Yellow
try {
    $frontendResponse = Invoke-WebRequest -Uri "http://localhost:3000" -Method GET -TimeoutSec 5 -ErrorAction Stop
    if ($frontendResponse.StatusCode -eq 200) {
        Write-Host "   ✅ 前端服务运行正常" -ForegroundColor Green
        Write-Host "   可访问: http://localhost:3000" -ForegroundColor Cyan
        $frontendRunning = $true
    }
} catch {
    Write-Host "   ❌ 前端服务未运行，请先启动前端服务" -ForegroundColor Red
    Write-Host "   启动命令: cd spring-ai-alibaba-nl2sql-web-ui && npm run dev" -ForegroundColor Cyan
    $frontendRunning = $false
}

Write-Host ""
Write-Host "=== 测试完成 ===" -ForegroundColor Green
Write-Host ""

if ($backendRunning -and $frontendRunning) {
    Write-Host "🎉 所有服务都正常运行！" -ForegroundColor Green
    Write-Host ""
    Write-Host "可以通过以下链接访问语义模型配置页面：" -ForegroundColor Yellow
    Write-Host "http://localhost:3000/#/semantic-model" -ForegroundColor Cyan
} else {
    Write-Host "⚠️  部分服务未运行，请检查服务状态" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "功能验证项目：" -ForegroundColor Yellow
Write-Host "□ 页面正常加载" -ForegroundColor White
Write-Host "□ 数据列表显示" -ForegroundColor White
Write-Host "□ 搜索功能正常" -ForegroundColor White
Write-Host "□ 新增配置功能" -ForegroundColor White
Write-Host "□ 编辑配置功能" -ForegroundColor White
Write-Host "□ 删除配置功能" -ForegroundColor White
Write-Host "□ 批量操作功能" -ForegroundColor White
Write-Host "□ 数据集筛选功能" -ForegroundColor White

Write-Host ""
Write-Host "提示：如果后台服务未运行，可以使用VS Code的任务来启动：" -ForegroundColor Gray
Write-Host "Ctrl+Shift+P -> Tasks: Run Task -> Start Backend" -ForegroundColor Gray
