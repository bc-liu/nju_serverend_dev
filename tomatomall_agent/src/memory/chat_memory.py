"""对话记忆系统

采用 LangGraph Checkpointer 机制实现多轮对话记忆。
- 当前使用 MemorySaver（进程内），单实例部署足够。
- 后续可平滑切换为 Redis/Postgres 持久化，只需替换 checkpointer 实现。

thread_id 作为会话隔离维度，等于 conversation_id。
"""
import uuid
from typing import Optional

from langgraph.checkpoint.memory import MemorySaver
from langgraph.checkpoint.base import BaseCheckpointSaver

# 全局单例 checkpointer
_checkpointer: BaseCheckpointSaver = MemorySaver()


def get_checkpointer() -> BaseCheckpointSaver:
    """获取全局 checkpointer 实例"""
    return _checkpointer


def build_thread_config(conversation_id: Optional[str] = None) -> tuple[str, dict]:
    """构建 thread 配置。若未提供 conversation_id 则生成新的。

    返回 (conversation_id, config)
    """
    cid = conversation_id or str(uuid.uuid4())
    config = {"configurable": {"thread_id": cid}}
    return cid, config


def reset_memory() -> None:
    """清空所有记忆（仅用于测试）"""
    global _checkpointer
    _checkpointer = MemorySaver()
