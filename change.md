现在项目里 Kafka 的用法总结

- Kafka 运行在 Docker 里（通过 Docker Compose 启动）
  
  - 配置文件：项目根目录 docker-compose.yml（service：kafka，端口：9092）
  - 启动 Kafka：docker compose up -d kafka
  - 停止 Kafka：docker compose down
  - 后端连接地址：localhost:9092（spring.kafka.bootstrap-servers=localhost:9092）

- 下单（checkout）时发消息： tomatomall.order.created
  
  - 触发点：用户调用 /api/cart/checkout 下单成功写库后
  - 做的事：异步发送一条“订单已创建/待支付”的事件（包含 orderId、userId、总金额、购物车项、地址等）
  - 代码位置： CartServiceImpl.checkout → publishOrderCreatedEvent
  - 消费者：收到后目前主要用于日志验证（ Kafka received ... order.created ）
    - 位置： OrderCreatedConsumer.onMessage
- 支付成功时发消息： tomatomall.order.paid 
  
  - 触发点：订单被置为 SUCCESS 后
  - 做的事：发送一条“订单已支付”的事件到 Kafka
  - 代码位置：
    - 生产事件： AlipayUtils.publishOrderPaidEvent
    - 模拟触发入口： OrdersController.mockPaid
- Kafka 消费 order.paid 后执行业务：结算冻结库存 + 清理购物车
  
  - 下单时会“冻结库存”： amount -= qty ， frozen += qty 
  - 支付成功后，Kafka 消费者会把 frozen -= qty （结算冻结库存），并删除订单-购物车关系与购物车项，避免残留/重复
  - 代码位置： OrderCreatedConsumer.onOrderPaid
