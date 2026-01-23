<template>
  <div class="statistics-container">
    <h1>API调用统计</h1>
    
    <div class="filters">
      <button @click="fetchAllStatistics" class="btn btn-primary">
        查看所有接口统计
      </button>
      <div class="api-filter">
        <input 
          v-model="className" 
          placeholder="Controller名称" 
          class="form-input"
        />
        <input 
          v-model="methodName" 
          placeholder="方法名称" 
          class="form-input"
        />
        <button @click="fetchApiStatistics" class="btn btn-secondary">
          查看指定接口统计
        </button>
      </div>
    </div>
    
    <div v-if="loading" class="loading">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>
    
    <div v-else-if="error" class="error">
      {{ error }}
    </div>
    
    <div v-else-if="statistics" class="statistics-content">
      <div v-if="isAllStatistics" class="all-statistics">
        <h2>所有接口统计数据</h2>
        <div class="statistics-grid">
          <div 
            v-for="(stats, api) in statistics" 
            :key="api" 
            class="statistic-card"
          >
            <h3>{{ api }}</h3>
            <div class="statistic-item">
              <span class="label">调用次数:</span>
              <span class="value">{{ stats.count }}</span>
            </div>
            <div class="statistic-item">
              <span class="label">总响应时间:</span>
              <span class="value">{{ stats.totalTime }}ms</span>
            </div>
            <div class="statistic-item">
              <span class="label">平均响应时间:</span>
              <span class="value">{{ stats.avgTime.toFixed(2) }}ms</span>
            </div>
            <div class="statistic-item">
              <span class="label">最近调用时间:</span>
              <span class="value">{{ formatTime(stats.lastCall) }}</span>
            </div>
          </div>
        </div>
      </div>
      
      <div v-else class="single-statistics">
        <h2>{{ className }}:{{ methodName }} 统计数据</h2>
        <div class="statistic-detail">
          <div class="statistic-item">
            <span class="label">调用次数:</span>
            <span class="value">{{ statistics.count }}</span>
          </div>
          <div class="statistic-item">
            <span class="label">总响应时间:</span>
            <span class="value">{{ statistics.totalTime }}ms</span>
          </div>
          <div class="statistic-item">
            <span class="label">平均响应时间:</span>
            <span class="value">{{ statistics.avgTime.toFixed(2) }}ms</span>
          </div>
          <div class="statistic-item">
            <span class="label">最近调用时间:</span>
            <span class="value">{{ formatTime(statistics.lastCall) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';

interface Statistics {
  count: number;
  totalTime: number;
  avgTime: number;
  lastCall: number;
}

interface AllStatistics {
  [key: string]: Statistics;
}

const className = ref('');
const methodName = ref('');
const loading = ref(false);
const error = ref('');
const statistics = ref<AllStatistics | Statistics | null>(null);
const isAllStatistics = ref(false);

const fetchAllStatistics = async () => {
  loading.value = true;
  error.value = '';
  
  try {
    const response = await fetch('http://localhost:8080/api/statistics/all');
    if (!response.ok) {
      throw new Error('获取统计数据失败');
    }
    const data = await response.json();
    statistics.value = data.data;
    isAllStatistics.value = true;
  } catch (err) {
    error.value = err instanceof Error ? err.message : '未知错误';
  } finally {
    loading.value = false;
  }
};

const fetchApiStatistics = async () => {
  if (!className.value || !methodName.value) {
    error.value = '请输入Controller名称和方法名称';
    return;
  }
  
  loading.value = true;
  error.value = '';
  
  try {
    const response = await fetch(
      `http://localhost:8080/api/statistics/api?className=${className.value}&methodName=${methodName.value}`
    );
    if (!response.ok) {
      throw new Error('获取统计数据失败');
    }
    const data = await response.json();
    statistics.value = data.data;
    isAllStatistics.value = false;
  } catch (err) {
    error.value = err instanceof Error ? err.message : '未知错误';
  } finally {
    loading.value = false;
  }
};

const formatTime = (timestamp: number): string => {
  const date = new Date(timestamp);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
};
</script>

<style scoped>
.statistics-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  font-family: Arial, sans-serif;
}

h1 {
  text-align: center;
  color: #333;
  margin-bottom: 30px;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 30px;
  padding: 20px;
  background: #f5f5f5;
  border-radius: 8px;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  font-weight: bold;
  transition: background-color 0.3s;
}

.btn-primary {
  background-color: #007bff;
  color: white;
}

.btn-primary:hover {
  background-color: #0069d9;
}

.btn-secondary {
  background-color: #6c757d;
  color: white;
}

.btn-secondary:hover {
  background-color: #5a6268;
}

.api-filter {
  display: flex;
  gap: 10px;
  align-items: center;
}

.form-input {
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  width: 200px;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
}

.spinner {
  width: 50px;
  height: 50px;
  border: 5px solid #f3f3f3;
  border-top: 5px solid #007bff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error {
  background-color: #f8d7da;
  color: #721c24;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.statistics-content {
  margin-top: 30px;
}

h2 {
  color: #333;
  margin-bottom: 20px;
}

.all-statistics {
  margin-bottom: 40px;
}

.statistics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.statistic-card {
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s, box-shadow 0.3s;
}

.statistic-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.statistic-card h3 {
  color: #007bff;
  margin-bottom: 15px;
  font-size: 16px;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
}

.statistic-detail {
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.statistic-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.statistic-item:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.label {
  font-weight: 500;
  color: #666;
}

.value {
  font-weight: bold;
  color: #333;
}

@media (max-width: 768px) {
  .filters {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .api-filter {
    flex-direction: column;
    align-items: stretch;
  }
  
  .form-input {
    width: 100%;
  }
  
  .statistics-grid {
    grid-template-columns: 1fr;
  }
}
</style>
