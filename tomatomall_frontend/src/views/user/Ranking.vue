<script setup lang="ts">
import { ref } from 'vue'
import { Product } from '../../type'

// 模拟排行榜数据
const rankingList = ref<Product[]>([
  {
    id: 1,
    name: '深入理解计算机系统',
    author: 'Randal E. Bryant',
    price: 139.00,
    description: '本书从程序员的视角详细阐述计算机系统的本质概念，并展示这些概念如何实实在在地影响应用程序的正确性、性能和实用性。',
    cover: 'https://img2.doubanio.com/view/subject/l/public/s29195878.jpg',
    category: '计算机',
    stockpile: {
      id: 1,
      amount: 100
    },
    sales: 1250
  },
  {
    id: 2,
    name: '算法导论',
    author: 'Thomas H. Cormen',
    price: 128.00,
    description: '本书全面介绍了计算机算法，是一本将严谨性和全面性融为一体深入探讨各类算法的著作。',
    cover: 'https://img1.doubanio.com/view/subject/l/public/s29107381.jpg',
    category: '计算机',
    stockpile: {
      id: 2,
      amount: 50
    },
    sales: 980
  },
  {
    id: 3,
    name: 'JavaScript高级程序设计',
    author: 'Nicholas C. Zakas',
    price: 99.00,
    description: '本书是JavaScript经典图书的新版。第4版全面、深入地介绍了JavaScript开发者必须掌握的前端开发技术。',
    cover: 'https://img2.doubanio.com/view/subject/l/public/s8958650.jpg',
    category: '计算机',
    stockpile: {
      id: 3,
      amount: 200
    },
    sales: 856
  },
  {
    id: 4,
    name: '设计模式',
    author: 'Erich Gamma',
    price: 89.00,
    description: '本书是引导读者走出软件设计迷宫的指路明灯，凝聚了软件开发界几十年设计经验的结晶。',
    cover: 'https://img1.doubanio.com/view/subject/l/public/s1074361.jpg',
    category: '计算机',
    stockpile: {
      id: 4,
      amount: 150
    },
    sales: 720
  },
  {
    id: 5,
    name: '代码大全',
    author: 'Steve McConnell',
    price: 128.00,
    description: '本书是著名IT畅销书作者史蒂夫·迈克康奈尔11年前的经典著作的全新演绎。',
    cover: 'https://img2.doubanio.com/view/subject/l/public/s1495029.jpg',
    category: '计算机',
    stockpile: {
      id: 5,
      amount: 180
    },
    sales: 650
  }
])

// 计算排名
const getRankClass = (index: number) => {
  switch (index) {
    case 0:
      return 'rank-first'
    case 1:
      return 'rank-second'
    case 2:
      return 'rank-third'
    default:
      return ''
  }
}
</script>

<template>
  <div class="ranking-container">
    <div class="ranking-header">
      <h1>📚 畅销书籍排行榜</h1>
      <p class="subtitle">根据购买次数排序</p>
    </div>

    <div class="ranking-list">
      <div v-for="(book, index) in rankingList" :key="book.id" class="ranking-item">
        <div class="rank-number" :class="getRankClass(index)">
          {{ index + 1 }}
        </div>
        <div class="book-info">
          <div class="book-cover">
            <img :src="book.cover" :alt="book.name">
          </div>
          <div class="book-details">
            <h3>{{ book.name }}</h3>
            <p class="author">作者：{{ book.author }}</p>
            <p class="description">{{ book.description }}</p>
            <div class="book-meta">
              <span class="price">¥{{ book.price.toFixed(2) }}</span>
              <span class="sales">销量：{{ book.sales }}本</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ranking-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);
}

.ranking-header {
  text-align: center;
  margin-bottom: 3rem;
}

.ranking-header h1 {
  font-size: 2.5rem;
  color: #2c3e50;
  margin: 0;
}

.subtitle {
  color: #666;
  margin-top: 0.5rem;
}

.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.ranking-item {
  display: flex;
  align-items: center;
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease;
}

.ranking-item:hover {
  transform: translateY(-5px);
}

.rank-number {
  font-size: 2rem;
  font-weight: bold;
  color: #666;
  width: 60px;
  text-align: center;
}

.rank-first {
  color: #f59e0b;
}

.rank-second {
  color: #6b7280;
}

.rank-third {
  color: #b45309;
}

.book-info {
  display: flex;
  gap: 2rem;
  flex: 1;
}

.book-cover {
  width: 120px;
  height: 160px;
  overflow: hidden;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.book-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.book-details {
  flex: 1;
}

.book-details h3 {
  margin: 0 0 0.5rem 0;
  font-size: 1.5rem;
  color: #2c3e50;
}

.author {
  color: #666;
  margin: 0 0 1rem 0;
}

.description {
  color: #666;
  margin: 0 0 1rem 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.book-meta {
  display: flex;
  gap: 2rem;
  align-items: center;
}

.price {
  color: #e74c3c;
  font-size: 1.2rem;
  font-weight: bold;
}

.sales {
  color: #666;
}

@media (max-width: 768px) {
  .ranking-container {
    padding: 1rem;
  }

  .book-info {
    flex-direction: column;
    gap: 1rem;
  }

  .book-cover {
    width: 100%;
    height: 200px;
  }

  .book-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }
}
</style> 