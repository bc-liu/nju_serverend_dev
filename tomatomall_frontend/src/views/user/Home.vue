<script setup lang="ts">
import { ref } from 'vue'
import {router} from "../../router";
import {userInfo, userInfoUpdate} from "../../api/user.ts";
import {ElMessage} from "element-plus";
import {getAllProductInfo} from "../../api/products";
import {Product, User} from "../../type";
import {
  addProductToCart,
} from "../../api/shopping";


const user = ref<User>()
getUserInfo();
validateUser();

const isDropdownVisible = ref(false)
const isEditing = ref(false)
const editedUser = ref(user)

const products = ref<Product[]>([])
getProductInfo();


//保存修改的用户信息
const handleSave = () => {
  if (editedUser.value) {
    userInfoUpdate(editedUser.value).then(res => {
      if (res.data.code === '200') {
        ElMessage({
          message: "登录成功！",
          type: 'success',
          center: true,
        })
      }else {
        ElMessage({
          message: res.data.msg,
          type: 'error',
          center: true,
        })
      }
    })
    isEditing.value = false
    getUserInfo()
    editedUser.value = ref(user)
  }
}

//退出登录
const handleLogout = () => {
  router.push('/login')
}

//在加载界面前，先获得用户信息
function getUserInfo() {
  userInfo(sessionStorage.getItem("username")).then(res => {
    user.value = res.data.data;
  })
}

//在加载界面前，获得全部商品信息
function getProductInfo() {
  getAllProductInfo().then(res => {
    console.log(res.data.data)
    products.value = res.data.data;
  })
}

//验证用户身份
function validateUser() {
  if (!user) {
    ElMessage({
      message: "未登录",
      type: 'error',
      center: true,
    })
    router.push('/login')
  }
  if(sessionStorage.getItem("role")!='USER') {
    ElMessage({
      message: "当前身份无法查看该页面",
      type: 'error',
      center: true,
    })
    router.push('/login')
  }
}

//将商品添加到购物车
function addToCart(product) {
  if (!product.stockpile || product.stockpile.amount <= 0) {
    console.log(product.stockpile.amount)
    ElMessage.warning('商品库存不足')
    return
  }
  addProductToCart(Number(product.id), 1).then(res => {
    if (res.data.code === '200') {
      ElMessage.success('添加成功')
    } else if(res.data.code === '400') {
      ElMessage({
        message: res.data.msg,
        type: 'error',
        center: true,
      })
    }
  })
}

function goToProduct(productId) {
  router.push(`/products/${productId}`)
}

</script>

<template>
  <div class="app-container">
    <!-- Header -->
    <header class="header">
      <div class="header-content">
        <div class="logo">
          <h1>🍅 番茄书店</h1>
        </div>

        <nav class="nav-menu">
          <a href="#" class="nav-link">首页</a>
          <router-link to="/user/advertisement" class="nav-link">推荐</router-link>
          <router-link to="/user/cart" class="nav-link">购物车</router-link>
        </nav>

        <div v-if="user" class="user-menu" @click="isDropdownVisible = !isDropdownVisible">
          <img :src="user.avatar" :alt="user.name" class="user-avatar">
          <div v-if="isDropdownVisible" class="dropdown-menu">
            <router-link to="/user/profile" class="dropdown-item">个人中心</router-link>
            <router-link to="/user/myBooks" class="dropdown-item">我的书架</router-link>
            <router-link to="/user/pendingOrders" class="dropdown-item">未支付订单</router-link>
            <a href="#" class="dropdown-item" @click="handleLogout">退出登录</a>
          </div>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="main-content">
      <div class="content-grid">
        <!-- User Profile Section -->
        <div v-if="user" class="profile-card">
          <div class="profile-header">
            <img :src="user.avatar" :alt="user.name" class="profile-avatar">
            <h2>{{ user.name }}</h2>
            <p class="email">{{ user.email }}</p>
          </div>

          <div class="profile-info" v-if="!isEditing">
            <button class="edit-button" @click="isEditing = true">
              编辑资料
            </button>
          </div>

          <div class="profile-edit" v-if="isEditing===true">
            <div class="edit-form">
              <div class="form-group">
                <label>名称</label>
                <input v-model="editedUser.name" type="text">
              </div>
              <div class="form-group">
                <label>邮箱</label>
                <input v-model="editedUser.email" type="email">
              </div>
              <div class="button-group">
                <button class="save-button" @click="handleSave">保存</button>
                <button class="cancel-button" @click="isEditing = false">取消</button>
              </div>
            </div>
          </div>
        </div>

        <div class="shopping-container">
          <!-- 商品展示区 -->
          <div class="product-list">
            <el-card v-for="item in products" :key="item.id" class="product-card" shadow="hover">
              <img :src="item.cover" class="product-image" @click="goToProduct(Number(item.id))"/>
              <h3>{{ item.title }}</h3>
              <p class="price">￥{{ item.price }}</p>
              <el-button
                  type="primary"
                  :disabled="item.stockpile == null"
                  @click="addToCart(item)"
              >
                加入购物车
              </el-button>
            </el-card>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.app-container {
  min-height: 100vh;
}

.header {
  background: white;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 1rem 2rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo h1 {
  margin: 0;
  font-size: 1.5rem;
  color: #e74c3c;
}

.nav-menu {
  display: flex;
  gap: 2rem;
}

.nav-link {
  color: #2c3e50;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s;
}

.nav-link:hover {
  color: #e74c3c;
}

.user-menu {
  position: relative;
  cursor: pointer;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  padding: 0.5rem 0;
  margin-top: 0.5rem;
  min-width: 150px;
}

.dropdown-item {
  display: block;
  padding: 0.5rem 1rem;
  color: #2c3e50;
  text-decoration: none;
  transition: background-color 0.2s;
}

.dropdown-item:hover {
  background-color: #f5f7fa;
  color: #e74c3c;
}

.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

.content-grid {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 2rem;
  margin-top: 2rem;
}

.profile-card {
  background: white;
  border-radius: 12px;
  padding: 2rem;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
}

.profile-header {
  text-align: center;
  margin-bottom: 2rem;
}

.profile-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  margin-bottom: 1rem;
  object-fit: cover;
}

.profile-header h2 {
  margin: 0;
  color: #2c3e50;
}

.email {
  color: #666;
  margin-top: 0.5rem;
}

.profile-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  margin-bottom: 2rem;
  text-align: center;
}

.stat-item {
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 8px;
}

.stat-value {
  display: block;
  font-size: 1.5rem;
  font-weight: bold;
  color: #e74c3c;
}

.stat-label {
  color: #666;
  font-size: 0.9rem;
}

.edit-button {
  width: 100%;
  padding: 0.75rem;
  background: #e74c3c;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.edit-button:hover {
  background: #c0392b;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  color: #666;
}

.form-group input {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.button-group {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  margin-top: 1.5rem;
}

.save-button, .cancel-button {
  padding: 0.75rem;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.save-button {
  background: #e74c3c;
  color: white;
}

.save-button:hover {
  background: #c0392b;
}

.cancel-button {
  background: #f8f9fa;
  color: #2c3e50;
}

.cancel-button:hover {
  background: #e9ecef;
}

.bookshelf-section {
  background: white;
  border-radius: 12px;
  padding: 2rem;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.section-header h2 {
  margin: 0;
  color: #2c3e50;
}

.book-info h3 {
  margin: 0;
  font-size: 1.1rem;
  color: #2c3e50;
}

.author {
  color: #666;
  font-size: 0.9rem;
  margin: 0.5rem 0;
}

.progress-bar {
  height: 4px;
  background: #ddd;
  border-radius: 2px;
  margin: 0.5rem 0;
}

.progress {
  height: 100%;
  background: #e74c3c;
  border-radius: 2px;
}

.progress-text {
  font-size: 0.9rem;
  color: #666;
}

.recommendations {
  margin-top: 3rem;
}

.recommendations h2 {
  color: #2c3e50;
  margin-bottom: 1rem;
}

.recommendation-placeholder {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 2rem;
  text-align: center;
  color: #666;
}

@media (max-width: 768px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .header-content {
    flex-direction: column;
    gap: 1rem;
  }

  .nav-menu {
    width: 100%;
    justify-content: center;
  }
}

.shopping-container {
  padding: 2rem;
  background: #f9f9f9;
  min-height: 100vh;
}

.product-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
}

.product-card {
  text-align: center;
  padding: 1rem;
  transition: 0.3s;
}

.product-image {
  width: 100%;
  height: 150px;
  object-fit: cover;
  margin-bottom: 10px;
}

.price {
  font-weight: bold;
  color: #f56c6c;
  margin: 10px 0;
}

.floating-cart-btn {
  position: fixed;
  bottom: 30px;
  right: 30px;
  z-index: 1000;
}

.cart-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  gap: 10px;
}

.cart-footer {
  margin-top: 20px;
  text-align: right;
}

.total {
  font-size: 1.2rem;
  margin-bottom: 10px;
}

.empty-text {
  text-align: center;
  color: #999;
  padding: 2rem;
}
</style>