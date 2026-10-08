# 智慧停车场管理系统架构设计

## 1. 架构目标

智慧停车场管理系统面向中小型停车场运营场景，采用前后端分离架构，目标是支持用户管理、车辆管理、车位管理、在线预约、自动入场、自动出场、停车计费、车牌识别、数据统计、权限管理、日志审计和实时状态监控。

系统设计目标：

- 高可用：核心服务无状态化，支持横向扩展。
- 高并发：预约、入场、出场等并发场景使用事务、乐观锁和缓存控制。
- 高扩展性：OCR、支付、硬件、通知等能力通过接口和策略模式扩展。
- 高安全性：统一认证、统一鉴权、密码加密、接口限流、日志审计。
- 易维护：后端分层清晰，前端模块化，接口语义稳定。

## 2. 总体架构

```text
Browser
  |
  | HTTPS
  v
Nginx
  |
  | /api -> Backend
  | /static -> Frontend
  v
Spring Boot Backend
  |
  | JDBC / MyBatis-Plus
  v
MySQL 8
  |
  | Cache / Token / Hot Data
  v
Redis

External / Future Integrations:
  - Baidu License Plate Recognition API
  - Payment providers
  - Barrier gate controllers
  - LED screens
  - Notification providers
```

## 3. 技术栈

### 3.1 后端

- Java 17
- Spring Boot 3.x
- Spring Security
- JWT
- MyBatis-Plus
- MySQL 8
- Redis
- Maven
- Lombok
- MapStruct
- Swagger / OpenAPI 3
- Quartz 或 Spring Schedule
- Hutool

### 3.2 前端

- Vue 3
- Vite
- TypeScript
- Pinia
- Vue Router
- Axios
- Element Plus
- ECharts

### 3.3 部署

- Ubuntu 22+
- Docker
- Docker Compose
- Nginx

## 4. 后端分层

```text
controller
  - 接收 HTTP 请求
  - 参数校验
  - 调用 service
  - 返回统一响应

service
  - 业务编排
  - 事务控制
  - 并发控制
  - 缓存读写

mapper
  - MyBatis-Plus 数据访问
  - SQL 查询

entity
  - 数据库表映射

dto
  - 请求入参模型

vo
  - 响应出参模型

converter
  - MapStruct 对象转换

config
  - Security、Redis、Swagger、MyBatis-Plus 配置

security
  - JWT 过滤器
  - 登录认证
  - 权限校验

common
  - 统一响应
  - 统一异常
  - 通用枚举
  - 常量

task
  - 定时任务

strategy
  - OCR、支付、通知等策略扩展
```

## 5. 前端分层

```text
src
  api
    - Axios 请求封装
    - 业务 API 模块
  router
    - Vue Router 路由配置
    - 路由守卫
  stores
    - Pinia 状态管理
  views
    - 页面级组件
  components
    - 通用组件
  layouts
    - 登录后主布局
    - 登录页布局
  types
    - TypeScript 类型定义
  utils
    - Token、权限、格式化、校验工具
```

## 6. 核心模块

### 6.1 用户与权限模块

职责：

- 用户注册、登录、修改密码、修改资料。
- 管理员维护用户状态和角色。
- JWT 认证。
- Spring Security 鉴权。

角色：

- `ROLE_ADMIN`
- `ROLE_USER`

密码要求：

- 使用 `BCryptPasswordEncoder` 加密。
- 不允许明文存储。

### 6.2 车辆模块

职责：

- 用户维护自己的车辆。
- 管理员可查看和管理所有车辆。
- 车牌号全局唯一。

核心约束：

- `vehicles.plate_number` 建唯一索引。
- 普通用户只能操作自己的车辆。

### 6.3 车位模块

职责：

- 管理员维护车位。
- 用户查询可用车位。
- 支持车位状态展示。

车位状态：

- `FREE`：空闲。
- `RESERVED`：已预约。
- `OCCUPIED`：已占用。

并发要求：

- 使用 MyBatis-Plus 乐观锁。
- `parking_spaces.version` 作为版本字段。
- 修改车位状态时必须带状态条件和版本条件。

### 6.4 预约模块

职责：

- 用户预约空闲车位。
- 防止重复预约。
- 防止车位超卖。
- 预约成功后锁定车位。
- 超时后自动释放预约。

状态：

- `ACTIVE`
- `USED`
- `EXPIRED`
- `CANCELLED`

并发策略：

- 数据库事务保证预约记录和车位状态一致。
- 乐观锁保证同一车位不会被重复锁定。
- Redis 缓存车位状态，提高查询性能。

### 6.5 入场与出场模块

职责：

- 管理员人工登记入场和出场。
- OCR 识别后自动入场和出场。
- 入场创建停车记录并占用车位。
- 出场计算费用并释放车位。

停车记录状态：

- `PARKING`
- `FINISHED`

### 6.6 计费模块

职责：

- 管理员维护计费规则。
- 出场时根据规则计算停车费用。

计费公式：

```text
duration <= freeMinute:
  fee = 0

duration > freeMinute:
  fee = min(ceil((duration - freeMinute) / 60) * unitPrice, maxDaily)
```

费用保留两位小数。

### 6.7 OCR 车牌识别模块

职责：

- 支持图片上传识别车牌。
- 支持自动入场。
- 支持自动出场。
- 记录识别日志。

识别方案：

```text
PlateRecognitionService
  - BaiduPlateRecognitionService
```

默认实现直接调用百度智能云车牌识别 API：

- 请求地址：`https://aip.baidubce.com/rest/2.0/ocr/v1/license_plate`
- 鉴权方式：通过百度 `API Key` 和 `Secret Key` 获取 `access_token`。
- Token 获取地址：`https://aip.baidubce.com/oauth/2.0/token`
- 请求格式：`application/x-www-form-urlencoded`
- 图片参数：优先使用 `image`，即图片 Base64 编码后再 URL Encode；也可按配置使用图片 `url`。
- 结果解析：从 `words_result` 中读取车牌号、车牌颜色和逐字符置信度。
- 置信度计算：将百度返回的 `probability` 数组取平均值，作为系统内部 `confidence`。
- 阈值控制：低于配置阈值的识别结果不自动入场或出场，转入人工确认。

配置要求：

- `BAIDU_OCR_API_KEY`
- `BAIDU_OCR_SECRET_KEY`
- `BAIDU_OCR_LICENSE_PLATE_URL`
- `BAIDU_OCR_TOKEN_URL`
- `BAIDU_OCR_CONFIDENCE_THRESHOLD`

安全要求：

- 百度 API Key 和 Secret Key 只能通过环境变量或部署密钥注入。
- 不允许提交到代码仓库、配置样例中的真实值或文档中。
- `access_token` 需要缓存，缓存 TTL 应短于百度返回的有效期。

### 6.8 统计分析模块

职责：

- Dashboard 实时统计。
- 收入趋势。
- 车流量趋势。
- 车位占用率。
- 区域车位分布。
- 车牌识别成功率。

缓存策略：

- Dashboard 统计可缓存到 `parking:dashboard`。
- 实时性要求高的数据允许短 TTL。
- 发生入场、出场、预约、释放车位时主动刷新或删除相关缓存。

### 6.9 AI 智能问答与建议模块

职责：

- AI 智能问答：自然语言查询车位、车辆、预约、停车记录和计费规则。
- 意图识别与实时业务数据上下文组装。
- 多引擎策略：内置规则引擎（默认）、OpenAI 兼容大模型（可选）与 Python RAG 服务（可选）。
- 系统提示词外部化到 `ai_prompt_templates`，按业务意图解析，内置默认兜底。
- 外部大模型或 Python 服务失败自动降级到规则引擎。
- 推荐问题与回答引用数据展示。
- AI 会话与消息持久化，用户只能访问自己的会话，管理员可审计全部会话。
- 用户建议提交、查询、管理员处理与回复。

分层设计：

- `AssistantService`：会话编排、消息落库、越权校验。
- `SuggestionService`：建议创建、分页筛选、状态机与处理回复。
- `AiChatProvider`：策略接口，`RuleBasedAiChatProvider`、`OpenAiCompatibleAiChatProvider` 与 `PythonAiChatProvider` 实现。
- `AiChatProviderRouter`：按配置选择提供方并在异常时降级。
- `AiPromptTemplateService`：解析并管理外部化系统提示词。
- `AiIntentResolver`：关键词意图识别。
- `AssistantContextBuilder`：通过既有 Service 汇集实时业务数据。
- `AiKnowledgeBase`：内置业务知识与兜底回答。

数据与安全：

- 会话与消息写入 `ai_chat_sessions`、`ai_chat_messages`，建议写入 `user_suggestions`。
- 回答引用数据以 JSON 数组保存，不保存外部模型密钥或敏感信息。
- 模型密钥仅从环境变量读取，不写入代码与文档。

### 6.10 Python AI RAG 服务

服务边界：

- 独立的 Python FastAPI 服务（`ai-service`），容器内监听 `8000` 端口，通过 Docker 内网被 Java 后端以服务名 `http://ai-service:8000` 调用。
- 不直接暴露给浏览器，不配置 CORS；对外由前端 Nginx 以 `/ai-api/` 反向代理转发。
- 与 Java 后端职责分离：Java 负责鉴权、会话落库、意图识别与实时业务数据组装；Python 负责知识检索与回答生成。

检索与生成流水线：

1. markdown 知识库加载与分块：`data/knowledge/*.md` 按标题与段落切分为约 120–400 字符的块并缓存。
2. BM25 风格词法检索：对中文按单字 + 二元组、ASCII 按单词分词，构建内存 BM25 索引并返回归一化 `0..1` 分数与片段。
3. 上下文合并：Java 实时数据片段以 `score = 1.0` 合并，保证实时业务数据优先于静态知识。
4. 回答生成：优先调用 OpenAI 兼容大模型（`provider=openai`），不可用时使用抽取式回答（`provider=extractive`），未知问题也会返回兜底文本而非 500。

预设提示词解析：

- 提示词模板在 Java 侧外部化到 `ai_prompt_templates`，解析顺序为“意图启用模板 → `default_assistant` → 内置默认”。
- Python 侧另有内置预设（`default_assistant`、`SPACE_STATUS`、`FEE_RULE` 等），优先级为“请求显式 `system_prompt` → `intent` 预设 → `default_assistant`”。

降级链：

- 降级链：优先使用当前配置的 Java 提供方（`python` 或 `openai`）；两者调用失败时直接回退到内置 `rule-based` 规则引擎，接口仍返回 200。Python 服务内部再按 `openai -> extractive` 降级。

Nginx 转发：

- `location /ai-api/` 将 `/ai-api/**` 代理到 `http://ai-service:8000/`；`proxy_pass` 末尾的 `/` 会去掉 `/ai-api/` 前缀，因此 `/ai-api/v1/chat` 映射为上游 `/v1/chat`。
- 仅 `GET /ai-api/health` 为公开探针；其余 `/ai-api/**` 需携带有效 Bearer JWT，由 Nginx `auth_request` 调用后端 `/api/v1/users/me` 校验后转发。

## 7. 缓存架构

Redis Key 规范：

```text
parking:space:{id}
parking:dashboard
parking:user:{id}
parking:reservation:{id}
parking:token:{userId}:{tokenId}
```

缓存使用原则：

- 数据库是最终事实来源。
- Redis 用于提升读性能和减少热点查询压力。
- 涉及车位状态变更时，先完成数据库事务，再更新或失效缓存。
- Token 可存储到 Redis，支持主动失效和登出。

## 8. 安全架构

认证：

- 用户登录成功后签发 JWT。
- JWT 包含用户 ID、角色、过期时间。
- 服务端可结合 Redis 管理 Token 有效性。

鉴权：

- 管理端接口要求 `ROLE_ADMIN`。
- 用户端接口要求 `ROLE_USER` 或登录用户权限。
- 用户只能访问自己的车辆、预约和停车记录。

安全防护：

- MyBatis-Plus 参数绑定防止 SQL 注入。
- 前端输出转义和后端入参校验降低 XSS 风险。
- JWT 场景下关闭或合理配置 CSRF。
- 关键写接口增加重复提交校验。
- 登录、预约、OCR 上传等接口增加限流。
- 所有管理操作写入操作日志。

## 9. 事务与并发设计

### 9.1 预约车位

事务边界：

1. 查询车辆是否属于当前用户。
2. 查询车位是否为 `FREE`。
3. 使用乐观锁将车位更新为 `RESERVED`。
4. 创建 `ACTIVE` 预约记录。
5. 提交事务。
6. 更新 Redis 车位状态。

失败处理：

- 乐观锁更新失败时返回车位已被占用或预约。
- 重复预约时返回业务异常。

### 9.2 入场

事务边界：

1. 查找车辆。
2. 查找是否存在有效预约。
3. 分配或确认车位。
4. 更新预约为 `USED`。
5. 更新车位为 `OCCUPIED`。
6. 创建停车记录。
7. 提交事务。
8. 更新 Redis。

### 9.3 出场

事务边界：

1. 查询进行中的停车记录。
2. 计算停车时长。
3. 根据计费规则计算费用。
4. 更新停车记录为 `FINISHED`。
5. 释放车位为 `FREE`。
6. 提交事务。
7. 更新 Redis 和 Dashboard 缓存。

## 10. 可扩展设计

### 10.1 OCR 扩展

通过 `PlateRecognitionService` 抽象车牌识别能力。当前生产实现固定为 `BaiduPlateRecognitionService`，业务层只依赖接口，不直接感知百度 API 请求、鉴权、重试和响应解析细节。

后续如果需要切换供应商，可以新增实现类，但当前项目不再实现 OpenCV + PaddleOCR 方案。

### 10.2 支付扩展

预留 `PaymentService`：

- 微信支付
- 支付宝支付
- ETC 支付

### 10.3 硬件扩展

预留硬件适配接口：

- 道闸控制器
- LED 显示屏
- RFID 设备
- 摄像头设备

### 10.4 通知扩展

预留通知接口：

- 短信通知
- 邮件通知
- 微信消息推送

## 11. 部署架构

本地开发：

```text
MySQL + Redis: Docker Compose
Backend: Maven / Spring Boot
Frontend: Vite
```

生产部署：

```text
Nginx
  - 静态资源
  - API 反向代理
Backend Container
MySQL Container or Managed MySQL
Redis Container or Managed Redis
```

## 12. 日志与审计

系统日志：

- 应用启动日志。
- 接口异常日志。
- 关键业务日志。

操作日志：

- 管理员新增、修改、删除用户。
- 管理员修改车位。
- 管理员调整计费规则。
- 人工入场和出场。
- OCR 自动入场和出场。

识别日志：

- 车牌号。
- 置信度。
- 图片地址。
- 摄像头类型。
- 识别结果。
- 识别时间。

## 13. 阶段化架构原则

后续开发按可运行业务切片推进：

- 每个阶段都有可启动的后端服务。
- 从有前端阶段开始，每个阶段都有可启动的前端页面。
- 每个阶段都包含数据库迁移说明。
- 每个阶段都包含接口验证、单元测试和必要的集成测试。
- 阶段 5 起真实车牌识别依赖百度 OCR API 配置；自动化测试使用 Mock HTTP 服务模拟百度响应，不使用本地 PaddleOCR。
