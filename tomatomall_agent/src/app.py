"""FastAPI 入口 — Agent 服务

接口：
- GET  /health              健康检查
- POST /agent/chat          非流式对话（含记忆 + HITL + 引用溯源）
- POST /agent/chat/stream   SSE 流式对话
- POST /agent/resume        恢复被 HITL 中间件暂停的操作（二次确认）
"""
import json
import logging
import uuid
from typing import Optional

from fastapi import FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from langchain_core.messages import HumanMessage
from langgraph.types import Command
from pydantic import BaseModel

from src.agent.shopping_agent import create_shopping_agent
from src.config import settings
from src.memory.chat_memory import build_thread_config
from src.tool.product_tool import extract_product_ids

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="TomatoMall Agent", version="0.3.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

agent = create_shopping_agent()


# ---------- 请求/响应模型 ----------

class ChatRequest(BaseModel):
    message: str
    conversation_id: Optional[str] = None


class ResumeRequest(BaseModel):
    conversation_id: str
    confirmed: bool


class HealthResponse(BaseModel):
    status: str
    llm_model: str
    backend_url: str


# ---------- 辅助函数 ----------

def _build_config(conversation_id: Optional[str], token: Optional[str]):
    """构建 LangGraph config，包含 thread_id（记忆）和 token（认证）"""
    cid = conversation_id or str(uuid.uuid4())
    config = {
        "configurable": {
            "thread_id": cid,
            "token": token,
        }
    }
    return cid, config


def _check_interrupt(config: dict):
    """检查 Agent 是否处于 HITL 中断等待状态。返回 interrupt 信息或 None。"""
    try:
        state = agent.get_state(config)
        if state.next:
            for task in state.tasks:
                if hasattr(task, "interrupts") and task.interrupts:
                    intr = task.interrupts[0]
                    value = intr.value
                    # HumanInTheLoopMiddleware 的 interrupt value 是 HITLRequest
                    reqs = None
                    if hasattr(value, "action_requests"):
                        reqs = value.action_requests
                    elif isinstance(value, dict) and "action_requests" in value:
                        reqs = value["action_requests"]
                    if reqs:
                        r = reqs[0]
                        tool_name = getattr(r, "name", "") or (r.get("name", "unknown") if isinstance(r, dict) else "unknown")
                        tool_args = getattr(r, "args", None) or (r.get("args", {}) if isinstance(r, dict) else {})
                        return {
                            "type": "hitl_confirm",
                            "tool": tool_name,
                            "args": tool_args,
                            "message": f"确认执行 {tool_name}（参数: {tool_args}）？",
                        }
                    # 兜底
                    return {"type": "interrupt", "value": str(value)}
    except Exception as e:
        logger.warning(f"_check_interrupt error: {e}")
    return None


def _build_response(messages, conversation_id: str) -> dict:
    """构建带引用溯源的响应"""
    ai_msg = ""
    if messages:
        last = messages[-1]
        ai_msg = getattr(last, "content", "") or ""
    references = extract_product_ids(messages)
    return {
        "response": ai_msg,
        "conversation_id": conversation_id,
        "references": references,
    }


# 拒绝关键词 — 用户在中断等待期间发来的消息包含这些词时视为拒绝
_REJECT_KEYWORDS = {"取消", "不要", "算了", "拒绝", "否", "不", "cancel", "no", "nope"}


def _is_reject(message: str) -> bool:
    """解析用户在中断等待期间发送的消息意图，返回 True 表示拒绝"""
    msg = message.strip().lower()
    return any(kw in msg for kw in _REJECT_KEYWORDS)


async def _do_resume(config: dict, conversation_id: str, confirmed: bool, user_message: str = "") -> dict:
    """执行 HITL 恢复（批准/拒绝挂起的工具调用）"""
    if confirmed:
        resume_value = {"decisions": [{"type": "approve"}]}
    else:
        reason = f"用户取消: {user_message}" if user_message else "用户取消"
        resume_value = {"decisions": [{"type": "reject", "args": {"reason": reason}}]}

    try:
        result = await agent.ainvoke(
            Command(resume=resume_value),
            config=config,
        )
    except Exception as e:
        logger.error(f"resume failed: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"恢复执行失败: {str(e)}")

    return _build_response(result.get("messages", []), conversation_id)


# ---------- 接口 ----------

@app.get("/health", response_model=HealthResponse)
async def health():
    return HealthResponse(
        status="ok",
        llm_model=settings.llm_model,
        backend_url=settings.backend_base_url,
    )


@app.post("/agent/chat")
async def chat(req: ChatRequest, token: Optional[str] = Header(None)):
    """非流式对话接口

    - 支持多轮记忆（通过 conversation_id 关联）
    - 当触发二次确认时，返回 need_confirm 字段，需调用 /agent/resume 继续
    - 返回 references 字段，包含本次对话提及的商品ID列表（引用溯源）
    """
    conversation_id, config = _build_config(req.conversation_id, token)

    try:
        result = await agent.ainvoke(
            {"messages": [HumanMessage(content=req.message)]},
            config=config,
        )
    except Exception as e:
        logger.error(f"agent.ainvoke failed: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Agent 执行失败: {str(e)}")

    # 检查是否被 HITL 中间件暂停（等待二次确认）
    interrupt_info = _check_interrupt(config)
    if interrupt_info:
        return {
            "need_confirm": True,
            "confirm_info": interrupt_info,
            "conversation_id": conversation_id,
        }

    return _build_response(result.get("messages", []), conversation_id)


@app.post("/agent/chat/stream")
async def chat_stream(req: ChatRequest, token: Optional[str] = Header(None)):
    """SSE 流式对话接口

    事件类型：
    - token:        LLM 生成的文本片段
    - tool_start:   工具调用开始
    - tool_end:     工具调用结束（含截断输出）
    - need_confirm: 需要用户二次确认（携带 confirm_info）
    - references:   本次对话引用的商品ID列表
    - done:         结束
    """
    conversation_id, config = _build_config(req.conversation_id, token)

    async def event_generator():
        try:
            async for event in agent.astream_events(
                {"messages": [HumanMessage(content=req.message)]},
                config=config,
                version="v2",
            ):
                kind = event.get("event")

                if kind == "on_chat_model_stream":
                    chunk = event["data"]["chunk"]
                    if hasattr(chunk, "content") and chunk.content:
                        yield f"data: {json.dumps({'type': 'token', 'content': chunk.content}, ensure_ascii=False)}\n\n"

                elif kind == "on_tool_start":
                    tool_name = event.get("name", "unknown")
                    yield f"data: {json.dumps({'type': 'tool_start', 'tool': tool_name}, ensure_ascii=False)}\n\n"

                elif kind == "on_tool_end":
                    tool_name = event.get("name", "unknown")
                    output = str(event["data"].get("output", ""))[:200]
                    yield f"data: {json.dumps({'type': 'tool_end', 'tool': tool_name, 'output': output}, ensure_ascii=False)}\n\n"
        except Exception as e:
            logger.error(f"stream error: {e}", exc_info=True)
            yield f"data: {json.dumps({'type': 'error', 'message': str(e)}, ensure_ascii=False)}\n\n"

        # 流结束后检查是否被 HITL 中断
        interrupt_info = _check_interrupt(config)
        if interrupt_info:
            yield f"data: {json.dumps({'type': 'need_confirm', 'confirm_info': interrupt_info, 'conversation_id': conversation_id}, ensure_ascii=False)}\n\n"
        else:
            try:
                state = agent.get_state(config)
                refs = extract_product_ids(state.values.get("messages", []))
                yield f"data: {json.dumps({'type': 'references', 'product_ids': refs}, ensure_ascii=False)}\n\n"
            except Exception:
                pass

        yield f"data: {json.dumps({'type': 'done', 'conversation_id': conversation_id}, ensure_ascii=False)}\n\n"

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )


@app.post("/agent/resume")
async def resume(req: ResumeRequest, token: Optional[str] = Header(None)):
    """恢复被 HITL 中间件暂停的操作（二次确认/取消）

    使用同一 conversation_id 恢复执行。
    confirmed=True → Command(resume=[{"type":"accept"}]) 批准工具调用
    confirmed=False → Command(resume=[{"type":"reject","args":{"reason":"用户取消"}}]) 拒绝
    """
    _, config = _build_config(req.conversation_id, token)

    # 确认当前确实处于中断状态
    interrupt_info = _check_interrupt(config)
    if not interrupt_info:
        raise HTTPException(status_code=400, detail="当前会话没有等待确认的操作")

    if req.confirmed:
        resume_value = {"decisions": [{"type": "approve"}]}
    else:
        resume_value = {"decisions": [{"type": "reject", "args": {"reason": "用户取消"}}]}

    try:
        result = await agent.ainvoke(
            Command(resume=resume_value),
            config=config,
        )
    except Exception as e:
        logger.error(f"resume failed: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"恢复执行失败: {str(e)}")

    return _build_response(result.get("messages", []), req.conversation_id)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("src.app:app", host="0.0.0.0", port=settings.agent_port, reload=True)
