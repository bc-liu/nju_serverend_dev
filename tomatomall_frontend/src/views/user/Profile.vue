<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { User } from '../../type'
import {updatePassword, userInfo, userInfoUpdate} from "../../api/user";
import {router} from "../../router/index";

const user = ref<User>()
getUserInfo();

//在加载界面前，先获得用户信息
function getUserInfo() {
  userInfo(sessionStorage.getItem("username")).then(res => {
    user.value = res.data.data;
  })
}

const isEditing = ref(false)
const editedUser = ref({ ...user.value })

// 密码修改相关
const showPasswordDialog = ref(false)
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== newPassword.value) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

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
    editedUser.value = { ...user.value }
  }
}
const handleCancel = () => {
  editedUser.value = { ...user.value }
  isEditing.value = false
}

const handleAvatarSuccess = (response) => {
  user.value.avatar = response.data
  ElMessage.success('头像上传成功')
}

const beforeAvatarUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!')
  }
  return isImage && isLt2M
}

const handleChangePassword = () => {
  showPasswordDialog.value = true
}

const handlePasswordSubmit = () => {
  if (newPassword.value !== confirmPassword.value) {
    ElMessage.error('两次输入的新密码不一致')
  } else {
    // 这里应该调用后端API保存新密码
    updatePassword({
      oldPassword: oldPassword.value,
      newPassword: newPassword.value,
    }).then(res => {
      if(res.data.code === '200') {
        ElMessage.success('密码修改成功')
        router.push('/login')
      } else {
        ElMessage({
          message: res.data.msg,
          type: 'error',
          center: true,
        })
      }
    })
  }

  showPasswordDialog.value = false
  // 清空表单
  oldPassword.value = ''
  newPassword.value = ''
  confirmPassword.value = ''
}
</script>

<template>
  <div class="profile-container">
    <div class="profile-card">
      <div class="profile-header">
        <div class="avatar-section">
          <el-upload
            class="avatar-uploader"
            action="http://localhost:8080/upload"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            :before-upload="beforeAvatarUpload"
          >
            <img :src="user.avatar" class="avatar" />
            <div class="avatar-hover">
              <el-icon><Camera /></el-icon>
              <span>更换头像</span>
            </div>
          </el-upload>
        </div>
        <h2>{{ user.name }}</h2>
        <p class="role-tag">{{ user.role === 'ADMIN' ? '管理员' : '用户' }}</p>
      </div>

      <div class="profile-content">
        <div class="action-bar">
          <el-button type="primary" @click="isEditing = true" v-if="!isEditing">
            编辑资料
          </el-button>
          <div v-else class="edit-actions">
            <el-button type="success" @click="handleSave">保存</el-button>
            <el-button @click="handleCancel">取消</el-button>
          </div>
        </div>

        <div class="info-section">
          <div class="info-item">
            <span class="label">用户名</span>
            <span class="value">{{ user.username }}</span>
          </div>
          <div class="info-item">
            <span class="label">密码</span>
            <div class="password-section">
              <span class="value">******</span>
              <el-button type="primary" link @click="handleChangePassword">
                修改密码
              </el-button>
            </div>
          </div>
          <div class="info-item">
            <span class="label">邮箱</span>
            <el-input
              v-if="isEditing"
              v-model="editedUser.email"
              type="email"
              placeholder="请输入邮箱"
            />
            <span v-else class="value">{{ user.email }}</span>
          </div>
          <div class="info-item">
            <span class="label">电话</span>
            <el-input
              v-if="isEditing"
              v-model="editedUser.telephone"
              placeholder="请输入电话"
            />
            <span v-else class="value">{{ user.telephone }}</span>
          </div>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="showPasswordDialog"
      title="修改密码"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form :model="{ oldPassword, newPassword, confirmPassword }" :rules="passwordRules">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            v-model="oldPassword"
            type="password"
            placeholder="请输入原密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showPasswordDialog = false">取消</el-button>
          <el-button type="primary" @click="handlePasswordSubmit">
            确认修改
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.profile-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);
  padding: 2rem;
}

.profile-card {
  max-width: 800px;
  margin: 0 auto;
  background: white;
  border-radius: 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.profile-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 3rem 2rem;
  text-align: center;
  color: white;
}

.avatar-section {
  margin-bottom: 1.5rem;
}

.avatar-uploader {
  position: relative;
  display: inline-block;
}

.avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  border: 4px solid white;
  object-fit: cover;
}

.avatar-hover {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: white;
  opacity: 0;
  transition: opacity 0.3s;
  cursor: pointer;
}

.avatar-uploader:hover .avatar-hover {
  opacity: 1;
}

.profile-header h2 {
  margin: 0;
  font-size: 1.8rem;
  font-weight: 600;
}

.role-tag {
  display: inline-block;
  background: rgba(255, 255, 255, 0.2);
  padding: 0.3rem 1rem;
  border-radius: 20px;
  margin-top: 0.5rem;
  font-size: 0.9rem;
}

.profile-content {
  padding: 2rem;
}

.action-bar {
  margin-bottom: 2rem;
  text-align: right;
}

.edit-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
}

.info-section {
  display: grid;
  gap: 1.5rem;
}

.info-item {
  display: grid;
  grid-template-columns: 120px 1fr;
  align-items: center;
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 8px;
}

.label {
  color: #666;
  font-weight: 500;
}

.value {
  color: #2c3e50;
}

.password-section {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
}

@media (max-width: 768px) {
  .profile-container {
    padding: 1rem;
  }

  .info-item {
    grid-template-columns: 1fr;
    gap: 0.5rem;
  }
}
</style> 