# cluster-microservices

工业级、轻量级的「真微服务」学习示例：**API 网关 + 服务注册 + 三个独立业务服务**，每服务独立数据库（MySQL），配合 **Redis** 缓存与 **RabbitMQ** 异步消息；基础设施用 **Docker Compose** 一键拉起。

## 架构一览

| 组件 | 作用 |
|------|------|
| **Spring Cloud Gateway** | 统一入口 `8080`，路由到各服务 |
| **Nacos** | 服务注册与发现（各服务通过服务名互相调用） |
| **user-service** `8081` | 用户；MySQL `user_db`；Redis 缓存用户查询；**消费**订单创建消息（模拟通知） |
| **product-service** `8082` | 商品；MySQL `product_db`；Redis 缓存列表/详情；提供扣减库存接口 |
| **order-service** `8083` | 订单；MySQL `order_db`；**OpenFeign** 调用户/商品；下单后 **发布** RabbitMQ 事件 |

数据流（下单）简述：

1. 客户端只访问网关 `POST /api/orders`。
2. 订单服务用 Feign 同步校验用户、读商品、调用商品服务扣库存、写本库订单。
3. 订单服务向交换机 `order.exchange` 发送路由键 `order.created` 的消息。
4. 用户服务队列 `user.order.notify` 绑定该路由键，异步打印「模拟通知」（体现 **异步解耦**）。

## 除 RabbitMQ / Redis / MySQL / Docker 外，常见「工业级」拼图

本仓库已包含：**注册中心（Nacos）**、**网关**、**声明式 HTTP（Feign）**、**负载均衡（`lb://`）**、**Flyway 迁移**、**Actuator 健康检查**。  
未纳入以保持轻量，但生产环境常会继续加：**统一配置中心**（仍可用 Nacos Config）、**分布式链路追踪**（Micrometer Tracing + Zipkin/Tempo）、**日志聚合**（ELK/Loki）、**限流熔断**（Sentinel）、**容器编排**（K8s）、**CI/CD** 等。

## 环境要求

- JDK 17+
- Maven 3.8+
- Docker Desktop（用于 Compose 起 MySQL / Redis / RabbitMQ / Nacos）
- **可选前端**：Node.js 18+（用于 Vue 3 演示页）

## 1. 启动基础设施

在仓库根目录：

```bash
docker compose up -d
```

等待 Nacos 控制台可访问：<http://127.0.0.1:8848/nacos>（默认账号密码 `nacos/nacos`）。  
RabbitMQ 管理界面：<http://127.0.0.1:15672>（`guest/guest`）。

MySQL：`127.0.0.1:3307`（映射容器内 3306），用户 `root`，密码 `root`；首次启动脚本会创建 `user_db`、`order_db`、`product_db`。

## 2. 编译

```bash
mvn clean package -DskipTests
```

## 3. 启动微服务（建议顺序）

在四个终端分别执行（或 IDE 运行主类）：

1. `user-service` → `com.cluster.user.UserApplication`
2. `product-service` → `com.cluster.product.ProductApplication`
3. `order-service` → `com.cluster.order.OrderApplication`
4. `gateway` → `com.cluster.gateway.GatewayApplication`

所有服务默认连接本机 `127.0.0.1` 上的 Nacos / MySQL / Redis / RabbitMQ（与 `docker-compose.yml` 端口一致）。

## 4. 调用示例（经网关）

创建用户：

```http
POST http://127.0.0.1:8080/api/users
Content-Type: application/json

{"username":"alice","email":"alice@example.com"}
```

查看商品列表：

```http
GET http://127.0.0.1:8080/api/products
```

下单（`userId` 用上一步返回的 id，`productId` 一般为 `1` 或 `2`，来自商品表种子数据）：

```http
POST http://127.0.0.1:8080/api/orders
Content-Type: application/json

{"userId":1,"productId":1,"quantity":2}
```

观察 **user-service** 控制台日志，应出现 `[异步] 模拟通知`；在 RabbitMQ 管理界面可看到队列与消息投递情况。

## 5. Vue 3 演示页（可选）

仓库内 `frontend` 为 **Vite + Vue 3 + Element Plus + Axios**：创建用户、查看商品、下单；HTTP 封装在 `frontend/src/api/http.js`。请求经开发服务器 **代理到网关 `8080`**（见 `frontend/vite.config.js`）。网关已配置 **CORS**，便于浏览器直接访问 API。

```bash
cd frontend
npm install
npm run dev
```

浏览器打开终端里提示的本地地址（一般为 <http://127.0.0.1:5173>）。需先按上文 **启动基础设施** 与 **四个 Java 服务**，否则页面会报错。页头右侧开关可切换 **浅色 / 深色**（写入 `localStorage` 键 `cluster_theme`；无记录时跟随系统偏好）。

## 6. 可选：把 JAR 打进 Docker

各模块已提供 `Dockerfile`，需在对应模块先 `mvn package` 再 `docker build`。若服务跑在容器内，需把环境变量中的 `127.0.0.1` 改为 Compose 服务名（如 `mysql`、`redis`、`rabbitmq`、`nacos`），并保证与 Nacos 注册 IP 可达（进阶：host 网络或统一 overlay 网络），此处不展开，避免新手被网络细节绊住。

## 端口汇总

| 服务 | 端口 |
|------|------|
| gateway | 8080 |
| Vue 演示页 (Vite dev) | 5173 |
| user-service | 8081 |
| product-service | 8082 |
| order-service | 8083 |
| MySQL | 3307 |
| Redis | 6380 |
| RabbitMQ AMQP | 5672 |
| RabbitMQ UI | 15672 |
| Nacos | 8848 |

## 已知改进方向

- 下单与扣库存可改为 **本地消息表 + 可靠投递** 或 **Seata 分布式事务**，按业务一致性要求选型。
- 网关可加 **鉴权**（JWT）、**限流**、**请求日志**。
- 为 Feign 增加熔断与重试策略，避免级联故障。

---

若你希望下一步把 **四个 Java 进程也放进 Compose** 并一键 `docker compose up` 跑全栈，可以说明，我可以按你当前 Docker 网络环境补一版可运行的 `docker-compose` 服务定义。
