"""导购 Agent — 基于 LangChain v1 create_agent + 中间件架构

特性：
- ReAct 推理循环（Thought → Action → Observation）
- 接入 Checkpointer 实现多轮对话记忆
- HumanInTheLoopMiddleware 框架级二次确认（add_to_cart 调用前自动 interrupt）
"""
from langchain_openai import ChatOpenAI
from langchain.agents import create_agent
from langchain.agents.middleware import HumanInTheLoopMiddleware

from src.config import settings
from src.memory.chat_memory import get_checkpointer
from src.tool.product_tool import ALL_TOOLS

SYSTEM_PROMPT = """你是 TomatoMall 电商平台的智能导购助手。你的职责是：

1. **理解用户需求**：用户可能用自然语言描述购物需求（如"100块以内的分布式系统书"），你需要理解其意图，并转换为搜索条件调用 search_products。
2. **搜索与推荐**：使用 search_products 工具（支持 keyword/max_price/min_price/min_rate/sort_by 参数）查找商品，根据用户需求筛选和排序。需要详情时调用 get_product_detail。
3. **对比分析**：当用户在多个商品间犹豫时，帮他们做对比分析。
4. **购物车操作**：使用 add_to_cart 工具帮用户将选中的商品加入购物车。系统会自动对加购操作进行二次确认，你只需正常调用即可。
5. **多轮记忆**：你可以记住当前会话中之前的对话内容，用户提到"刚才那个""第二本书"时可以结合上下文理解。

注意事项：
- 商品价格、库存等事实信息必须来自工具调用结果，严禁自行编造。
- 推荐时要给出具体理由，并引用商品ID（格式 [ID:数字]）方便用户确认。
- 用户登录 token 已通过系统上下文自动传递，你不需要在对话中询问或处理 token。
- 如果工具返回"未提供用户登录token"或"认证失败"，请告诉用户需要先登录。
- 回复要简洁友好，像一位专业的导购员。"""


def create_shopping_agent():
    """创建导购 Agent（v1 架构：create_agent + HITL 中间件 + 记忆）"""
    llm = ChatOpenAI(
        model=settings.llm_model,
        api_key=settings.llm_api_key,
        base_url=settings.llm_base_url,
        temperature=0.3,
        streaming=True,
    )

    agent = create_agent(
        model=llm,
        tools=ALL_TOOLS,
        system_prompt=SYSTEM_PROMPT,
        middleware=[
            HumanInTheLoopMiddleware(
                interrupt_on={"add_to_cart": True},
            ),
        ],
        checkpointer=get_checkpointer(),
    )
    return agent
