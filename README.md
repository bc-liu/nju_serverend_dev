# TomatoMall — 高并发电商 + AI 智能导购 Agent

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.6-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.5-4fc08d)](https://vuejs.org/)
[![Python](https://img.shields.io/badge/Python-3.10+-blue)](https://www.python.org/)
[![LangChain](https://img.shields.io/badge/LangChain-v1-1c3c3c)](https://www.langchain.com/)
[![LangGraph](https://img.shields.io/badge/LangGraph-0.2+-orange)](https://langchain-ai.github.io/langgraph/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

一个从零搭建的**全栈电商系统**，涵盖高并发秒杀、多级缓存、异步解耦等生产级架构，并在其上构建了基于 **LangGraph ReAct Agent** 的 AI 智能导购——支持自然语言商品搜索、多轮对比、Human-in-the-Loop 安全加购。

---

## 项目概览

本项目由两个紧密协作的子项目组成：

### 🛒 电商后台（tomatomall_backend + tomatomall_frontend）

完整的前后端分离电商系统，覆盖商品、库存、购物车、订单、支付全链路。核心解决**高并发下的超卖、缓存一致性、消息可靠消费**三个生产级难题。

| 指标 | 结果 |
|------|------|
| 并发订单 QPS | **1000+** |
| 接口响应优化 | 86ms → **22ms**（↓74%） |
| 500并发成功率 | **99.5%+** |
| 超卖 | **0** |

### 🤖 AI 智能导购 Agent（tomatomall_agent）

在电商后台之上构建的 **LangGraph ReAct Agent**。用户可以用自然语言完成商品搜索、对比分析、加购等全链路操作——比如 _"500元以内降噪最好的耳机，对比索尼和Bose"_。核心亮点：

- **ReAct 推理循环**：Thought → Action → Observation 自主决策调用工具
- **Human-in-the-Loop**：框架级中间件架构，敏感操作自动中断等待确认
- **多轮记忆**：支持 _"刚才那个"_、_"第二本书"_ 等上下文指代
- **引用溯源**：所有推荐可追溯到具体商品 ID
- **SSE 流式输出**：逐 token 推送，优化长对话体验

---

## 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                      用户 / 前端                         │
│              Vue 3 + Element Plus (:80)                  │
└──────────┬──────────────────────────┬───────────────────┘
           │                          │
           ▼                          ▼
┌──────────────────────┐   ┌──────────────────────────────┐
│   Java 后端 (:8080)   │   │   AI Agent (:8000)            │
│   Spring Boot 2.7.6   │◄──│   FastAPI + LangGraph        │
│                      │   │   ReAct Agent                 │
│  ┌────────────────┐  │   │                              │
│  │ 多级缓存架构    │  │   │  ┌──────────────────────┐    │
│  │ L1 Caffeine     │  │   │  │ HumanInTheLoop       │    │
│  │ L2 Redis        │  │   │  │ Middleware            │    │
│  │ 布隆过滤器      │  │   │  └──────────────────────┘    │
│  └────────────────┘  │   │  ┌──────────────────────┐    │
│  ┌────────────────┐  │   │  │ 5 Domain Tools       │    │
│  │ Kafka 异步解耦  │  │   │  │ search/get/cart/...  │    │
│  │ 订单/支付/缓存  │  │   │  └──────────────────────┘    │
│  └────────────────┘  │   │  ┌──────────────────────┐    │
│  ┌────────────────┐  │   │  │ MemorySaver          │    │
│  │ 支付宝沙箱支付  │  │   │  │ (多轮记忆)           │    │
│  └────────────────┘  │   │  └──────────────────────┘    │
└──────────┬───────────┘   └──────────────────────────────┘
           │
    ┌──────┴───────┐
    ▼              ▼
┌────────┐  ┌──────────┐
│ MySQL  │  │  Redis   │
│ 8.0    │  │  7.0      │
└────────┘  └──────────┘
```

---

## 技术亮点

### 🎯 多级缓存架构（L1 Caffeine + L2 Redis）

```
请求 → L1 Caffeine (纳秒级) → L2 Redis (毫秒级) → MySQL
```

- **防击穿**：Redisson 分布式锁 + Watchdog 自动续期 + Pub/Sub 高效唤醒
- **防穿透**：Redis BitMap 布隆过滤器 + 空值缓存
- **一致性**：延迟双删策略 + Kafka 跨实例 L1 缓存失效通知
- **效果**：接口响应从 86ms → 22ms（提速 74%），QPS 1000+

### 🔒 库存精准扣减（零超卖）

- 数据库原子行锁 + Redis Lua 脚本双重保障
- 预冻结 + 超时回滚模型
- 乐观锁 → 悲观锁的演进路径完整记录

### 📨 Kafka 异步解耦

- 订单创建 → 支付 → 库存结算全链路异步化
- 事件日志表 + 幂等校验 + 手动 ACK 保证不丢不重
- 缓存失效消息通过 Kafka 广播实现多实例 L1 一致性

### 🤖 ReAct Agent + Human-in-the-Loop

- LangChain v1 `create_agent` + LangGraph 中间件架构
- **框架级 HITL**：新增 HITL 工具只需在 `interrupt_on` 中配置，无需修改业务代码
- `InjectedToolArg` + `RunnableConfig` 解决异步执行下 contextvars 丢失的 401 鉴权问题
- 5 个领域工具，Pydantic 参数校验，SSE 流式输出

### 🏗️ 工程化

- Docker Compose 一键部署基础设施（MySQL + Redis + Kafka）
- Flyway 数据库版本管理
- JMeter 压测验证 + Python 性能分析脚本
- 全局异常处理 + AOP 调用统计 + 注解式权限控制

---

## 快速开始

```bash
# 1. 克隆项目
git clone https://github.com/bc-liu/nju_serverend_dev.git
cd nju_serverend_dev

# 2. 启动基础设施（MySQL + Redis + Kafka）
docker compose up -d

# 3. 初始化 Kafka Topic
# 详见: docs/KAFKA_SETUP.md

# 4. 启动 Java 后端（端口 8080）
cd tomatomall_backend && mvn spring-boot:run

# 5. 启动 AI Agent（端口 8000）
cd ../tomatomall_agent && python -m uvicorn src.app:app --port 8000

# 6. 启动前端（端口 80）
cd ../tomatomall_frontend && npm install && npm run dev
```

> 💡 详细部署文档与环境变量说明见 [docs/DEPLOY.md](./docs/DEPLOY.md)

---

## 子项目文档

| 项目 | 文档 | 说明 |
|------|------|------|
| 🤖 AI Agent | [AGENT_GUIDE.md](./tomatomall_agent/AGENT_GUIDE.md) | 功能说明 · API 接口 · Postman 测试 · 已知限制 |
| 🛒 电商后台 | [性能测试手册](./tomatomall_backend/性能测试与数据验证手册.md) | 压测方案 · 缓存性能验证 · 超卖验证 |
| 📡 API 参考 | [docs/API.md](./docs/API.md) | 完整 REST API 文档 |

---

## 技术栈

### 后端
| 技术 | 用途 |
|------|------|
| Spring Boot 2.7.6 | 基础框架 |
| Spring Data JPA | 数据访问 |
| Spring Security + JWT | 认证授权 |
| MySQL 8.0 | 持久化存储 |
| Redis 7.0 + Redisson | 缓存 & 分布式锁 |
| Caffeine | L1 本地缓存 |
| Kafka 3.9 | 消息队列 |
| Flyway | 数据库迁移 |
| Docker | 容器化部署 |

### Agent
| 技术 | 用途 |
|------|------|
| FastAPI + Uvicorn | HTTP 服务 |
| LangChain v1 + LangGraph | Agent 框架 |
| DeepSeek (OpenAI 兼容) | LLM 推理 |
| Pydantic | 数据校验 |
| httpx | HTTP 客户端 |

### 前端
| 技术 | 用途 |
|------|------|
| Vue 3 + TypeScript | 前端框架 |
| Element Plus | UI 组件库 |
| Axios | HTTP 客户端 |
| Vite | 构建工具 |

---

## 目录结构

```
nju_serverend_dev/
├── tomatomall_backend/        # Java Spring Boot 电商后台
│   ├── src/main/java/.../     # 控制器/服务/实体/工具
│   ├── src/main/resources/    # Flyway 迁移 + Lua 脚本
│   └── src/test/              # 单元测试 + 性能脚本
├── tomatomall_frontend/       # Vue 3 前端
│   └── src/views/             # 页面组件
├── tomatomall_agent/          # Python AI Agent
│   └── src/
│       ├── agent/             # ReAct Agent 定义
│       ├── tool/              # 5 个领域工具
│       └── memory/            # 多轮对话记忆
├── docker-compose.yml         # 基础设施编排
└── docs/                      # 详细文档
```

---

## 后续规划

- [ ] 记忆持久化：MemorySaver → Redis/Postgres Checkpointer
- [ ] 长期用户画像：从对话历史抽取偏好
- [ ] Guardrail：Prompt Injection 拦截 + 幻觉校验
- [ ] 可观测性：Trace 落库 + Token 计费
- [ ] 摘要压缩：超 N 轮触发 LLM 摘要
- [ ] Spring Boot 3.x 迁移

---

**个人项目** · 欢迎 Star ⭐ · 问题反馈请提 [Issue](https://github.com/bc-liu/nju_serverend_dev/issues)
