# -*- coding: utf-8 -*-
<#
.SYNOPSIS
    TomatoMall 500并发压测一键运行脚本
.DESCRIPTION
    使用 JMeter 非 GUI 模式运行压测，自动生成 HTML 报告和 CSV 结果
    测试完成后自动调用 Python 脚本分析成功率
.NOTES
    使用方法: 右键 -> 使用 PowerShell 运行
    或在终端执行: powershell -ExecutionPolicy Bypass -File run_stress_test.ps1
#>

$JMETER = "D:\Develop\JMeter\apache-jmeter-5.6.3\bin\jmeter.bat"
$TEST_PLAN = "d:\Develop\软工2\TomatoMall2.0\nju_serverend_dev\tomatomall_backend\src\test\jmeter_stress_test.jmx"
$RESULT_DIR = "d:\Develop\软工2\TomatoMall2.0\nju_serverend_dev\tomatomall_backend\src\test\jmeter_results"
$JTL_FILE = "$RESULT_DIR\result.jtl"
$HTML_DIR = "$RESULT_DIR\html_report"

# 创建结果目录
if (!(Test-Path $RESULT_DIR)) { New-Item -ItemType Directory -Path $RESULT_DIR -Force | Out-Null }
if (Test-Path $HTML_DIR) { Remove-Item $HTML_DIR -Recurse -Force }
if (Test-Path $JTL_FILE) { Remove-Item $JTL_FILE -Force }

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  TomatoMall 500并发压测" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "[1/3] 检查后端服务..." -ForegroundColor Yellow
$conn = Test-NetConnection -ComputerName localhost -Port 8080 -WarningAction SilentlyContinue
if ($conn.TcpTestSucceeded) {
    Write-Host "  -> 后端服务运行中 (8080端口可达)" -ForegroundColor Green
} else {
    Write-Host "  -> [错误] 后端服务未启动！请先启动后端。" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "[2/3] 启动 JMeter 压测 (非GUI模式)..." -ForegroundColor Yellow
Write-Host "  -> 测试计划: $TEST_PLAN"
Write-Host "  -> 线程数:   500"
Write-Host "  -> 爬升时间: 10秒"
Write-Host "  -> 持续时间: 60秒"
Write-Host "  -> 结果文件: $JTL_FILE"
Write-Host "  -> HTML报告: $HTML_DIR"
Write-Host ""

& $JMETER -n -t $TEST_PLAN -l $JTL_FILE -e -o $HTML_DIR -JTHREADS=500 -JRAMPUP=10 -JDURATION=60

Write-Host ""
Write-Host "[3/3] 分析测试结果..." -ForegroundColor Yellow

# 调用 Python 脚本分析结果
$pythonScript = "d:\Develop\软工2\TomatoMall2.0\nju_serverend_dev\tomatomall_backend\src\test\analyze_jmeter_results.py"
if (Test-Path $pythonScript) {
    python $pythonScript $JTL_FILE
} else {
    Write-Host "  Python分析脚本未找到，请手动查看 HTML 报告" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  压测完成！" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "HTML报告: $HTML_DIR\index.html"
Write-Host "CSV结果:  $JTL_FILE"
Write-Host ""
Write-Host "按任意键打开HTML报告..." -ForegroundColor Yellow
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
Start-Process "$HTML_DIR\index.html"
