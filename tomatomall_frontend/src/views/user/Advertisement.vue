<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import {router} from '../../router/index';
import { ElMessage } from 'element-plus'
import {createAd, delAd, getAd, updateAd} from "../../api/advertisement";
import {Advertisement} from "../../type"

const userRole = ref(sessionStorage.getItem("role"))
const isAdmin = computed(() => userRole.value === "ADMIN")

//获得广告
const ads = ref<Advertisement[]>([])
getAdvertisement()

function getAdvertisement() {
  getAd().then(res => {
    if (res.data.code === '200') {
      ads.value = res.data.data
    } else {
      ElMessage({
        message: res.data.msg,
        type: 'error',
        center: true,
      })
    }
  })
}

const dialogVisible = ref(false)
const editingAd = ref<Advertisement>()


function openAddDialog() {
  initEditingAd()
  dialogVisible.value = true
}

function openEditDialog(ad) {
  editingAd.value = { ...ad }
  dialogVisible.value = true
}

function saveAd() {
  if (editingAd.value.id) {
    updateAd(editingAd.value).then(res => {
      if (res.data.code === '200') {  //类型守卫，它检查 res.data 对象中是否存在名为 code 的属性
        ElMessage.success("更新成功")
      } else {
        ElMessage({
          message: res.data.msg,
          type: 'error',
          center: true,
        })
      }
    })
  } else {
    createAd(editingAd.value).then(res => {
      if (res.data.code === '200') {  //类型守卫，它检查 res.data 对象中是否存在名为 code 的属性
        ElMessage.success('广告添加成功')
      } else {
        ElMessage({
          message: res.data.msg,
          type: 'error',
          center: true,
        })
      }
    })
  }
  dialogVisible.value = false
  getAdvertisement()
}

function deleteAd(id) {
  delAd(Number(id)).then(res => {
      if (res.data.code === '200') {  //类型守卫，它检查 res.data 对象中是否存在名为 code 的属性
        ElMessage.success('广告删除成功')
      } else {
        ElMessage({
          message: res.data.msg,
          type: 'error',
          center: true,
        })
      }
  })
  getAdvertisement()
}

function goToProduct(productId) {
  router.push(`/products/${productId}`)
}

function initEditingAd() {
  editingAd.value = {
    id: '',
    title: '',
    content: '',
    imgUrl: '',
    productId: '',
  }
}

function handleCoverUploadSuccess(response) {
  // 假设后端返回上传后的图片 URL
  console.log(response.data)
  editingAd.value.imgUrl = response.data;
}

function beforeCoverUpload(file) {
  const isImage = file.type.startsWith('image/');
  const isLt2M = file.size / 1024 / 1024 < 2;

  if (!isImage) {
    this.$message.error('只能上传图片文件!');
  }
  if (!isLt2M) {
    this.$message.error('图片大小不能超过 2MB!');
  }
  return isImage && isLt2M;
}
</script>

<template>
  <div class="p-8 bg-gradient-to-r from-pink-50 via-purple-50 to-blue-50 min-h-screen rounded-xl shadow-lg">
    <div class="flex justify-between items-center mb-6">
      <h2 class="text-3xl font-bold text-purple-800 flex items-center gap-2">
        <i class="el-icon-picture-outline"></i> 推荐
      </h2>
      <el-button type="primary" size="large" icon="Plus" @click="openAddDialog" v-if="isAdmin">新增广告</el-button>
    </div>

    <el-table :data="ads" border stripe class="rounded-xl shadow-md" style="background-color: white">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="标题">
        <template #default="scope">
          <el-link type="primary" @click="goToProduct(Number(scope.row.productId))">
            {{ scope.row.title }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column prop="content" label="内容" />
      <el-table-column label="图片" width="120">
        <template #default="scope">
          <el-image
              :src="scope.row.imgUrl"
              style="width: 100px; height: 60px; border-radius: 8px"
              fit="cover"
          />
        </template>
      </el-table-column>
      <el-table-column prop="productId" label="商品ID" width="100" />
      <el-table-column label="操作" width="240" v-if="isAdmin">
        <template #default="scope">
          <div class="flex gap-2">
            <el-button size="small" type="primary" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteAd(scope.row.id)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑广告弹窗 -->
    <el-dialog v-model="dialogVisible" width="600px">
      <el-form :model="editingAd" label-width="100px" class="px-2">
        <el-form-item label="标题">
          <el-input v-model="editingAd.title" placeholder="请输入广告标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input type="textarea" v-model="editingAd.content" rows="3" placeholder="广告内容" />
        </el-form-item>
        <el-form-item label="封面图片">
          <el-upload
              class="upload-demo"
              action="http://localhost:8080/upload"
              :show-file-list="false"
              :on-success="handleCoverUploadSuccess"
              :before-upload="beforeCoverUpload"
          >
            <img v-if="editingAd.imgUrl" :src="editingAd.imgUrl" class="logo-image" />
            <el-button v-else type="primary">上传封面</el-button>
          </el-upload>
        </el-form-item>

        <el-form-item label="商品ID">
          <el-input-number v-model="editingAd.productId" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAd">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>


<style scoped>

.logo-image {
  width: 100px;        /* 或指定容器宽度 */
  height: 100px;       /* 你也可以用 auto 高度以保持比例 */
  object-fit: cover;   /* 保持填满但裁剪，多用于头像 */
  border-radius: 50%;  /* 可选：让头像变圆 */
  border: 1px solid #ccc;
}

h2 {
  animation: fadeInDown 0.8s ease;
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>

