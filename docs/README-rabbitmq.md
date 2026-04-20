# RabbitMQ 在本项目中的用法与新业务接入清单

本文说明当前示例（订单创建 → 用户侧异步通知）里 RabbitMQ 的用法，以及**新增一条消息类业务**时，需要在各服务里补全哪些内容。

## 1. 当前架构（订单已创建事件）

- **交换机类型**：`TopicExchange`，名称 `order.exchange`（持久化、非自动删除）。
- **路由键**：`order.created`。
- **队列**：`user.order.notify`（由 user-service 声明并绑定到上述交换机与路由键）。
- **消息体**：发送端将 `OrderCreatedMessage` 用 **Jackson 序列化成 JSON 字符串** 再发送；消费端用 **String 入参** 接收，再反序列化为同名结构（避免默认 Java 序列化在跨服务、跨类加载器下不兼容）。

**谁负责声明什么**

| 组件 | order-service（生产者） | user-service（消费者） |
|------|-------------------------|-------------------------|
| Exchange | ✅ 声明 `order.exchange` | ✅ 同名 Exchange（启动时幂等创建） |
| Queue | ❌ | ✅ `user.order.notify` |
| Binding | ❌ | ✅ `order.exchange` + `order.created` → `user.order.notify` |
| 发送 / 监听 | `RabbitTemplate.convertAndSend` | `@RabbitListener` |

生产者的 Exchange 与消费者侧的 Queue/Binding **名称必须一致**；若只在一个服务里声明，需保证该服务先于依赖方启动，或统一在一处管理（本示例两边都声明 Exchange 是为了各服务可独立启动）。

---

## 2. 依赖与配置（凡要用 MQ 的服务都要具备）

### 2.1 Maven

在对应模块的 `pom.xml` 中增加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

### 2.2 `application.yml`

连接 RabbitMQ（与 `docker-compose` 中服务或本机一致）：

```yaml
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:127.0.0.1}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USER:guest}
    password: ${RABBITMQ_PASSWORD:guest}
```

---

## 3. 新增一条「消息类」业务时要加什么

以下按**生产者服务**和**消费者服务**分别列出；若一个服务既发又收，两边步骤合并即可。

### 3.1 生产者（发布事件）

1. **常量**：在 `.../config/RabbitConfig.java` 中定义（或扩展）  
   - Exchange 名、Routing Key；若新交换机类型不是 Topic，改用 `DirectExchange` / `FanoutExchange` 等并相应调整绑定。
2. **Bean**：若使用**新的 Exchange**，增加 `@Bean` 声明该 Exchange（与现有 `orderExchange()` 并列）。
3. **消息 DTO**：在 `.../messaging/` 下新增 `XxxMessage`（如 `record`），字段名与 JSON 约定一致，便于 Jackson 序列化。
4. **发送逻辑**：在合适的业务方法里注入 `RabbitTemplate`（及需要时的 `ObjectMapper`），调用  
   `rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, objectMapper.writeValueAsString(msg))`  
   或直接使用可序列化对象（若与消费者约定好且包结构共享，需另行约定；本仓库跨服务采用 **JSON 字符串 + 双方各自一份 DTO**）。
5. **异常**：序列化失败时的处理策略（记录日志、是否影响主流程等）在本服务内明确。

### 3.2 消费者（订阅处理）

1. **常量**：在 `RabbitConfig` 中与生产者对齐：同一 Exchange 名、Routing Key；**新增队列名**（如 `模块.业务.用途`）。
2. **Bean**：  
   - `Queue`（持久化等参数与业务一致）；  
   - `Binding`：将 Queue 绑到 Exchange，并指定 Routing Key（Topic 用 `with(...)`）。
3. **消息 DTO**：在 `.../messaging/` 下定义与 JSON 字段一致的 `XxxMessage`（可与生产者字段名保持相同，类名可相同但属不同模块）。
4. **监听器**：新建 `@Component`，使用 `@RabbitListener(queues = "队列名")`，方法参数建议与发送方式一致（本仓库为 `String` + `objectMapper.readValue`）。
5. **业务逻辑**：在 Listener 中调用 Service，注意**幂等**（消息可能重复投递）和**异常**（抛错可能导致重试或入死信，视配置而定）。

### 3.3 命名与约定（建议）

| 概念 | 示例（当前） | 说明 |
|------|----------------|------|
| Exchange | `order.exchange` | 可按领域命名，一个领域一个交换机常见 |
| Routing Key | `order.created` | Topic 可用层级：`order.created`、`order.cancelled` |
| Queue | `user.order.notify` | 建议体现**消费者边界**，避免多服务共用一个队列名 |

---

## 4. 本仓库中的代码索引

| 说明 | 路径 |
|------|------|
| 生产者 Exchange 与路由键 | `order-service/.../config/RabbitConfig.java` |
| 发送代码 | `order-service/.../service/OrderService.java` |
| 消息体 | `order-service/.../messaging/OrderCreatedMessage.java` |
| 消费者 Exchange / Queue / Binding | `user-service/.../config/RabbitConfig.java` |
| 监听与反序列化 | `user-service/.../messaging/OrderNotifyListener.java` |
| 消费者消息体 | `user-service/.../messaging/OrderCreatedMessage.java` |

---

## 5. 可选后续增强（未在本示例中实现）

- **可靠投递**：生产者确认（publisher confirm）、消费者手动 ACK、死信队列（DLX）。
- **跨服务 DTO 共享**：抽成独立 `api`/`common` 模块，减少两份 `OrderCreatedMessage` 漂移。
- **版本化**：消息内增加 `eventVersion` 或 `schema` 字段，便于演进。

按第 3 节清单逐项打勾，即可完成一条新的 RabbitMQ 业务接入。
