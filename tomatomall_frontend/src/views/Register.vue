<template>

  <div class="register-container">
    <div class="register-card">
      <div class="register-header">
        <h1>🍅 番茄书店</h1>
        <p>创建新账号</p>
      </div>
      <!-- 注册表单 -->
      <el-form :model="form" :rules="rules">
        <el-row>
          <el-col :span="8">
            <el-form-item>
              <label for="identity">身份</label>
              <el-select id="identity"
                         v-model="form.role"
                         placeholder="请选择"
                         style="width: 100%;"
              >
                <el-option value="USER" label="顾客"/>
                <el-option value="ADMIN" label="管理员"/>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>

        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请确认密码" />
        </el-form-item>

        <el-form-item label="电话" prop="telephone">
          <el-input v-model="form.telephone" type="text" placeholder="请输入电话" />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" type="text" placeholder="请输入邮箱" />
        </el-form-item>

        <!-- 上传商店Logo -->
        <el-form-item prop="avatar">
          <div class="logo-upload">
            <el-upload
                class="upload-demo"
                action="http://localhost:8080/upload"
                :on-success="handleLogoSuccess"
                :before-upload="beforeLogoUpload"
                :file-list="fileList"
                :show-file-list="false"
            >
              <el-button type="primary" icon="el-icon-upload2">上传头像</el-button>
            </el-upload>
            <div v-if="form.avatar" class="logo-preview">
              <img :src="form.avatar" alt="用户头像" class="logo-image" />
            </div>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleRegister" style="width: 100%; background-color: #e44d26; border-color: #e44d26;">
            注册
          </el-button>
        </el-form-item>

        <el-form-item style="text-align: center;">
          <el-link href="#/login" style="color: #888;">已经有账号？去登录</el-link>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElForm, ElFormItem, ElInput, ElButton, ElLink, ElRow, ElCol, ElCard, ElMessage } from 'element-plus';
import {userRegister} from "../api/user.ts";
import {router} from "../router/index.ts";

// 注册表单的响应式数据
const form = ref({
  username: '',
  password: '',
  confirmPassword: '',
  name: '',
  avatar: '',
  role: '',
  telephone: '',
  email: '',
  location: '',
});

// 表单验证规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: (rule: any, value: string, callback: any) => {
        if (value !== form.value.password) {
          callback(new Error('两次密码输入不一致'));
        } else {
          callback();
        }
      }, trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  avatar: [
    { required: false } // 头像通常不做强制要求
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ],
  telephone: [
    { required: false, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  email: [
    { required: false, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
};


// 注册处理函数
function handleRegister() {
  userRegister(form.value).then(res => {
    if (res.data.code === '200') {  //类型守卫，它检查 res.data 对象中是否存在名为 code 的属性
      ElMessage({
        message: "注册成功！请登录账号",
        type: 'success',
        center: true,
      })
      router.push({path: "/login"})
    } else {
      ElMessage({
        message: res.data.msg,
        type: 'error',
        center: true,
      })
    }
  })
}


const fileList = ref([]);

const beforeLogoUpload = (file) => {
  const isImage = file.type.startsWith('image/');
  if (!isImage) {
    ElMessage.error('只能上传图片文件');
  }
  return isImage;
};

const handleLogoSuccess = (response, file) => {
  form.value.avatar = response.data;
  fileList.value = [file];
};



</script>

<style scoped>

.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f7fa;
  padding: 1rem;
}

.register-card {
  background: white;
  border-radius: 12px;
  padding: 2.5rem;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
}

.register-header {
  text-align: center;
  margin-bottom: 2rem;
}

.register-header h1 {
  color: #e74c3c;
  font-size: 2rem;
  margin-bottom: 0.5rem;
}

.register-header p {
  color: #666;
  font-size: 1.1rem;
}

/* 注册页面标题和按钮样式 */
h1 {
  font-size: 48px;
  font-weight: bold;
}

h2 {
  font-family: 'Arial', sans-serif;
  color: #e44d26;
}

.logo-image {
  width: 100px;        /* 或指定容器宽度 */
  height: 100px;       /* 你也可以用 auto 高度以保持比例 */
  object-fit: cover;   /* 保持填满但裁剪，多用于头像 */
  border-radius: 50%;  /* 可选：让头像变圆 */
  border: 1px solid #ccc;
}
</style>
