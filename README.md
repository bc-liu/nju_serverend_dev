# TomatoMall 项目使用说明

## 1. 项目简介

TomatoMall 是一个基于 Spring Boot + Vue 的电商系统，包含完整的前后端功能，支持用户管理、商品管理、购物车、订单管理、支付等核心功能。

### 1.1 技术栈

| 分类 | 技术            | 版本   | 说明                                                             |
| ---- | --------------- | ------ | ---------------------------------------------------------------- |
| 后端 | Spring Boot     | 2.7.6  | 基础框架                                                         |
| 后端 | Spring Data JPA | -      | 数据访问                                                         |
| 后端 | Spring Security | -      | 安全框架                                                         |
| 后端 | Redis           | 7.0    | **缓存和令牌存储**（商品数据缓存、布隆过滤器、Redisson分布式锁） |
| 后端 | Redisson        | 3.23.4 | 分布式锁框架                                                     |
| 后端 | Kafka           | 3.9.1  | 消息队列                                                         |
| 后端 | MySQL           | 8.0    | 数据库                                                           |
| 前端 | Vue 3           | 3.5.13 | 前端框架                                                         |
| 前端 | Element Plus    | 2.9.7  | UI组件库                                                         |
| 前端 | Axios           | 1.8.4  | HTTP客户端                                                       |

## 2. 部署说明

### 2.1 环境要求

- Docker Desktop 20.10.0+ (Windows/Mac) 或 Docker Engine 20.10.0+ (Linux)
- Git 2.0+
- 至少 4GB 内存
- 至少 20GB 磁盘空间

### 2.2 快速部署

1. **克隆项目**

   ```bash
   git clone <项目地址>
   cd nju_serverend_dev
   ```

2. **启动服务**

   ```bash
   # 使用 Docker Compose 启动所有服务
   docker-compose up -d
   ```

   此命令会启动以下服务：
   - MySQL (端口 3306)
   - Redis (端口 6379)
   - Kafka (端口 9092)
   - 后端服务 (端口 8080)
   - 前端服务 (端口 80)

3. **初始化 Kafka 主题**

   ```bash
   # 进入 Kafka 容器
   docker exec -it kafka /bin/bash
   
   # 创建主题
   /opt/kafka/bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --topic tomatomall.order.created --partitions 1 --replication-factor 1
   /opt/kafka/bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --topic tomatomall.order.paid --partitions 1 --replication-factor 1
   
   # 退出容器
   exit
   ```

4. **访问系统**

   - 前端地址：`http://localhost`
   - 后端API地址：`http://localhost:8080`

### 2.3 服务管理

```bash
# 查看服务状态
docker-compose ps

# 停止服务
docker-compose stop

# 启动服务
docker-compose start

# 重启服务
docker-compose restart

# 停止并移除服务（保留数据）
docker-compose down

# 停止并移除服务（包括数据）
docker-compose down -v

# 查看服务日志
docker-compose logs <服务名>
# 例如：查看后端日志
docker-compose logs backend
```

### 2.4 环境变量配置

后端服务的环境变量配置位于 `docker-compose.yml` 文件中：

| 环境变量                       | 说明            | 默认值                                                                                                                            |
| ------------------------------ | --------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| SPRING_DATASOURCE_URL          | 数据库连接地址  | jdbc:mysql://mysql:3306/tomatomall?characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true |
| SPRING_DATASOURCE_USERNAME     | 数据库用户名    | root                                                                                                                              |
| SPRING_DATASOURCE_PASSWORD     | 数据库密码      | 123456                                                                                                                            |
| SPRING_REDIS_HOST              | Redis主机       | redis                                                                                                                             |
| SPRING_REDIS_PORT              | Redis端口       | 6379                                                                                                                              |
| SPRING_KAFKA_BOOTSTRAP_SERVERS | Kafka地址       | kafka:9092                                                                                                                        |
| SPRING_KAFKA_CONSUMER_GROUP_ID | Kafka消费者组ID | tomatomall-group                                                                                                                  |

## 3. 核心功能说明

### 3.1 用户管理

- 用户注册、登录、注销
- 用户信息查看和修改
- 密码修改
- 基于角色的权限控制

### 3.2 商品管理

- 商品列表查询
- 商品详情查看（**Redis缓存优化**）
- 商品添加、修改、删除（**延迟双删策略**）
- 库存管理

### 3.3 购物车

- 添加商品到购物车
- 修改购物车商品数量
- 删除购物车商品
- 购物车结算

### 3.4 订单管理

- 订单创建
- 订单支付（支付宝沙箱）
- 订单状态管理
- 订单历史查询

### 3.5 广告管理

- 广告列表查询
- 广告添加、修改、删除

### 3.6 统计功能

- API调用统计
- 系统运行状态监控

## 4. API接口说明

### 4.1 用户相关接口

#### 4.1.1 用户注册

- **请求路径**：`POST /api/accounts`
- **请求参数**：
  ```json
  {
    "username": "用户名",
    "password": "密码",
    "name": "姓名",
    "avatar": "头像URL",
    "role": "角色（ADMIN/USER）",
    "telephone": "电话",
    "email": "邮箱",
    "location": "地址"
  }
  ```
- **响应**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "注册成功"
  }
  ```

#### 4.1.2 用户登录

- **请求路径**：`POST /api/accounts/login`
- **请求参数**：
  ```json
  {
    "username": "用户名",
    "password": "密码"
  }
  ```
- **响应**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "JWT令牌"
  }
  ```

#### 4.1.3 获取用户信息

- **请求路径**：`GET /api/accounts/{username}`
- **请求参数**：路径参数 `username`（用户名）
- **响应**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "id": 1,
      "username": "用户名",
      "name": "姓名",
      "avatar": "头像URL",
      "role": "角色",
      "telephone": "电话",
      "email": "邮箱",
      "location": "地址"
    }
  }
  ```

#### 4.1.4 更新用户信息

- **请求路径**：`PUT /api/accounts`
- **请求头**：`Authorization: Bearer {token}`
- **请求参数**：
  ```json
  {
    "username": "用户名",
    "name": "姓名",
    "avatar": "头像URL",
    "telephone": "电话",
    "email": "邮箱",
    "location": "地址"
  }
  ```
- **响应**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "更新成功"
  }
  ```

#### 4.1.5 修改密码

- **请求路径**：`PUT /api/accounts/updatePassword`
- **请求头**：`Authorization: Bearer {token}`
- **请求参数**：
  ```json
  {
    "oldPassword": "旧密码",
    "newPassword": "新密码"
  }
  ```
- **响应**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "更新密码成功"
  }
  ```

### 4.2 商品相关接口

#### 4.2.1 获取商品列表

- **请求路径**：`GET /api/products`
- **响应**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": [
      {
        "id": 1,
        "name": "商品名称",
        "price": 99.99,
        "description": "商品描述",
        "imageUrl": "商品图片URL",
        "stockpile": {
          "amount": 100
        }
      }
    ]
  }
  ```

#### 4.2.2 获取商品详情

- **请求路径**：`GET /api/products/{id}`
- **请求参数**：路径参数 `id`（商品ID）
- **响应**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "id": 1,
      "name": "商品名称",
      "price": 99.99,
      "description": "商品描述",
      "imageUrl": "商品图片URL",
      "stockpile": {
        "amount": 100
      }
    }
  }
  ```

#### 4.2.3 添加商品

- **请求路径**：`POST /api/products`
- **请求头**：`Authorization: Bearer {token}`
- **请求参数**：
  ```json
  {
    "name": "商品名称",
    "price": 99.99,
    "description": "商品描述",
    "imageUrl": "商品图片URL",
    "stockpile": {
      "amount": 100
    }
  }
  ```
- **响应**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "id": 1,
      "name": "商品名称",
      "price": 99.99,
      "description": "商品描述",
      "imageUrl": "商品图片URL",
      "stockpile": {
        "amount": 100
      }
    }
  }
  ```

#### 4.2.4 更新商品

- **请求路径**：`PUT /api/products`
- **请求头**：`Authorization: Bearer {token}`
- **请求参数**：
  ```json
  {
    "id": 1,
    "name": "商品名称",
    "price": 99.99,
    "description": "商品描述",
    "imageUrl": "商品图片URL"
  }
  ```
- **响应**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "更新成功"
  }
  ```

#### 4.2.5 删除商品

- **请求路径**：`DELETE /api/products/{id}`
- **请求头**：`Authorization: Bearer {token}`
- **请求参数**：路径参数 `id`（商品ID）
- **响应**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "删除成功"
  }
  ```

### 4.3 购物车相关接口

#### 4.3.1 添加商品到购物车

- **请求路径**：`POST /api/cart`
- **请求头**：`Authorization: Bearer {token}`
- **请求参数**：
  - `productId`：商品ID
  - `quantity`：数量
- **响应**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "id": 1,
      "productId": 1,
      "quantity": 2,
      "product": {
        "id": 1,
        "name": "商品名称",
        "price": 99.99
      }
    }
  }
  ```

#### 4.3.2 删除购物车商品

- **请求路径**：`DELETE /api/cart/{cartItemId}`
- **请求头**：`Authorization: Bearer {token}`
- **请求参数**：路径参数 `cartItemId`（购物车项ID）
- **响应**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "删除成功"
  }
  ```

#### 4.3.3 修改购物车商品数量

- **请求路径**：`PATCH /api/cart/{cartItemId}`
- **请求头**：`Authorization: Bearer {token}`
- **请求参数**：
  ```json
  {
    "quantity": 3
  }
  ```
- **响应**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "修改数量成功"
  }
  ```

#### 4.3.4 获取购物车列表

- **请求路径**：`GET /api/cart`
- **请求头**：`Authorization: Bearer {token}`
- **响应**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "carts": [
        {
          "id": 1,
          "productId": 1,
          "quantity": 2,
          "product": {
            "id": 1,
            "name": "商品名称",
            "price": 99.99
          }
        }
      ],
      "totalAmount": 199.98,
      "total": 2
    }
  }
  ```

#### 4.3.5 购物车结算

- **请求路径**：`POST /api/cart/checkout`
- **请求头**：`Authorization: Bearer {token}`
- **请求参数**：
  - `cartItemId`：购物车项ID列表
  - `shoppingAddress`：收货地址
  - `paymentMethod`：支付方式
- **响应**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "orderId": 1,
      "totalAmount": 199.98,
      "status": "PENDING",
      "createTime": "2026-01-23T12:00:00"
    }
  }
  ```

### 4.4 订单相关接口

#### 4.4.1 订单支付

- **请求路径**：`POST /api/orders/{orderId}/pay`
- **请求参数**：路径参数 `orderId`（订单ID）
- **响应**：HTML表单（跳转到支付宝支付页面）

#### 4.4.2 获取待处理订单

- **请求路径**：`GET /api/orders/pendingOrders`
- **请求头**：`Authorization: Bearer {token}`
- **响应**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": [
      {
        "orderId": 1,
        "totalAmount": 199.98,
        "status": "PENDING",
        "createTime": "2026-01-23T12:00:00"
      }
    ]
  }
  ```

#### 4.4.3 取消订单

- **请求路径**：`POST /api/orders/{orderId}/cancel`
- **请求头**：`Authorization: Bearer {token}`
- **请求参数**：路径参数 `orderId`（订单ID）
- **响应**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "取消成功"
  }
  ```

### 4.5 广告相关接口

#### 4.5.1 获取广告列表

- **请求路径**：`GET /api/advertisements`
- **响应**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": [
      {
        "id": 1,
        "title": "广告标题",
        "content": "广告内容",
        "imgUrl": "广告图片URL",
        "productId": 1
      }
    ]
  }
  ```

#### 4.5.2 创建广告

- **请求路径**：`POST /api/advertisements`
- **请求头**：`Authorization: Bearer {token}`
- **请求参数**：
  ```json
  {
    "title": "广告标题",
    "content": "广告内容",
    "imgUrl": "广告图片URL",
    "productId": 1
  }
  ```
- **响应**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "id": 1,
      "title": "广告标题",
      "content": "广告内容",
      "imgUrl": "广告图片URL",
      "productId": 1
    }
  }
  ```

#### 4.5.3 更新广告

- **请求路径**：`PUT /api/advertisements`
- **请求头**：`Authorization: Bearer {token}`
- **请求参数**：
  ```json
  {
    "id": 1,
    "title": "广告标题",
    "content": "广告内容",
    "imgUrl": "广告图片URL",
    "productId": 1
  }
  ```
- **响应**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "更新成功"
  }
  ```

#### 4.5.4 删除广告

- **请求路径**：`DELETE /api/advertisements/{id}`
- **请求头**：`Authorization: Bearer {token}`
- **请求参数**：路径参数 `id`（广告ID）
- **响应**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "删除成功"
  }
  ```

### 4.6 统计相关接口

#### 4.6.1 获取API调用统计

- **请求路径**：`GET /api/statistics`
- **响应**：
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "AccountController:getUser": {
        "count": "10",
        "totalTime": "1000",
        "avgTime": 100,
        "lastCall": "1674441600000"
      }
    }
  }
  ```

## 5. 系统架构

### 5.1 后端架构

- **控制器层**：处理HTTP请求，参数验证，返回响应
- **服务层**：实现业务逻辑
- **数据访问层**：通过Spring Data JPA访问数据库
- **实体层**：定义数据库表结构
- **工具层**：提供通用功能
- **配置层**：系统配置
- **切面**：API调用统计
- **异常处理**：全局异常处理

### 5.2 前端架构

- **视图层**：Vue组件
- **路由层**：Vue Router
- **API层**：Axios请求封装
- **工具层**：通用工具函数

### 5.3 数据流向

1. **用户请求** → 前端API调用 → 后端控制器 → 服务层 → 数据访问层 → 数据库
2. **响应** → 数据库 → 数据访问层 → 服务层 → 控制器 → 前端API响应 → 用户界面
3. **认证流程**：用户登录 → 生成JWT令牌 → 存储到Redis → 后续请求携带令牌 → 验证令牌 → 授权访问
4. **消息流程**：订单创建 → 发送Kafka消息 → 消费者处理消息 → 更新库存

### 5.4 Redis缓存优化架构

#### 5.4.1 商品数据缓存策略

- **缓存键**：`product:{id}`
- **缓存时间**：30-40分钟随机过期时间
- **缓存空值**：防止缓存穿透，缓存不存在的商品ID

#### 5.4.2 防缓存击穿（Cache Breakdown）

- **Redisson分布式锁**：使用Redisson的RLock实现高性能分布式锁
- **超时与快速失败**：锁等待超时500ms，超时后快速失败返回null，避免线程长时间阻塞
- **锁持有时间**：锁自动释放时间10秒，防止死锁
- **双重检查**：获取锁后再次检查缓存，防止重复加载
- **Pub/Sub机制**：Redisson内部基于Pub/Sub实现高效唤醒，不再盲目sleep
- **自动管理**：Redisson自动管理锁释放，确保锁一定会被释放

#### 5.4.3 防缓存穿透（Cache Penetration）

- **布隆过滤器**：使用Redis BitMap实现，过滤不存在的商品ID
- **空值缓存**：缓存查询结果为空的请求，设置较短过期时间

#### 5.4.4 延迟双删策略（Delayed Double Delete）

- **第一次删除**：在数据库更新前删除缓存
- **数据库更新**：执行实际的数据库操作
- **第二次删除**：延迟1秒后再次删除缓存，确保数据一致性

#### 5.4.5 缓存一致性保证

- **写操作同步**：所有商品创建、更新、删除操作同步更新缓存
- **布隆过滤器维护**：新增商品时自动更新布隆过滤器
- **初始化加载**：系统启动时自动初始化布隆过滤器

#### 5.4.6 Redisson分布式锁优化

**优化思路**：
- 引入Redisson的RLock，移除本地锁，完全信任分布式锁的互斥性
- 使用`RLock.tryLock(waitTime, leaseTime, unit)`指定最大等待时间和锁持有时间
- 设置合理的等待超时（500ms），超时后快速失败，避免线程长时间阻塞

**技术优势**：
1. **高性能**：Redisson内部基于Pub/Sub机制实现高效唤醒，减少CPU空转
2. **可靠性**：锁自动续期（Watchdog机制），防止业务执行时间过长导致锁提前释放
3. **易用性**：API简洁，自动管理锁释放，无需手动删除锁键
4. **可扩展**：支持集群模式、哨兵模式等多种Redis部署方式

**配置参数**：
- 锁等待超时：500ms
- 锁持有时间：10秒
- 锁前缀：`lock:product:`

## 6. 安全说明

### 6.1 认证与授权

- 使用JWT进行身份认证
- 基于角色的权限控制
- 密码使用BCrypt加密存储
- Redis存储令牌，支持令牌过期和失效

### 6.2 数据安全

- 数据库连接使用SSL（生产环境）
- 敏感信息使用环境变量配置
- API接口防止SQL注入和XSS攻击
- 上传文件安全验证

### 6.3 网络安全

- 跨域请求使用CORS配置
- API请求头包含token验证
- 生产环境建议使用HTTPS

## 7. 常见问题与解决方案

### 7.1 服务启动失败

**症状**：`docker-compose up -d` 后服务状态为 `Exit`

**解决方案**：
1. 查看服务日志：`docker-compose logs <服务名>`
2. 检查端口是否被占用
3. 检查环境变量配置
4. 检查依赖服务是否正常启动

### 7.2 数据库连接失败

**症状**：后端服务日志显示 "Connection to MySQL failed"

**解决方案**：
1. 检查MySQL服务是否启动
2. 检查数据库连接参数
3. 检查数据库是否存在
4. 检查网络连接

### 7.3 Kafka消息不消费

**症状**：订单创建后库存未更新

**解决方案**：
1. 检查Kafka服务是否启动
2. 检查主题是否创建
3. 检查消费者组配置
4. 查看Kafka日志

### 7.4 前端访问后端API失败

**症状**：前端控制台显示 "401 Unauthorized" 或 "403 Forbidden"

**解决方案**：
1. 检查是否已登录并获取令牌
2. 检查令牌是否过期
3. 检查用户权限
4. 检查跨域配置

## 8. 开发与扩展

### 8.1 开发环境设置

1. **后端开发**：
   - 使用IntelliJ IDEA或Eclipse
   - 导入Maven项目
   - 配置本地数据库和Redis

2. **前端开发**：
   - 使用VS Code
   - 运行 `npm install` 安装依赖
   - 运行 `npm run dev` 启动开发服务器

### 8.2 代码扩展

1. **添加新功能**：
   - 遵循现有代码结构
   - 控制器 → 服务 → 数据访问
   - 添加对应的前端组件

2. **数据库扩展**：
   - 创建新的实体类
   - 生成数据库表结构
   - 添加Repository接口

3. **API扩展**：
   - 遵循RESTful风格
   - 添加适当的权限控制
   - 更新API文档

### 8.3 性能优化

1. **数据库优化**：
   - 添加适当的索引
   - 优化SQL查询
   - 使用缓存减少数据库访问

2. **Redis优化**：
   - 合理设计键名
   - 设置适当的过期时间
   - 使用Pipeline批量操作

3. **代码优化**：
   - 减少重复代码
   - 使用异步处理
   - 优化算法复杂度

## 9. 部署到生产环境

### 9.1 环境准备

- **服务器**：至少2核4GB内存
- **操作系统**：Linux（推荐Ubuntu 20.04+）
- **Docker**：Docker Engine 20.10.0+
- **网络**：开放必要的端口

### 9.2 配置调整

1. **修改docker-compose.yml**：
   - 更新环境变量为生产配置
   - 添加域名配置
   - 调整资源限制

2. **数据库配置**：
   - 使用生产级数据库
   - 配置数据备份
   - 设置强密码

3. **安全配置**：
   - 启用SSL
   - 配置防火墙
   - 设置日志监控

### 9.3 部署流程

1. **克隆代码**：
   ```bash
   git clone <项目地址>
   cd nju_serverend_dev
   ```

2. **构建镜像**：
   ```bash
   docker-compose build
   ```

3. **启动服务**：
   ```bash
   docker-compose up -d
   ```

4. **初始化数据**：
   - 创建Kafka主题
   - 初始化系统数据

5. **验证服务**：
   - 检查服务状态
   - 测试API接口
   - 监控系统运行

## 10. 版本历史

| 版本   | 日期       | 变更内容 |
| ------ | ---------- | -------- |
| v1.0.0 | 2026-01-23 | 初始版本 |

## 11. 联系与支持

- **项目地址**：<项目地址>
- **文档地址**：<文档地址>
- **问题反馈**：<问题反馈地址>

---

**© 2026 TomatoMall 团队**
