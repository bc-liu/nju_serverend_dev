<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from "element-plus";
import { CartItem, Product } from "../../type";
import {
  deleteCartItemById,
  getCartList,
  submitOrder,
  updateQuantity
} from "../../api/shopping";
import { pcaTextArr } from "element-china-area-data";
import { startPay } from "../../api/order";

const carts = ref<CartItem[]>([])
const selectedItems = ref<number[]>([])
const cartVisible = ref(false)
const totalAmount = ref(0)
const total = ref(0)

// 弹窗控制
const payDialogVisible = ref(false)
const isOrderSubmitted = ref(false)
const paymentForm = ref('')
const paymentMethod = ref('alipay')
const selectedRegion = ref([])

const order = ref({
  orderId: '',
  userId: '',
  totalAmount: 0,
  paymentMethod: '',
  createTime: '',
  status: '',
})

// 计算选中的商品总价
const selectedTotalAmount = computed(() => {
  return carts.value
    .filter(item => selectedItems.value.includes(item.cartItemId))
    .reduce((total, item) => total + item.product.price * item.quantity, 0)
})

// 计算选中的商品数量
const selectedTotal = computed(() => {
  return carts.value
    .filter(item => selectedItems.value.includes(item.cartItemId))
    .reduce((total, item) => total + item.quantity, 0)
})

// 全选/取消全选
const toggleSelectAll = () => {
  if (selectedItems.value.length === carts.value.length) {
    selectedItems.value = []
  } else {
    selectedItems.value = carts.value.map(item => item.cartItemId)
  }
}

// 获取购物车列表
function getCart() {
  getCartList().then(res => {
    if (res.data.code === '200') {
      carts.value = res.data.data.carts;
      total.value = res.data.data.total;
      totalAmount.value = res.data.data.totalAmount;
    } else if(res.data.code === '400') {
      ElMessage({
        message: res.data.msg,
        type: 'error',
        center: true,
      })
    }
  })
}

// 更新商品数量
function updateItemQuantity(cartItemId, quantity) {
  updateQuantity(Number(cartItemId), {quantity: quantity}).then(res => {
    if (res.data.code === '200') {
      getCart()
    } else if(res.data.code === '400') {
      ElMessage({
        message: res.data.msg,
        type: 'error',
        center: true,
      })
    }
  })
}

// 删除购物车商品
function removeFromCart(id) {
  deleteCartItemById(Number(id)).then(res => {
    if (res.data.code === '200') {
      getCart()
      selectedItems.value = selectedItems.value.filter(itemId => itemId !== id)
    } else if(res.data.code === '400') {
      ElMessage({
        message: res.data.msg,
        type: 'error',
        center: true,
      })
    }
  })
}

// 初始化订单
function initOrder() {
  order.value = {
    orderId: '',
    username: '',
    totalAmount: 0,
    paymentMethod: '',
    createTime: '',
    status: '',
  }
}

// 打开支付弹窗
function openPayDialog() {
  if (selectedItems.value.length === 0) {
    ElMessage.warning('请选择要购买的商品')
    return
  }
  payDialogVisible.value = true
}

function closePayDialog() {
  payDialogVisible.value = false
  isOrderSubmitted.value = false
  paymentMethod.value = ''
  selectedRegion.value = []
  initOrder()
}

function handleSubmitPay() {
  if (selectedItems.value.length === 0) {
    ElMessage.warning('请选择要购买的商品')
    return
  }
  const fullAddress = selectedRegion.value.join(',');
  submitOrder(selectedItems.value, fullAddress, paymentMethod.value).then(res => {
    if (res.data.code === '200') {
      order.value = res.data.data
      isOrderSubmitted.value = true
    } else if(res.data.code === '400') {
      ElMessage({
        message: res.data.msg,
        type: 'error',
        center: true,
      })
    }
  })
}

// 支付订单
function payOrder() {
  startPay(Number(order.value.orderId)).then(res => {
    console.log(res.data)

    // 创建一个容器 div（避免破坏 Vue）
    const container = document.createElement('div')
    container.innerHTML = res.data // 这是后端返回的一段完整的 <form>...</form> HTML

    document.body.appendChild(container)

    const form = container.querySelector('form')
    if (form) {
      form.submit()
    } else {
      console.error('form not found in response')
    }
  })
  selectedItems.value = []
  getCart()
  payDialogVisible.value = false
}

// 初始化
getCart()
</script>

<template>
  <div class="cart-container">
    <h2>我的购物车</h2>
    
    <div v-if="carts.length === 0" class="empty-cart">
      <el-empty description="购物车是空的" />
    </div>
    
    <div v-else class="cart-content">
      <div class="cart-header">
        <el-checkbox
          :model-value="selectedItems.length === carts.length"
          @change="toggleSelectAll"
        >
          全选
        </el-checkbox>
        <span>商品信息</span>
        <span>单价</span>
        <span>数量</span>
        <span>小计</span>
        <span>操作</span>
      </div>

      <div class="cart-items">
        <div v-for="item in carts" :key="item.cartItemId" class="cart-item">
          <el-checkbox
            v-model="selectedItems"
            :label="item.cartItemId"
          />
          <div class="item-info">
            <img :src="item.product.cover" :alt="item.product.title" class="item-image">
            <span class="item-title">{{ item.product.title }}</span>
          </div>
          <span class="item-price">￥{{ item.product.price }}</span>
          <el-input-number
            v-model="item.quantity"
            :min="1"
            @change="updateItemQuantity(item.cartItemId, item.quantity)"
          />
          <span class="item-subtotal">￥{{ item.product.price * item.quantity }}</span>
          <el-button type="danger" icon="Delete" @click="removeFromCart(item.cartItemId)" circle />
        </div>
      </div>

      <div class="cart-footer">
        <div class="selected-info">
          已选择 {{ selectedTotal }} 件商品
        </div>
        <div class="total-amount">
          总计：￥{{ selectedTotalAmount }}
        </div>
        <el-button type="primary" @click="openPayDialog" :disabled="selectedItems.length === 0">
          结算
        </el-button>
      </div>
    </div>

    <!-- 支付弹窗 -->
    <el-dialog v-model="payDialogVisible" title="确认支付信息">
      <div class="mb-3">
        <el-cascader
          placeholder="请选择省市区"
          size="large"
          :options="pcaTextArr"
          v-model="selectedRegion">
        </el-cascader>
      </div>

      <div class="mb-3">
        <p>选择支付方式：</p>
        <el-radio-group v-model="paymentMethod">
          <el-radio label="alipay">支付宝</el-radio>
          <el-radio label="wechat">微信</el-radio>
          <el-radio label="credit_card">信用卡</el-radio>
        </el-radio-group>
      </div>

      <template #footer>
        <el-button @click="closePayDialog">取消</el-button>
        <el-button type="primary" @click="handleSubmitPay">提交订单</el-button>
      </template>

      <div v-if="isOrderSubmitted" class="mt-6 p-4 border rounded shadow">
        <el-descriptions title="订单详情" border :column="2">
          <el-descriptions-item label="订单号">{{ order.orderId }}</el-descriptions-item>
          <el-descriptions-item label="支付方式">{{ order.paymentMethod }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ order.createTime }}</el-descriptions-item>
          <el-descriptions-item label="订单金额">￥{{ order.totalAmount }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">{{ order.status }}</el-descriptions-item>
        </el-descriptions>

        <div class="mt-4 text-right">
          <el-button type="success" @click="payOrder">立即支付</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.cart-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

.cart-header {
  display: grid;
  grid-template-columns: 80px 3fr 1fr 1fr 1fr 1fr;
  padding: 1rem;
  background: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 1rem;
  align-items: center;
}

.cart-items {
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.cart-item {
  display: grid;
  grid-template-columns: 80px 3fr 1fr 1fr 1fr 1fr;
  padding: 1rem;
  align-items: center;
  border-bottom: 1px solid #ebeef5;
}

.cart-item:last-child {
  border-bottom: none;
}

.item-info {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.item-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
}

.item-title {
  font-size: 0.9rem;
  color: #333;
}

.item-price, .item-subtotal {
  color: #f56c6c;
  font-weight: bold;
}

.cart-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 2rem;
  margin-top: 2rem;
  padding: 1rem;
  background: #f5f7fa;
  border-radius: 4px;
}

.selected-info {
  color: #666;
}

.total-amount {
  font-size: 1.2rem;
  color: #f56c6c;
  font-weight: bold;
}

.empty-cart {
  padding: 4rem 0;
  text-align: center;
}
</style> 