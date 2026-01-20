<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {Product} from "../../type";
import {getProductInfo, getStockpile} from "../../api/products";
import {addProductToCart} from "../../api/shopping";

const route = useRoute()
const router = useRouter()
const productId = route.params.id
const quantity = ref(1)

const product = ref<Product>()
getProductById(productId)

function getProductById(id) {
  getProductInfo(Number(id)).then(res => {
    if (res.data.code === '200') {
      product.value = res.data.data
      getStockpile(productId).then(res => {
        product.value.stockpile = res.data.data
      })
    } else {
      ElMessage({
        message: res.data.msg,
        type: 'error',
        center: true,
      })
    }
  })
}

const goBack = () => {
  router.back()
}

const addToCart = () => {
  if (!product.value.stockpile || product.value.stockpile.amount <= 0) {
    ElMessage.warning('商品库存不足')
    return
  }
  if (quantity.value > product.value.stockpile.amount) {
    ElMessage.warning('超出库存数量')
    return
  }
  addProductToCart(Number(productId), quantity.value).then(res => {
    if (res.data.code === '200') {
      ElMessage.success('添加成功')
    } else {
      ElMessage.error(res.data.msg)
    }
  })
}
</script>

<template>
  <div class="product-detail-container">
    <div class="product-card">
      <!-- 商品封面图 -->
      <div class="product-image-section">
        <el-image
          :src="product?.cover"
          fit="cover"
          class="product-image"
          :preview-src-list="[product?.cover]"
        />
      </div>

      <!-- 商品信息 -->
      <div class="product-info-section">
        <div class="product-header">
          <h1 class="product-title">{{ product?.title }}</h1>
          <div class="product-price">¥{{ product?.price?.toFixed(2) }}</div>
        </div>

        <div class="product-description">
          {{ product?.description }}
          <div class="rating">
            评分：
            <span class="rate-number">{{ product?.rate }}</span>
            <span class="star">★</span>
          </div>
        </div>

        <div class="product-specs">
          <h3>商品规格</h3>
          <div class="specs-grid">
            <el-tag 
              v-for="(spec, idx) in product?.specifications" 
              :key="idx"
              class="spec-tag"
            >
              {{ spec.item }}: {{ spec.value }}
            </el-tag>
          </div>
        </div>

        <div class="product-stock">
          <h3>库存状态</h3>
          <div class="stock-info" :class="{ 'low-stock': product?.stockpile?.amount <= 10 }">
            <el-icon><Box /></el-icon>
            <span>当前库存: {{ product?.stockpile?.amount || 0 }} 件</span>
          </div>
        </div>

        <div class="product-actions">
          <el-button 
            type="primary" 
            size="large" 
            class="add-to-cart-btn"
            :disabled="!product?.stockpile?.amount"
            @click="addToCart"
          >
            <el-icon><ShoppingCart /></el-icon>
            加入购物车
          </el-button>
        </div>

        <div class="action-buttons">
          <el-button @click="goBack" plain>
            <el-icon><Back /></el-icon>
            返回
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.product-detail-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);
  padding: 2rem;
}

.product-card {
  max-width: 1200px;
  margin: 0 auto;
  background: white;
  border-radius: 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 2rem;
  padding: 2rem;
}

@media (min-width: 768px) {
  .product-card {
    flex-direction: row;
  }
}

.product-image-section {
  flex: 1;
  min-width: 300px;
}

.product-image {
  width: 100%;
  height: 400px;
  border-radius: 12px;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.product-image:hover {
  transform: scale(1.02);
}

.product-info-section {
  flex: 1;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.product-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

.product-title {
  font-size: 2rem;
  font-weight: 700;
  color: #2c3e50;
  margin: 0;
}

.product-price {
  font-size: 2rem;
  font-weight: 700;
  color: #e74c3c;
}

.product-description {
  color: #666;
  line-height: 1.6;
  font-size: 1.1rem;
}

.product-specs {
  background: #f8f9fa;
  padding: 1rem;
  border-radius: 12px;
}

.product-specs h3 {
  margin: 0 0 1rem 0;
  color: #2c3e50;
}

.specs-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.spec-tag {
  font-size: 0.9rem;
}

.product-stock {
  background: #f8f9fa;
  padding: 1rem;
  border-radius: 12px;
}

.product-stock h3 {
  margin: 0 0 1rem 0;
  color: #2c3e50;
}

.stock-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #27ae60;
  font-size: 1.1rem;
}

.stock-info.low-stock {
  color: #e74c3c;
}

.product-actions {
  display: flex;
  gap: 1rem;
  align-items: center;
  margin-top: 1rem;
}

.quantity-selector {
  width: 150px;
}

.add-to-cart-btn {
  flex: 1;
  height: 48px;
  font-size: 1.1rem;
}

.action-buttons {
  display: flex;
  gap: 1rem;
  margin-top: 1rem;
}

@media (max-width: 768px) {
  .product-card {
    padding: 1rem;
  }

  .product-title {
    font-size: 1.5rem;
  }

  .product-price {
    font-size: 1.5rem;
  }

  .product-actions {
    flex-direction: column;
  }

  .quantity-selector {
    width: 100%;
  }

  .rating {
    margin-top: 8px;
    font-weight: bold;
    font-size: 16px;
    color: #333;
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .rate-number {
    font-size: 20px;
    color: #ff8c00; /* 橘黄色，吸引注意 */
  }

  .star {
    color: gold;
    font-size: 18px;
  }

}
</style>
