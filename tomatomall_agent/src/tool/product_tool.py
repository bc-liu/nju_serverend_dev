"""调用 Java 后端 API 的工具集"""
from typing import Optional

import httpx
from langchain_core.tools import tool

from src.config import settings
import contextvars
#设置日志
import logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 共享 httpx 客户端
_client = httpx.AsyncClient(base_url=settings.backend_base_url, timeout=10.0)

#传递token
user_token_var = contextvars.ContextVar("user_token", default=None)

def set_user_token(token: str) -> None:
    """设置当前请求的用户 token"""
    user_token_var.set(token)


def get_user_token() -> Optional[str]:
    """获取当前请求的用户 token"""
    return user_token_var.get()

@tool
async def search_products(keyword: Optional[str] = None) -> str:
    """搜索商品列表。可以通过关键词筛选商品。返回所有商品的基本信息（名称、价格、评分、描述等）。

    Args:
        keyword: 可选的搜索关键词，用于在商品标题或描述中匹配
    """
    resp = await _client.get("/api/products")
    resp.raise_for_status()
    data = resp.json()

    products = data.get("data", [])
    if not products:
        return "暂无商品"

    # 如果有关键词，做简单的客户端过滤
    if keyword:
        keyword_lower = keyword.lower()
        filtered = [
            p for p in products
            if keyword_lower in (p.get("title") or "").lower()
            or keyword_lower in (p.get("description") or "").lower()
        ]
        products = filtered

    if not products:
        return f"未找到与「{keyword}」相关的商品"

    # 精简输出，避免 Token 浪费
    result_parts = []
    for p in products[:10]:  # 最多返回10个
        result_parts.append(
            f"[ID:{p['id']}] {p['title']} | 价格:¥{p['price']} | 评分:{p['rate']} | {p.get('description', '')}"
        )
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

    # 获取库存信息
    stock_info = ""
    stock_resp = await _client.get(f"/api/products/stockpile/{product_id}")
    if stock_resp.status_code == 200:
        stock_data = stock_resp.json().get("data", {})
        stock_info = f" | 库存: {stock_data.get('amount', 'N/A')}, 冻结: {stock_data.get('frozen', 'N/A')}"

    specs = p.get("specifications", [])
    spec_str = ""
    if specs:
        spec_str = "\n规格: " + ", ".join(
            f"{s.get('item','')}={s.get('value','')}" for s in specs
        )

    return (
        f"ID: {p['id']}\n"
        f"名称: {p['title']}\n"
        f"价格: ¥{p['price']}\n"
        f"评分: {p['rate']}\n"
        f"描述: {p.get('description', '无')}\n"
        f"详情: {p.get('detail', '无')}"
        f"{spec_str}{stock_info}"
    )


@tool
async def get_cart() -> str:
    """获取当前用户的购物车内容。使用当前会话的用户token进行身份验证。"""
    token = get_user_token()
    if not token:
        return "错误：未提供用户登录token，请先登录并在请求中携带token。"
    
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
                f"  [CartID:{item.get('cartItemId')}] {product.get('title')} x{qty} = ¥{subtotal:.2f}"
            )
        result_parts.append(f"合计: ¥{total:.2f}")
        return "\n".join(result_parts)
    except Exception as e:
        logger.error(f"get_cart failed: {e}")
        return f"获取购物车失败: {str(e)}"


@tool
async def add_to_cart(product_id: int, quantity: int) -> str:
    """将商品加入购物车。使用当前会话的用户token进行身份验证。

    Args:
        product_id: 商品ID
        quantity: 购买数量
    """
    token = get_user_token()
    if not token:
        return "错误：未提供用户登录token，请先登录并在请求中携带token。"
    
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
        if data.get("code") == "200" or data.get("code") == 200:
            return f"已成功将商品(ID:{product_id}) x{quantity} 加入购物车"
        return f"加入购物车失败: {data.get('msg', '未知错误')}"
    except httpx.HTTPStatusError as e:
        logger.error(f"add_to_cart HTTP error: {e}")
        return f"加入购物车失败（HTTP错误）: {e.response.status_code}"
    except Exception as e:
        logger.error(f"add_to_cart failed: {e}")
        return f"加入购物车失败: {str(e)}"


@tool
async def get_pending_orders() -> str:
    """获取当前用户的待支付订单列表。使用当前会话的用户token进行身份验证。"""
    token = get_user_token()
    if not token:
        return "错误：未提供用户登录token，请先登录并在请求中携带token。"
    
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
