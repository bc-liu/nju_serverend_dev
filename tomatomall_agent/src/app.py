"""FastAPI 入口 — Agent 服务"""
import json
import uuid
from typing import Optional

from fastapi import FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from langchain_core.messages import HumanMessage
from pydantic import BaseModel

from src.agent.shopping_agent import create_shopping_agent
from src.config import settings
from src.tool.product_tool import set_user_token

app = FastAPI(title="TomatoMall Agent", version="0.1.0")

# 跨域配置（开发环境）
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 全局 Agent 实例
agent = create_shopping_agent()


# ---------- 请求/响应模型 ----------

class ChatRequest(BaseModel):
    message: str
    conversation_id: Optional[str] = None


class HealthResponse(BaseModel):
    status: str
    llm_model: str
    backend_url: str


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
    """非流式对话接口（调试用）"""
    user_msg = req.message
    #设置当前请求的用户token
    set_user_token(token)

    result = await agent.ainvoke(
        {"messages": [HumanMessage(content=req.message)]}
    )
    # 取最后一条 AI 消息
    ai_msg = result["messages"][-1].content
    return {"response": ai_msg, "conversation_id": req.conversation_id or str(uuid.uuid4())}


@app.post("/agent/chat/stream")
async def chat_stream(req: ChatRequest, token: Optional[str] = Header(None)):
    """SSE 流式对话接口"""
    set_user_token(token)

    async def event_generator():
        async for event in agent.astream_events(
            {"messages": [HumanMessage(content=req.message)]},
            version="v2",
        ):
            kind = event.get("event")

            # 流式输出 LLM 生成的 token
            if kind == "on_chat_model_stream":
                chunk = event["data"]["chunk"]
                if hasattr(chunk, "content") and chunk.content:
                    yield f"data: {json.dumps({'type': 'token', 'content': chunk.content}, ensure_ascii=False)}\n\n"

            # 工具调用开始
            elif kind == "on_tool_start":
                tool_name = event.get("name", "unknown")
                yield f"data: {json.dumps({'type': 'tool_start', 'tool': tool_name}, ensure_ascii=False)}\n\n"

            # 工具调用结束
            elif kind == "on_tool_end":
                tool_name = event.get("name", "unknown")
                output = str(event["data"].get("output", ""))[:200]
                yield f"data: {json.dumps({'type': 'tool_end', 'tool': tool_name, 'output': output}, ensure_ascii=False)}\n\n"

        yield f"data: {json.dumps({'type': 'done'}, ensure_ascii=False)}\n\n"

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("src.app:app", host="0.0.0.0", port=settings.agent_port, reload=True)
