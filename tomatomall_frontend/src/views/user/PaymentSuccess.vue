<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()

// 订单信息
const orderNumber = ref('')
const amount = ref('0.00')
const paymentTime = ref('')

// 获取路由参数中的订单信息
onMounted(() => {
  // 从路由参数或查询参数中获取订单信息
  const orderId = route.query.orderId as string
  const orderAmount = route.query.amount as string

  if (orderId) {
    orderNumber.value = orderId
  } else {
    orderNumber.value = generateOrderNumber()
  }

  if (orderAmount) {
    amount.value = orderAmount
  }

  paymentTime.value = new Date().toLocaleString('zh-CN')
})

// 生成随机订单号
const generateOrderNumber = () => {
  const timestamp = Date.now()
  const random = Math.floor(Math.random() * 1000)
  return `ORDER${timestamp}${random}`
}

// 跳转到订单页面
const goToOrders = () => {
  router.push('/user/MyBooks')
}

// 跳转到首页
const goToHome = () => {
  router.push('/home')
}
</script>

<template>
  <div class="payment-success-container">
    <div class="success-card">
      <!-- 成功图标 -->
      <div class="success-icon">
        <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="12" cy="12" r="10" stroke="#4CAF50" stroke-width="2"/>
          <path d="M8 12L11 15L16 9" stroke="#4CAF50" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>

      <!-- 成功标题 -->
      <h1 class="success-title">支付成功！</h1>
      <p class="success-subtitle">您的订单已成功支付，感谢您的购买</p>

      <!-- 订单信息 -->
      <div class="order-info">
        <div class="info-item">
          <span class="label">订单号：</span>
          <span class="value">{{ orderNumber }}</span>
        </div>
        <div class="info-item">
          <span class="label">支付金额：</span>
          <span class="value price">￥{{ amount }}</span>
        </div>
        <div class="info-item">
          <span class="label">支付时间：</span>
          <span class="value">{{ paymentTime }}</span>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <button class="btn btn-primary" @click="goToOrders">
          <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M9 12L11 14L15 10" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M21 12C21 16.9706 16.9706 21 12 21C7.02944 21 3 16.9706 3 12C3 7.02944 7.02944 3 12 3C16.9706 3 21 7.02944 21 12Z" stroke="currentColor" stroke-width="2"/>
          </svg>
          查看订单
        </button>
        <button class="btn btn-secondary" @click="goToHome">
          <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M3 9L12 2L21 9V20C21 20.5304 20.7893 21.0391 20.4142 21.4142C20.0391 21.7893 19.5304 22 19 22H5C4.46957 22 3.96086 21.7893 3.58579 21.4142C3.21071 21.0391 3 20.5304 3 20V9Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M9 22V12H15V22" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          继续购物
        </button>
      </div>

      <!-- 温馨提示 -->
      <div class="tips">
        <h3>温馨提示</h3>
        <ul>
          <li>您可以在"我的订单"中查看订单状态</li>
          <li>如有问题，请联系客服：15146705116</li>
        </ul>
      </div>
    </div>
  </div>
</template>


<style scoped>
.payment-success-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.success-card {
  background: white;
  border-radius: 20px;
  padding: 40px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  text-align: center;
  max-width: 500px;
  width: 100%;
}

.success-icon {
  margin-bottom: 20px;
}

.success-icon svg {
  width: 80px;
  height: 80px;
  animation: successAnimation 0.6s ease-in-out;
}

@keyframes successAnimation {
  0% {
    transform: scale(0);
    opacity: 0;
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

.success-title {
  color: #2c3e50;
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 10px;
}

.success-subtitle {
  color: #7f8c8d;
  font-size: 16px;
  margin-bottom: 30px;
}

.order-info {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 30px;
  text-align: left;
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.info-item:last-child {
  margin-bottom: 0;
}

.label {
  color: #6c757d;
  font-weight: 500;
}

.value {
  color: #2c3e50;
  font-weight: 600;
}

.value.price {
  color: #e74c3c;
  font-size: 18px;
}

.action-buttons {
  display: flex;
  gap: 15px;
  margin-bottom: 30px;
  justify-content: center;
  flex-wrap: wrap;
}

.btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  text-decoration: none;
}

.btn svg {
  width: 18px;
  height: 18px;
}

.btn-primary {
  background: #4CAF50;
  color: white;
}

.btn-primary:hover {
  background: #45a049;
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(76, 175, 80, 0.3);
}

.btn-secondary {
  background: #f8f9fa;
  color: #6c757d;
  border: 2px solid #e9ecef;
}

.btn-secondary:hover {
  background: #e9ecef;
  color: #495057;
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.tips {
  background: #fff3cd;
  border: 1px solid #ffeaa7;
  border-radius: 8px;
  padding: 20px;
  text-align: left;
}

.tips h3 {
  color: #856404;
  font-size: 16px;
  margin-bottom: 10px;
  font-weight: 600;
}

.tips ul {
  margin: 0;
  padding-left: 20px;
}

.tips li {
  color: #856404;
  font-size: 14px;
  margin-bottom: 5px;
  line-height: 1.4;
}

.tips li:last-child {
  margin-bottom: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .payment-success-container {
    padding: 10px;
  }
  
  .success-card {
    padding: 30px 20px;
  }
  
  .action-buttons {
    flex-direction: column;
  }
  
  .btn {
    width: 100%;
    justify-content: center;
  }
}
</style> 