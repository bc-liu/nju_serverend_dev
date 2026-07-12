"""调用 Java 后端 API 的工具集

特性：
- search_products 支持多字段筛选（keyword 匹配 title/description/detail）+ 价格区间 + 评分 + 排序
- add_to_cart 引入 Human-in-the-loop：通过 LangGraph interrupt 暂停，等待用户确认后才执行
- 工具返回结构化文本，商品 ID 以 [ID:X] 形式嵌入，便于引用溯源
- token 通过 LangGraph RunnableConfig 传递（不依赖 contextvars）
"""
import re
from typing import Annotated, Optional

import httpx
from langchain_core.tools import tool, InjectedToolArg
from langchain_core.runnables import RunnableConfig

from src.config import settings
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 共享 httpx 客户端
_client = httpx.AsyncClient(base_url=settings.backend_base_url, timeout=10.0)


def _get_token_from_config(config: RunnableConfig) -> Optional[str]:
    """从 LangGraph RunnableConfig 中获取用户 token"""
    if config is None:
        return None
    configurable = config.get("configurable", {}) if isinstance(config, dict) else {}
    return configurable.get("token")


# ---------- 引用溯源辅助 ----------

def extract_product_ids(messages) -> list[int]:
    """从 Agent 的消息历史中提取所有被提及的商品 ID，用于引用溯源。"""
    ids = []
    seen = set()
    for msg in messages:
        content = getattr(msg, "content", "") or ""
        for m in re.finditer(r"\[ID:(\d+)\]", content):
            pid = int(m.group(1))
            if pid not in seen:
                seen.add(pid)
                ids.append(pid)
    return ids


@tool
async def search_products(
    keyword: Optional[str] = None,
    max_price: Optional[float] = None,
    min_price: Optional[float] = None,
    min_rate: Optional[float] = None,
    sort_by: Optional[str] = None,
) -> str:
    """搜索商品列表。支持多维度筛选与排序。

    Args:
        keyword: 搜索关键词，会在商品的 title / description / detail 三个字段中模糊匹配
        max_price: 价格上限（含），如 100 表示 100 元及以下
        min_price: 价格下限（含）
        min_rate: 最低评分（含），如 4.5 表示评分 4.5 及以上
        sort_by: 排序方式，可选值：price_asc(价格升序) / price_desc(价格降序) / rate_desc(评分降序)
    """
    resp = await _client.get("/api/products")
    resp.raise_for_status()
    data = resp.json()

    products = data.get("data", [])
    if not products:
        return "暂无商品"

    # --- 多字段关键词匹配 ---
    if keyword:
        kw = keyword.lower()
        products = [
            p for p in products
            if kw in (p.get("title") or "").lower()
            or kw in (p.get("description") or "").lower()
            or kw in (p.get("detail") or "").lower()
        ]

    # --- 价格区间 ---
    if min_price is not None:
        products = [p for p in products if float(p.get("price", 0)) >= min_price]
    if max_price is not None:
        products = [p for p in products if float(p.get("price", 0)) <= max_price]

    # --- 评分过滤 ---
    if min_rate is not None:
        products = [p for p in products if float(p.get("rate", 0)) >= min_rate]

    # --- 排序 ---
    if sort_by == "price_asc":
        products.sort(key=lambda p: float(p.get("price", 0)))
    elif sort_by == "price_desc":
        products.sort(key=lambda p: float(p.get("price", 0)), reverse=True)
    elif sort_by == "rate_desc":
        products.sort(key=lambda p: float(p.get("rate", 0)), reverse=True)

    if not products:
        cond = []
        if keyword: cond.append(f"关键词「{keyword}」")
        if max_price is not None: cond.append(f"价格≤{max_price}")
        if min_price is not None: cond.append(f"价格≥{min_price}")
        if min_rate is not None: cond.append(f"评分≥{min_rate}")
        return f"未找到满足条件（{'，'.join(cond)}）的商品"

    # --- 结构化输出，嵌入 [ID:X] 供引用溯源 ---
    result_parts = [f"共找到 {len(products)} 件商品："]
    for p in products[:15]:
        result_parts.append(
            f"[ID:{p['id']}] {p['title']} | 价格:¥{p['price']} | 评分:{p['rate']} | {p.get('description', '')}"
        )
    if len(products) > 15:
        result_parts.append(f"...还有 {len(products) - 15} 件，请缩小条件查看")
    return "\n".join(result_parts)


@tool
async def get_product_detail(product_id: int) -> str:
    """获取指定商品的详细信息，包括规格、库存等。

    Args:
        product_id: 商品ID
    """
    resp = await _client.get(f"/api/products/{product_id}")
    resp.raise_for_status()
    data = resp.json()
    p = data.get("data", {})
    if not p:
        return f"未找到ID为{product_id}的商品"

    stock_info = ""
    try:
        stock_resp = await _client.get(f"/api/products/stockpile/{product_id}")
        if stock_resp.status_code == 200:
            stock_data = stock_resp.json().get("data", {})
            stock_info = f" | 库存: {stock_data.get('amount', 'N/A')}, 冻结: {stock_data.get('frozen', 'N/A')}"
    except Exception:
        pass

    specs = p.get("specifications", [])
    spec_str = ""
    if specs:
        spec_str = "\n规格: " + ", ".join(
            f"{s.get('item', '')}={s.get('value', '')}" for s in specs
        )

    return (
        f"[ID:{p['id']}] {p['title']}\n"
        f"价格: ¥{p['price']}\n"
        f"评分: {p['rate']}\n"
        f"描述: {p.get('description', '无')}\n"
        f"详情: {p.get('detail', '无')}"
        f"{spec_str}{stock_info}"
    )


@tool
async def get_cart(config: Annotated[RunnableConfig, InjectedToolArg]) -> str:
    """获取当前用户的购物车内容。需要用户已登录。"""
    token = _get_token_from_config(config)
    if not token:
        return "错误：未提供用户登录token，请先登录。"

    try:
        resp = await _client.get("/api/cart", headers={"token": token})
        if resp.status_code == 401:
            return "认证失败：token无效或已过期，请重新登录。"
        resp.raise_for_status()
        data = resp.json().get("data", {})
        items = data.get("carts", [])
        if not items:
            return "购物车为空"

        result_parts = ["购物车商品:"]
        total = 0
        for item in items:
            product = item.get("product", {})
            price = float(product.get("price", 0))
            qty = item.get("quantity", 0)
            subtotal = price * qty
            total += subtotal
            result_parts.append(
                f"  [CartID:{item.get('cartItemId')}] [ID:{product.get('id')}] {product.get('title')} x{qty} = ¥{subtotal:.2f}"
            )
        result_parts.append(f"合计: ¥{total:.2f}")
        return "\n".join(result_parts)
    except Exception as e:
        logger.error(f"get_cart failed: {e}")
        return f"获取购物车失败: {str(e)}"


@tool
async def add_to_cart(product_id: int, quantity: int, config: Annotated[RunnableConfig, InjectedToolArg]) -> str:
    """将商品加入购物车。此操作受 HumanInTheLoopMiddleware 保护，调用前会自动触发二次确认。

    Args:
        product_id: 商品ID
        quantity: 购买数量
    """
    token = _get_token_from_config(config)
    if not token:
        return "错误：未提供用户登录token，请先登录。"

    try:
        resp = await _client.post(
            "/api/cart",
            params={"productId": product_id, "quantity": quantity},
            headers={"token": token},
        )
        logger.info(f"add_to_cart response: status={resp.status_code}, body={resp.text[:200]}")

        if resp.status_code == 401:
            return "认证失败：token无效或已过期，请重新登录。"
        resp.raise_for_status()
        data = resp.json()
        if str(data.get("code")) == "200":
            return f"已成功将商品[ID:{product_id}] x{quantity} 加入购物车"
        return f"加入购物车失败: {data.get('msg', '未知错误')}"
    except httpx.HTTPStatusError as e:
        logger.error(f"add_to_cart HTTP error: {e}")
        return f"加入购物车失败（HTTP错误）: {e.response.status_code}"
    except Exception as e:
        logger.error(f"add_to_cart failed: {e}")
        return f"加入购物车失败: {str(e)}"


@tool
async def get_pending_orders(config: Annotated[RunnableConfig, InjectedToolArg]) -> str:
    """获取当前用户的待支付订单列表。需要用户已登录。"""
    token = _get_token_from_config(config)
    if not token:
        return "错误：未提供用户登录token，请先登录。"

    try:
        resp = await _client.get("/api/orders/pendingOrders", headers={"token": token})
        if resp.status_code == 401:
            return "认证失败：token无效或已过期，请重新登录。"
        resp.raise_for_status()
        data = resp.json().get("data", [])
        if not data:
            return "暂无待支付订单"

        result_parts = ["待支付订单:"]
        for o in data:
            result_parts.append(
                f"  [订单ID:{o.get('orderId')}] 总价:¥{o.get('totalPrice')} | 状态:{o.get('status')} | {o.get('shoppingAddress', '')}"
            )
        return "\n".join(result_parts)
    except Exception as e:
        logger.error(f"get_pending_orders failed: {e}")
        return f"获取待支付订单失败: {str(e)}"


# 导出所有工具
ALL_TOOLS = [search_products, get_product_detail, get_cart, add_to_cart, get_pending_orders]
