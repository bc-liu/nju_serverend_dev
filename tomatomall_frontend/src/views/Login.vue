<script setup lang="ts">
import { ref } from 'vue';
import {ElForm, ElFormItem, ElInput, ElButton, ElLink, ElRow, ElCol, ElCard, ElMessage} from 'element-plus';
import {userInfo, userLogin} from '../api/user.ts';
import {router} from '../router/index';

// 登录表单的响应式数据
const form = ref({
  username: '',
  password: ''
});

// 表单验证规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
};

// 登录按钮触发
function handleLogin() {
  userLogin(form.value).then(res => {
    console.log(res.data.code);
    if (res.data.code === '200') {
      ElMessage({
        message: "登录成功！",
        type: 'success',
        center: true,
      })
      const token = res.data.data
      console.log(token);
      sessionStorage.setItem('token', token)

      userInfo(form.value.username).then(res => {
        sessionStorage.setItem('username', res.data.data.username)
        sessionStorage.setItem('role', res.data.data.role)
        if(res.data.data.role === 'ADMIN') {
          router.push({path: "/admin/home"})
        } else {
          router.push({path: "/home"})
        }
      })
    } else  {
      ElMessage({
        message: res.data.msg,
        type: 'error',
        center: true,
      })
      password.value = ''
    }
  })
}
</script>


<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h1>🍅 番茄书店</h1>
        <p>欢迎回来</p>
      </div>

      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label for="email">用户名</label>
          <input
              id="email"
              type="text"
              v-model="form.username"
              placeholder="请输入用户名"
              required
          >
        </div>

        <div class="form-group">
          <label for="password">密码</label>
          <input
              id="password"
              type="password"
              v-model="form.password"
              placeholder="请输入密码"
              required
          >
        </div>

        <div class="form-options">
          <label class="remember-me">
            <input type="checkbox" v-model="form.remember">
            <span>记住我</span>
          </label>
          <a href="#" class="forgot-password">忘记密码？</a>
        </div>

        <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

        <button type="submit" class="login-button">登录</button>

        <div class="register-link">
          还没有账号？ <router-link to="/register">立即注册</router-link>
        </div>
      </form>
    </div>
  </div>
</template>


<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f7fa;
  padding: 1rem;
}

.login-card {
  background: white;
  border-radius: 12px;
  padding: 2.5rem;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 2rem;
}

.login-header h1 {
  color: #e74c3c;
  font-size: 2rem;
  margin-bottom: 0.5rem;
}

.login-header p {
  color: #666;
  font-size: 1.1rem;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-group label {
  color: #2c3e50;
  font-weight: 500;
}

.form-group input {
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 1rem;
  transition: border-color 0.2s;
}

.form-group input:focus {
  border-color: #e74c3c;
  outline: none;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.remember-me {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #666;
  cursor: pointer;
}

.forgot-password {
  color: #e74c3c;
  text-decoration: none;
  font-size: 0.9rem;
}

.forgot-password:hover {
  text-decoration: underline;
}

.login-button {
  background: #e74c3c;
  color: white;
  padding: 0.75rem;
  border: none;
  border-radius: 6px;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.login-button:hover {
  background: #c0392b;
}

.register-link {
  text-align: center;
  color: #666;
}

.register-link a {
  color: #e74c3c;
  text-decoration: none;
  font-weight: 500;
}

.register-link a:hover {
  text-decoration: underline;
}

.error-message {
  color: #e74c3c;
  text-align: center;
  font-size: 0.9rem;
  margin: 0;
}
</style>
