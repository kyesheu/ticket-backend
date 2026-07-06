#!/bin/bash
# =============================================
# Ticket API v1.0 集成测试脚本
# 用法: bash test-api.sh
# =============================================
set -e

BASE="http://localhost:8080"
PASS=0
FAIL=0

# 颜色
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

assert_contains() {
  local desc="$1" expected="$2" actual="$3"
  if echo "$actual" | grep -q "$expected"; then
    echo -e "  ${GREEN}[PASS]${NC} $desc"
    ((PASS++))
  else
    echo -e "  ${RED}[FAIL]${NC} $desc"
    echo "    expected to contain: $expected"
    echo "    actual: ${actual:0:300}"
    ((FAIL++))
  fi
}

assert_success() {
  local desc="$1" actual="$2"
  assert_contains "$desc" '"code":200' "$actual"
}

assert_error() {
  local desc="$1" actual="$2"
  assert_contains "$desc" '"code":500' "$actual"
}

echo "=========================================="
echo "  Ticket API v1.0 Integration Tests"
echo "=========================================="

# ---- 登录 ----
echo ""
echo "[0] 登录获取 Token"
LOGIN=$(curl -s -X POST "$BASE/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')
TOKEN=$(echo "$LOGIN" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
if [ -z "$TOKEN" ]; then
  echo "FATAL: 登录失败, 无法获取 Token"
  echo "Response: $LOGIN"
  exit 1
fi
AUTH="Authorization: Bearer $TOKEN"
CT="Content-Type: application/json"
echo "Token: ${TOKEN:0:30}..."

# ---- 分类模块 ----
echo ""
echo "[1] 分类管理"

R=$(curl -s -H "$AUTH" "$BASE/ticket/category/tree")
assert_success "GET /ticket/category/tree" "$R"
assert_contains "  树结构包含子节点" 'children' "$R"
assert_contains "  树包含一级分类" 'IT' "$R"

R=$(curl -s -H "$AUTH" "$BASE/ticket/category/list")
assert_success "GET /ticket/category/list" "$R"

# 新增
R=$(curl -s -X POST -H "$AUTH" -H "$CT" \
  -d '{"parentId":1,"categoryName":"E2E-TEST-CAT","orderNum":99}' \
  "$BASE/ticket/category")
assert_success "POST /ticket/category (新增)" "$R"

# 修改
R=$(curl -s -X PUT -H "$AUTH" -H "$CT" \
  -d '{"categoryId":9,"parentId":1,"categoryName":"E2E-TEST-MODIFIED","orderNum":99}' \
  "$BASE/ticket/category")
assert_success "PUT /ticket/category (修改)" "$R"

# 删除
R=$(curl -s -X DELETE -H "$AUTH" "$BASE/ticket/category/9")
assert_success "DELETE /ticket/category/9 (删除无子节点)" "$R"

R=$(curl -s -X DELETE -H "$AUTH" "$BASE/ticket/category/2")
assert_error "DELETE /ticket/category/2 (有子节点-应拒绝)" "$R"

# ---- 工单创建 ----
echo ""
echo "[2] 工单创建"

R1=$(curl -s -X POST -H "$AUTH" -H "$CT" \
  -d '{"title":"E2E-工单1 WiFi故障","content":"办公室WiFi无法连接","categoryId":6,"priority":"HIGH"}' \
  "$BASE/ticket")
assert_success "POST /ticket (创建工单1 HIGH优先级)" "$R1"
T1=$(echo "$R1" | grep -o '"data":[0-9]*' | grep -o '[0-9]*')
echo "  工单1 ID: $T1"

R2=$(curl -s -X POST -H "$AUTH" -H "$CT" \
  -d '{"title":"E2E-工单2 申请办公用品","content":"需要A4纸和签字笔","categoryId":7}' \
  "$BASE/ticket")
assert_success "POST /ticket (创建工单2 默认优先级)" "$R2"
T2=$(echo "$R2" | grep -o '"data":[0-9]*' | grep -o '[0-9]*')
echo "  工单2 ID: $T2"

# 列表
R=$(curl -s -H "$AUTH" "$BASE/ticket/list?pageNum=1&pageSize=10")
assert_success "GET /ticket/list (分页)" "$R"
assert_contains "  含total字段" '"total":' "$R"

R=$(curl -s -H "$AUTH" "$BASE/ticket/list?status=NEW")
assert_success "GET /ticket/list?status=NEW (按状态)" "$R"

R=$(curl -s -H "$AUTH" "$BASE/ticket/list?priority=HIGH")
assert_success "GET /ticket/list?priority=HIGH (按优先级)" "$R"

R=$(curl -s -H "$AUTH" "$BASE/ticket/list?keyword=WiFi")
assert_success "GET /ticket/list?keyword=WiFi (关键词)" "$R"

# 详情
R=$(curl -s -H "$AUTH" "$BASE/ticket/$T1")
assert_success "GET /ticket/$T1 (详情)" "$R"
assert_contains "  含ticketNo" 'ticketNo' "$R"
assert_contains "  含comments" 'comments' "$R"
assert_contains "  含logs" 'logs' "$R"

# ---- 合法状态流转 ----
echo ""
echo "[3] 合法状态流转"

R=$(curl -s -X PUT -H "$AUTH" -H "$CT" \
  -d '{"assigneeId":1,"comment":"分配给管理员处理"}' \
  "$BASE/ticket/$T1/assign")
assert_success "PUT /ticket/$T1/assign (NEW->PROCESSING)" "$R"

R=$(curl -s -X PUT -H "$AUTH" -H "$CT" \
  -d '{"comment":"已更换路由器，WiFi恢复"}' \
  "$BASE/ticket/$T1/process")
assert_success "PUT /ticket/$T1/process (PROCESSING->WAIT_CONFIRM)" "$R"

R=$(curl -s -X PUT -H "$AUTH" -H "$CT" \
  -d '{"comment":"确认问题已解决"}' \
  "$BASE/ticket/$T1/confirm")
assert_success "PUT /ticket/$T1/confirm (WAIT_CONFIRM->CLOSED)" "$R"

R=$(curl -s -X PUT -H "$AUTH" -H "$CT" \
  -d '{"comment":"不需要处理了"}' \
  "$BASE/ticket/$T2/cancel")
assert_success "PUT /ticket/$T2/cancel (NEW->CANCELLED)" "$R"

# ---- 非法状态流转 ----
echo ""
echo "[4] 非法状态流转"

R=$(curl -s -X PUT -H "$AUTH" -H "$CT" \
  -d '{"assigneeId":1,"comment":"重复分派"}' \
  "$BASE/ticket/$T1/assign")
assert_error "PUT /ticket/$T1/assign (CLOSED不可分派)" "$R"
assert_contains "  提示不允许分派" '不允许分派' "$R"

R=$(curl -s -X PUT -H "$AUTH" -H "$CT" \
  -d '{"comment":"想处理已关闭的"}' \
  "$BASE/ticket/$T1/process")
assert_error "PUT /ticket/$T1/process (CLOSED不可处理)" "$R"
assert_contains "  提示不允许处理" '不允许处理' "$R"

R=$(curl -s -X PUT -H "$AUTH" -H "$CT" \
  -d '{"comment":"重复确认"}' \
  "$BASE/ticket/$T1/confirm")
assert_error "PUT /ticket/$T1/confirm (CLOSED不可再确认)" "$R"

R=$(curl -s -X PUT -H "$AUTH" -H "$CT" \
  -d '{"comment":"想取消已取消的"}' \
  "$BASE/ticket/$T2/cancel")
assert_error "PUT /ticket/$T2/cancel (CANCELLED不可再取消)" "$R"

R=$(curl -s -X PUT -H "$AUTH" -H "$CT" \
  -d '{"comment":""}' \
  "$BASE/ticket/$T1/process")
assert_error "PUT /ticket/$T1/process (备注为空)" "$R"

R=$(curl -s -X PUT -H "$AUTH" -H "$CT" \
  -d '{"comment":""}' \
  "$BASE/ticket/$T1/cancel")
assert_error "PUT /ticket/$T1/cancel (取消原因为空)" "$R"

R=$(curl -s -X PUT -H "$AUTH" -H "$CT" \
  -d '{"assigneeId":9999,"comment":"不存在的人"}' \
  "$BASE/ticket/$T2/assign")
assert_error "PUT /ticket/$T2/assign (指派人不存在)" "$R"

# ---- 评论 ----
echo ""
echo "[5] 评论功能"

R=$(curl -s -X POST -H "$AUTH" -H "$CT" \
  -d '{"content":"请尽快处理，影响工作了","commentType":"EXTERNAL"}' \
  "$BASE/ticket/$T1/comment")
assert_success "POST /ticket/$T1/comment (添加评论)" "$R"

R=$(curl -s -X POST -H "$AUTH" -H "$CT" \
  -d '{"content":"可能需要采购新设备","commentType":"INTERNAL"}' \
  "$BASE/ticket/$T1/comment")
assert_success "POST /ticket/$T1/comment (内部备注)" "$R"

R=$(curl -s -H "$AUTH" "$BASE/ticket/$T1/comment")
assert_success "GET /ticket/$T1/comment (查看评论)" "$R"

R=$(curl -s -X POST -H "$AUTH" -H "$CT" \
  -d '{"content":""}' \
  "$BASE/ticket/$T1/comment")
assert_error "POST /ticket/$T1/comment (评论内容为空)" "$R"

# ---- 操作日志 ----
echo ""
echo "[6] 操作日志"

R=$(curl -s -H "$AUTH" "$BASE/ticket/$T1/logs")
assert_success "GET /ticket/$T1/logs (查看日志)" "$R"
assert_contains "  包含CREATE" 'CREATE' "$R"
assert_contains "  包含ASSIGN" 'ASSIGN' "$R"
assert_contains "  包含PROCESS" 'PROCESS' "$R"
assert_contains "  包含CONFIRM" 'CONFIRM' "$R"

# ---- 权限 ----
echo ""
echo "[7] 权限验证"

R=$(curl -s "$BASE/ticket/list")
assert_contains "GET /ticket/list (无Token)" '"code":401' "$R"

R=$(curl -s "$BASE/ticket/category/tree")
assert_contains "GET /ticket/category/tree (无Token)" '"code":401' "$R"

# ---- 结果 ----
echo ""
echo "=========================================="
echo -e "  结果: ${GREEN}$PASS 通过${NC}, ${RED}$FAIL 失败${NC}"
echo "=========================================="
if [ $FAIL -gt 0 ]; then
  exit 1
fi
