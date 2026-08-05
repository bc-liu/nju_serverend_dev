# TomatoMall Agent — 功能说明与接口测试文档

> 版本：v0.2.0 | 更新日期：2026-07-10

---

## 一、已实现功能清单

### P0 基建（已完成）
| 功能         | 说明                                             |
| ------------ | ------------------------------------------------ |
| LLM 网关     | DeepSeek（OpenAI 兼容接口），封装于 `ChatOpenAI` |
| SSE 流式输出 | `/agent/chat/stream`，逐 token 推送              |
| 配置管理     | `.env` + `pydantic-settings`                     |
| 跨域支持     | 全量 CORS                                        |

### P1 导购 Agent（本轮已完成）
| 功能                  | 实现方式                                                                                                                                                                             | 代码位置                                                                                                                   |
| --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------- |
| **ReAct 推理循环**    | LangChain v1 `create_agent` + 中间件架构（非已弃用的 `create_react_agent`）                                                                                                          | [shopping_agent.py](./src/agent/shopping_agent.py) |
| **多轮对话记忆**      | LangGraph Checkpointer（MemorySaver），通过 `conversation_id` 隔离会话                                                                                                               | [chat_memory.py](./src/memory/chat_memory.py)      |
| **多维度商品搜索**    | `search_products` 支持 keyword / max_price / min_price / min_rate / sort_by，keyword 在 title+description+detail 三字段匹配                                                          | [product_tool.py](./src/tool/product_tool.py)      |
| **Human-in-the-loop** | `HumanInTheLoopMiddleware` 框架级 HITL：`add_to_cart` 调用时中间件自动 `interrupt` 暂停图执行，用户通过 `/agent/resume` 用 `Command(resume={"decisions":[{"type":"approve"}]})` 恢复 | [shopping_agent.py](./src/agent/shopping_agent.py) |
| **引用溯源**          | 工具返回嵌入 `[ID:X]`，响应中提取 `references` 商品ID列表                                                                                                                            | [product_tool.py](./src/tool/product_tool.py)      |
| **工具集（5个）**     | search_products / get_product_detail / get_cart / add_to_cart / get_pending_orders                                                                                                   | 同上                                                                                                                       |

---

## 二、接口列表

| 方法 | 路径                 | 说明                                   |
| ---- | -------------------- | -------------------------------------- |
| GET  | `/health`            | 健康检查                               |
| POST | `/agent/chat`        | 非流式对话（含记忆 + HITL + 引用溯源） |
| POST | `/agent/chat/stream` | SSE 流式对话                           |
| POST | `/agent/resume`      | 恢复被中断的操作（二次确认/取消）      |

**Base URL**: `http://localhost:8000`

---

## 三、Postman 测试指南

### 准备工作
1. 启动 Java 后端（端口 8080）
2. 启动 Agent 服务：在 `tomatomall_agent` 目录下 `.\venv\Scripts\python.exe -m uvicorn src.app:app --host 0.0.0.0 --port 8000 --reload`
3. （加购测试需要）先登录 Java 后端获取 token：
   - `POST http://localhost:8080/api/accounts/login`，从响应中取 token

---

### 测试 1：健康检查

```
GET http://localhost:8000/health
```

**预期响应：**
```json
{
  "status": "ok",
  "llm_model": "deepseek-v4-flash",
  "backend_url": "http://localhost:8080"
}
```

---

### 测试 2：多维度商品搜索（验证 search_products 增强）

```
POST http://localhost:8000/agent/chat
Content-Type: application/json

{
  "message": "帮我找100块以内的书，按价格从低到高排"
}
```

**预期行为：** Agent 调用 `search_products(keyword="书", max_price=100, sort_by="price_asc")`

**预期响应：**
```json
{
  "response": "为您找到以下100元以内的书籍...\n[ID:2] 分布式系统...\n[ID:3] Java编程思想...",
  "conversation_id": "生成的UUID",
  "references": [2, 3]
}
```

---

### 测试 3：多轮对话记忆（验证 MemorySaver）

**第一轮请求：**
```
POST http://localhost:8000/agent/chat
Content-Type: application/json

{
  "message": "帮我推荐几本分布式系统的书",
  "conversation_id": "test-session-001"
}
```

> 记住返回的 `conversation_id`（这里显式传了 `test-session-001`）

**第二轮请求（用同一个 conversation_id）：**
```
POST http://localhost:8000/agent/chat
Content-Type: application/json

{
  "message": "刚才推荐的第二本书详细信息是什么？",
  "conversation_id": "test-session-001"
}
```

**预期行为：** Agent 能记住上一轮推荐了哪些书，并查询第二本的详情。

**验证点：** 如果不传 `conversation_id` 或传不同的值，Agent 将不知道"刚才"指什么。

---

### 测试 4：引用溯源

```
POST http://localhost:8000/agent/chat
Content-Type: application/json

{
  "message": "有什么电子产品推荐？"
}
```

**预期响应：** `references` 字段包含本次对话提及的所有商品 ID：
```json
{
  "response": "为您推荐以下电子产品：\n[ID:4] AirPods Pro 2...\n[ID:5] Apple Watch...",
  "conversation_id": "xxx",
  "references": [4, 5, 6]
}
```

前端可用 `references` 渲染可点击的商品卡片。

---

### 测试 5：Human-in-the-loop 二次确认（加购物车）

> 实现方式：`HumanInTheLoopMiddleware` 框架级 HITL。Agent 调用 `add_to_cart` 时，
> 中间件自动 `interrupt` 暂停图执行，返回 `need_confirm` 给客户端。
> 用户通过 `/agent/resume` 用 `Command(resume={"decisions":[{"type":"approve"}]})` 恢复。

**第 1 步：触发加购**

```
POST http://localhost:8000/agent/chat
Content-Type: application/json
token: 你的JWT_TOKEN

{
  "message": "帮我把分布式系统那本书加入购物车，1本",
  "conversation_id": "test-session-002"
}
```

**预期响应（中间件拦截，未实际加购）：**
```json
{
  "need_confirm": true,
  "confirm_info": {
    "type": "hitl_confirm",
    "tool": "add_to_cart",
    "args": {"product_id": 14, "quantity": 1},
    "message": "确认执行 add_to_cart（参数: {'product_id': 14, 'quantity': 1}）？"
  },
  "conversation_id": "test-session-002"
}
```

**第 2 步：确认加购**

```
POST http://localhost:8000/agent/resume
Content-Type: application/json
token: 你的JWT_TOKEN

{
  "conversation_id": "test-session-002",
  "confirmed": true
}
```

**预期响应（中间件放行，实际执行加购）：**
```json
{
  "response": "已成功将商品[ID:14] x1 加入购物车！",
  "conversation_id": "test-session-002",
  "references": [14]
}
```

**第 2 步（替代）：取消加购**
```json
{
  "conversation_id": "test-session-002",
  "confirmed": false
}
```

**预期响应：**
```json
{
  "response": "好的，已为您取消加购操作。",
  "conversation_id": "test-session-002",
  "references": [14]
}
```

---

### 测试 6：SSE 流式对话

```
POST http://localhost:8000/agent/chat/stream
Content-Type: application/json

{
  "message": "100块以内评分4.5以上的商品有哪些"
}
```

**预期 SSE 事件流：**
```
data: {"type":"tool_start","tool":"search_products"}

data: {"type":"tool_end","tool":"search_products","output":"共找到 5 件商品：\n[ID:1]..."}

data: {"type":"token","content":"为"}

data: {"type":"token","content":"您"}

data: {"type":"token","content":"找到"}

...

data: {"type":"references","product_ids":[1,2,3,7,9]}

data: {"type":"done","conversation_id":"xxx"}
```

> Postman 测试 SSE：在 Headers 中不要设置 Accept-Encoding，响应会以流式文本返回。

---

### 测试 7：查看购物车

```
POST http://localhost:8000/agent/chat
Content-Type: application/json
token: 你的JWT_TOKEN

{
  "message": "看看我的购物车",
  "conversation_id": "test-session-002"
}
```

---

## 四、测试数据

数据库中已有 10 条商品数据（通过 `V20260710__insert_sample_products.sql` 插入）：

| ID  | 商品名称                 | 价格  | 评分 | 品类     |
| --- | ------------------------ | ----- | ---- | -------- |
| 1   | 深入理解计算机系统       | ¥129  | 4.9  | 书籍     |
| 2   | 分布式系统：概念与设计   | ¥89   | 4.7  | 书籍     |
| 3   | Java编程思想             | ¥108  | 4.8  | 书籍     |
| 4   | AirPods Pro 2            | ¥1899 | 4.8  | 电子产品 |
| 5   | Apple Watch Series 9     | ¥3199 | 4.9  | 电子产品 |
| 6   | HHKB Professional HYBRID | ¥2699 | 4.9  | 电子产品 |
| 7   | 优衣库纯棉圆领T恤        | ¥79   | 4.6  | 服装     |
| 8   | Levi's 501经典牛仔裤     | ¥599  | 4.7  | 服装     |
| 9   | Anker USB-C快充线        | ¥39   | 4.8  | 数码配件 |
| 10  | 小米移动电源3 20000mAh   | ¥199  | 4.7  | 数码配件 |

**推荐测试话术：**
- "100块以内的书" → 验证价格+关键词筛选
- "评分4.8以上的电子产品" → 验证评分筛选
- "最便宜的数码配件" → 验证排序
- "帮我对比下ID 4和ID 5" → 验证多商品详情查询
- "把ID 2加入购物车" → 验证 HITL 二次确认

---

## 五、架构说明

```
用户 → FastAPI(Python:8000) → LangGraph ReAct Agent → 工具调用
                                                         ├─ search_products → Java GET /api/products
                                                         ├─ get_product_detail → Java GET /api/products/{id}
                                                         ├─ get_cart → Java GET /api/cart
                                                         ├─ add_to_cart → interrupt → /agent/resume → Java POST /api/cart
                                                         └─ get_pending_orders → Java GET /api/orders/pendingOrders

记忆：MemorySaver (进程内) ← thread_id = conversation_id
```

---

## 六、已知限制 & 后续计划

| 项           | 现状                        | 后续                                       |
| ------------ | --------------------------- | ------------------------------------------ |
| 记忆持久化   | MemorySaver（进程重启丢失） | 切换为 Redis/Postgres Checkpointer         |
| 长期用户画像 | 未实现                      | 从对话抽取偏好写入 DB                      |
| 下单链路     | 仅到加购物车                | 补 create_order / pay_order / cancel_order |
| Guardrail    | 未实现                      | Prompt Injection 拦截 + 价格幻觉校验       |
| 可观测性     | 仅日志                      | Trace 落库 + Token 计费                    |
| 摘要压缩     | 未实现                      | 超 N 轮触发 LLM 摘要                       |
