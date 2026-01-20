<template>
  <div class="pending-order">
    <div class="page-header">
      <h2>待支付订单</h2>
    </div>

    <div class="order-list" v-if="orders.length > 0">
      <div v-for="order in orders" :key="order.orderId" class="order-card">
        <div class="order-header">
          <span class="order-id">订单号：{{ order.orderId }}</span>
          <span class="order-date">{{ formatDate(order.createTime) }}</span>
        </div>
        <div class="order-footer">
          <div class="total">
            <span>总计：</span>
            <span class="total-price">¥{{ order.totalAmount.toFixed(2) }}</span>
          </div>
          <div class="actions">
<!--            <el-button type="danger" @click="cancelOrder(order.id)">取消订单</el-button>-->
            <el-button type="primary" @click="payOrder(order)">立即支付</el-button>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-else description="暂无待支付订单" />

    <!-- 支付对话框 -->
    <el-dialog
      v-model="payDialogVisible"
      title="订单支付"
      width="400px"
    >
      <el-descriptions title="订单详情" border :column="2">
        <el-descriptions-item label="订单号">{{ currentOrder.orderId }}</el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ currentOrder.paymentMethod }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ currentOrder.createTime }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">￥{{ currentOrder.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">{{ currentOrder.status }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="payDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmPay">确认支付</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage} from 'element-plus'
import {getPendingOrders, startPay} from '../../api/order.ts'
import {Orders} from "../../type";

const orders = ref<Orders>([])
const payDialogVisible = ref(false)
const currentOrder = ref(null)
const paymentForm = ref('')


// 获取待支付订单列表
const fetchOrders = async () => {
  getPendingOrders().then(res => {
    if (res.data.code === '200') {
      orders.value = res.data.data
    } else if(res.data.code === '400') {
      ElMessage({
        message: res.data.msg,
        type: 'error',
        center: true,
      })
    }
  })
}

// 打开支付对话框
const payOrder = (order) => {
  currentOrder.value = order
  payDialogVisible.value = true
}

// 确认支付
const confirmPay = () => {
  if (!currentOrder.value) return

  startPay(Number(currentOrder.value.orderId)).then(res => {
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
  payDialogVisible.value = false
  fetchOrders()
}

// 格式化日期
const formatDate = (date) => {
  return new Date(date).toLocaleString()
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
.pending-order {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 24px;
  color: #333;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.order-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.order-header {
  display: flex;
  justify-content: space-between;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
  margin-bottom: 15px;
}

.order-id {
  font-weight: bold;
  color: #333;
}

.order-date {
  color: #999;
}

.order-items {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.order-item {
  display: flex;
  gap: 15px;
  padding: 10px 0;
}

.book-cover {
  width: 80px;
  height: 120px;
  object-fit: cover;
  border-radius: 4px;
}

.item-info {
  flex: 1;
}

.item-info h3 {
  margin: 0 0 10px 0;
  font-size: 16px;
  color: #333;
}

.price {
  color: #f56c6c;
  font-weight: bold;
  margin: 5px 0;
}

.quantity {
  color: #666;
  margin: 5px 0;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #eee;
}

.total {
  font-size: 16px;
}

.total-price {
  color: #f56c6c;
  font-weight: bold;
  font-size: 20px;
}

.actions {
  display: flex;
  gap: 10px;
}

.pay-dialog-content {
  text-align: center;
}

.pay-amount {
  font-size: 20px;
  color: #f56c6c;
  font-weight: bold;
  margin-bottom: 20px;
}

.pay-methods {
  margin-top: 20px;
}
</style> 