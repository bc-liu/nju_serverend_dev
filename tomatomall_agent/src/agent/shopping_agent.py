"""导购 Agent — 基于 LangGraph ReAct 模式"""
from langchain_openai import ChatOpenAI
from langgraph.prebuilt import create_react_agent

from src.config import settings
from src.tool.product_tool import ALL_TOOLS

# 系统 Prompt
SYSTEM_PROMPT = """你是 TomatoMall 电商平台的智能导购助手。你的职责是：

1. **理解用户需求**：用户可能用自然语言描述购物需求（如"100块以内的分布式系统书"），你需要理解其意图。
2. **搜索与推荐**：使用 search_products 和 get_product_detail 工具查找商品，根据用户需求筛选和排序。
3. **对比分析**：当用户在多个商品间犹豫时，帮他们做对比分析。
4. **购物车操作**：使用 add_to_cart 工具帮用户将选中的商品加入购物车，使用 get_cart 工具查看购物车。

注意事项：
- 商品价格、库存等事实信息必须来自工具调用结果，严禁自行编造。
- 推荐时要给出具体理由，并引用商品ID方便用户确认。

- 用户登录 token 已通过系统上下文自动传递，你不需要在对话中询问或处理 token。
- 如果工具返回"未提供用户登录token"或"认证失败"，请告诉用户需要先登录。
- 回复要简洁友好，像一位专业的导购员。"""


def create_shopping_agent():
    """创建导购 ReAct Agent"""
    llm = ChatOpenAI(
        model=settings.llm_model,
        api_key=settings.llm_api_key,
        base_url=settings.llm_base_url,
        temperature=0.3,
        streaming=True,
    )

    agent = create_react_agent(
        model=llm,
        tools=ALL_TOOLS,
        prompt=SYSTEM_PROMPT,
    )
    return agent
