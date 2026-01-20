<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Product } from '../../type'
import {getMyProducts} from "../../api/products";

// 模拟已购买的书籍数据
const purchasedProducts = ref<Product[]>([
  // {
  //   id: 1,
  //   title: '深入理解计算机系统',
  //   price: 139.00,
  //   description: '本书从程序员的视角详细阐述计算机系统的本质概念，并展示这些概念如何实实在在地影响应用程序的正确性、性能和实用性。',
  //   cover: 'https://img2.doubanio.com/view/subject/l/public/s29195878.jpg',
  //   stockpile: {
  //     id: 1,
  //     amount: 100
  //   }
  // },
  // {
  //   id: 2,
  //   title: '算法导论',
  //   price: 128.00,
  //   description: '本书全面介绍了计算机算法，是一本将严谨性和全面性融为一体深入探讨各类算法的著作。',
  //   cover: 'https://img1.doubanio.com/view/subject/l/public/s29107381.jpg',
  //   stockpile: {
  //     id: 2,
  //     amount: 50
  //   }
  // },
  // {
  //   id: 3,
  //   title: 'JavaScript高级程序设计',
  //   price: 99.00,
  //   description: '本书是JavaScript经典图书的新版。第4版全面、深入地介绍了JavaScript开发者必须掌握的前端开发技术。',
  //   cover: 'https://img2.doubanio.com/view/subject/l/public/s8958650.jpg',
  //   stockpile: {
  //     id: 3,
  //     amount: 200
  //   }
  // }
])
getPurchasedProducts()

function getPurchasedProducts() {
  getMyProducts().then(res => {
    if(res.data.code === '200') {
      purchasedProducts.value = res.data.data
    } else {
      ElMessage({
        message: res.data.msg,
        type: 'error',
        center: true,
      })
    }
  })
}

const searchQuery = ref('')
const sortBy = ref('name')

const filteredBooks = computed(() => {
  let result = [...purchasedProducts.value]
  
  // 搜索过滤
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    result = result.filter(book => 
      book.name.toLowerCase().includes(query)
    )
  }
  
  // 排序
  result.sort((a, b) => {
    if (sortBy.value === 'name') {
      const nameA = a?.name || ''
      const nameB = b?.name || ''
      return nameA.localeCompare(nameB)
    } else if (sortBy.value === 'price') {
      return a.price - b.price
    }
    return 0
  })
  
  return result
})

</script>

<template>
  <div class="my-books-container">
    <div class="header">
      <h1>我的书架</h1>
    </div>

    <div class="books-grid">
      <el-card v-for="book in filteredBooks" :key="book.id" class="book-card">
        <div class="book-cover">
          <el-image :src="book.cover" fit="cover" />
        </div>
        <div class="book-info">
          <h3>{{ book.title }}</h3>
          <p class="price">¥{{ book.price.toFixed(2) }}</p>
          <p class="description">{{ book.description }}</p>
        </div>
      </el-card>
    </div>

    <div v-if="filteredBooks.length === 0" class="empty-state">
      <el-empty description="暂无已购买的书籍" />
    </div>
  </div>
</template>

<style scoped>
.my-books-container {
  padding: 2rem;
  min-height: 100vh;
  background: #f5f7fa;
}

.header {
  margin-bottom: 2rem;
}

.header h1 {
  margin: 0 0 1rem 0;
  color: #2c3e50;
  font-size: 2rem;
}

.controls {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
}

.search-input {
  width: 300px;
}

.sort-select {
  width: 150px;
}

.books-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 2rem;
}

.book-card {
  transition: transform 0.3s ease;
}

.book-card:hover {
  transform: translateY(-5px);
}

.book-cover {
  height: 200px;
  overflow: hidden;
  border-radius: 8px;
  margin-bottom: 1rem;
}

.book-cover .el-image {
  width: 100%;
  height: 100%;
}

.book-info {
  padding: 1rem;
}

.book-info h3 {
  margin: 0 0 0.5rem 0;
  font-size: 1.2rem;
  color: #2c3e50;
}

.author {
  color: #666;
  margin: 0 0 0.5rem 0;
}

.price {
  color: #e74c3c;
  font-weight: bold;
  margin: 0 0 0.5rem 0;
}

.description {
  color: #666;
  font-size: 0.9rem;
  margin: 0 0 1rem 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

@media (max-width: 768px) {
  .my-books-container {
    padding: 1rem;
  }

  .controls {
    flex-direction: column;
  }

  .search-input,
  .sort-select {
    width: 100%;
  }

  .books-grid {
    grid-template-columns: 1fr;
  }
}
</style> 