# 本地生活点评平台（hm-dianping）

基于 **Spring Boot 3 + Redis + RabbitMQ** 的本地生活点评类后端服务，涵盖商铺点评、达人探店、优惠券秒杀、关注与 Feed 流等核心业务，重点实践了 **Redis 高并发缓存方案** 与 **消息队列异步削峰**。

---

## 📌 技术栈

| 分类 | 技术 |
|------|------|
| 语言 / 框架 | Java 17、Spring Boot 3.5.15、Spring MVC |
| 持久层 | MyBatis-Plus 3.5.6、MySQL 8（mysql-connector-j 9.3） |
| 缓存 | Redis、Spring Data Redis、Redisson 3.52、commons-pool2 |
| 消息队列 | RabbitMQ（Spring AMQP） |
| 工具 | Lombok、Hutool 5.8、AOP（aspectjweaver） |
| 构建 | Maven、JDK 17 |

---

## 🏗️ 项目结构

```
src/main/java/com/hmdp/
├── controller/     # 9 个控制器（商铺 / 点评 / 关注 / 优惠券 / 秒杀 / 用户 / 上传）
├── service/        # 业务接口 + impl（含 VoucherOrderConsumer 消息消费者）
├── mapper/         # MyBatis-Plus Mapper（10 个）
├── entity/         # 实体类（Shop / Blog / Voucher / SeckillVoucher / Follow ...）
├── dto/            # 数据传输对象（Result / ScrollResult / UserDTO ...）
└── config/         # 配置（Mvc / Mybatis / Redisson / RabbitMQ / 全局异常）
src/main/resources/
├── application.yml # 主配置
├── seckill.lua     # 秒杀原子脚本
├── unlock.lua      # 分布式锁释放脚本
├── db/             # SQL 脚本
└── mapper/         # MyBatis XML
```

---

## 🔥 核心技术亮点

### 1. 缓存优化（缓存穿透 / 击穿 / 雪崩）

针对商铺详情等热点数据，落地三类缓存问题解决方案：

- **缓存穿透**：采用 **缓存空对象**（短 TTL）避免无效请求直击数据库。
- **缓存击穿**：采用 **逻辑过期 + 互斥锁异步重建**，热点 key 过期时仅一个线程重建缓存，其余请求直接返回旧数据，避免数据库被打爆。
- **缓存雪崩**：过期时间加随机值、多级缓存思路分散风险。

> 热点数据查询稳定在毫秒级，显著降低数据库压力。

### 2. 优惠券秒杀（Redis + Lua 原子操作）

- **库存校验 + 一人一单判断** 通过 **Redis + Lua 脚本** 原子完成，杜绝超卖与重复下单。
- **全局唯一订单号**：基于 Redis 自增 + 时间戳生成。
- **异步削峰**：秒杀下单链路改造为 **RabbitMQ 异步处理** —— 接口仅校验并投递消息，消费端异步落库，缩短响应时间、削平瞬时峰值并实现业务解耦。
- **分布式锁**：Redisson 实现锁，`unlock.lua` 保证释放的原子性。

### 3. 点赞与排行榜（Redis ZSet）

- 基于 **ZSet**（score 存点赞时间戳）实现 **一人一赞、取消点赞、点赞排行榜 Top5**。
- 秒级去重 + 排序，避免数据库高频写。

### 4. 社交与 Feed 流

- **关注 / 取关 / 共同关注**：使用 **Redis Set**，共同关注通过 **Set 交集（SINTER）** 实现。
- **Feed 流（推模式）**：发布动态时推送到粉丝收件箱 ZSet，基于时间戳实现 **滚动分页**，解决深分页性能问题。

---

## 🚀 快速开始

### 环境要求

- JDK 17
- MySQL 8.x
- Redis 6.x
- RabbitMQ 3.x
- Maven 3.8+

### 1. 初始化数据库

执行 `src/main/resources/db/` 下的 SQL 脚本导入表结构与初始数据。

### 2. 修改配置

编辑 `src/main/resources/application.yml`，配置你的数据库、Redis、RabbitMQ 连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/hmdp?useSSL=false&serverTimezone=UTC
    username: your_username
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379
  rabbitmq:
    host: localhost
    port: 5672
    username: your_username
    password: your_password
```

### 3. 启动服务

```bash
mvn spring-boot:run
```

或打包后运行：

```bash
mvn clean package -DskipTests
java -jar target/hm-dianping-0.0.1-SNAPSHOT.jar
```

服务默认端口见 `application.yml` 中 `server.port`。

---

## 📡 主要接口概览

| 模块 | 接口示例 | 说明 |
|------|---------|------|
| 商铺 | `GET /shop/{id}`、`GET /shop/of/type` | 商铺详情（带缓存）、按类型查询 |
| 商铺类型 | `GET /shop-type/list` | 商铺分类列表 |
| 点评 | `POST /blog`、`GET /blog/of/follow` | 发布探店笔记、关注流 |
| 点赞 | `PUT /blog/like/{id}`、`GET /blog/likes/{id}` | 点赞 / 取消、点赞排行 |
| 关注 | `PUT /follow/{id}/or/not`、`GET /follow/common/{id}` | 关注 / 取关、共同关注 |
| 优惠券 | `POST /voucher/seckill` | 新增秒杀券 |
| 秒杀 | `POST /voucher-order/seckill/{id}` | 秒杀下单（异步） |
| 用户 | `POST /user/login`、`GET /user/{id}` | 登录、查询用户信息 |
| 上传 | `POST /upload/blog` | 图片上传 |

---

## 💡 设计要点

- **分层清晰**：Controller → Service → Mapper 标准三层，DTO / Entity 分离。
- **全局异常处理**：`WebExceptionAdvice` 统一处理业务异常。
- **AOP 应用**：使用 aspectjweaver 处理横切逻辑。
- **Redis 连接池**：commons-pool2 管理连接池。
- **Lua 脚本外置**：`seckill.lua` / `unlock.lua` 独立管理，便于维护与复用。

---

## 📄 License

本项目为个人学习实践项目，仅供学习交流使用。
