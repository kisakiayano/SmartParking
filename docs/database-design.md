# 数据库设计

## 1. 设计原则

数据库采用 MySQL 8，表结构围绕用户、车辆、车位、预约、停车记录、计费规则、识别日志和操作日志设计。

设计原则：

- 使用 InnoDB 引擎。
- 主键使用 `BIGINT`。
- 业务状态使用 `VARCHAR` 存储枚举值，便于可读性和扩展。
- 金额使用 `DECIMAL(10,2)`。
- 时间使用 `DATETIME`。
- 高频查询字段建立索引。
- 关键唯一业务字段建立唯一索引。
- 车位并发变更使用 `version` 乐观锁字段。
- 所有核心业务表建议保留 `create_time` 和 `update_time`，即使原始需求未列出，也作为企业级审计基础字段。

## 2. 命名规范

数据库命名：

- 数据库名：`smart_parking`
- 表名：小写下划线复数形式，例如 `parking_spaces`
- 字段名：小写下划线，例如 `user_id`
- Java 实体字段：小驼峰，例如 `userId`

## 3. 表关系概览

```text
users 1 ---- n vehicles
users 1 ---- n reservations
vehicles 1 ---- n reservations
vehicles 1 ---- n parking_records
parking_spaces 1 ---- n reservations
parking_spaces 1 ---- n parking_records
fee_rules 1 ---- n parking_records (logical rule usage)
```

## 4. 枚举设计

### 4.1 用户角色

| 值 | 说明 |
| --- | --- |
| `ADMIN` | 管理员 |
| `USER` | 车主用户 |

Spring Security 中映射为：

| 数据库值 | 权限值 |
| --- | --- |
| `ADMIN` | `ROLE_ADMIN` |
| `USER` | `ROLE_USER` |

### 4.2 用户状态

| 值 | 说明 |
| --- | --- |
| `ENABLED` | 启用 |
| `DISABLED` | 禁用 |

### 4.3 车位状态

| 值 | 说明 | 前端颜色 |
| --- | --- | --- |
| `FREE` | 空闲 | 绿色 |
| `RESERVED` | 已预约 | 橙色 |
| `OCCUPIED` | 已占用 | 红色 |

### 4.4 预约状态

| 值 | 说明 |
| --- | --- |
| `ACTIVE` | 生效中 |
| `USED` | 已使用 |
| `EXPIRED` | 已过期 |
| `CANCELLED` | 已取消 |

### 4.5 停车记录状态

| 值 | 说明 |
| --- | --- |
| `PARKING` | 停车中 |
| `FINISHED` | 已完成 |

### 4.6 摄像头类型

| 值 | 说明 |
| --- | --- |
| `ENTRY` | 入口摄像头 |
| `EXIT` | 出口摄像头 |

### 4.7 识别结果

| 值 | 说明 |
| --- | --- |
| `SUCCESS` | 识别成功 |
| `FAILED` | 识别失败 |
| `LOW_CONFIDENCE` | 识别成功但置信度低于自动放行阈值 |

## 5. 表结构设计

### 5.1 users

用户表。

Phase 2.1 已落地脚本：

- `backend/src/main/resources/db/sql/V2_1__users.sql`

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `user_id` | BIGINT | PK | 用户 ID |
| `username` | VARCHAR(64) | NOT NULL, UNIQUE | 登录账号 |
| `password` | VARCHAR(100) | NOT NULL | BCrypt 密码 |
| `name` | VARCHAR(64) | NOT NULL | 姓名 |
| `phone` | VARCHAR(20) | UNIQUE | 手机号 |
| `role` | VARCHAR(20) | NOT NULL | 角色 |
| `status` | VARCHAR(20) | NOT NULL | 状态 |
| `token_version` | BIGINT | NOT NULL DEFAULT 0 | 令牌版本号，改密/禁用时递增使已签发 JWT 立即失效 |
| `create_time` | DATETIME | NOT NULL | 创建时间 |
| `update_time` | DATETIME | NOT NULL | 更新时间 |

索引：

- `uk_users_username(username)`
- `uk_users_phone(phone)`
- `idx_users_role(role)`
- `idx_users_status(status)`

外键关系：

- 当前阶段 `users` 不依赖其他表。
- 后续 `vehicles.user_id`、`reservations.user_id` 将引用 `users.user_id`。
- 操作日志中的操作人暂按用户名冗余记录，不强制外键，避免用户删除或改名影响审计记录。

Phase 2.1 业务约束：

- `username` 全局唯一。
- 非空 `phone` 全局唯一。
- 注册用户默认 `role=USER`、`status=ENABLED`。
- `password` 必须存储 BCrypt 密文。

### 5.2 vehicles

车辆表。

Phase 2.2 已落地脚本：

- `backend/src/main/resources/db/sql/V2_2__vehicles.sql`

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `vehicle_id` | BIGINT | PK | 车辆 ID |
| `plate_number` | VARCHAR(20) | NOT NULL, UNIQUE | 车牌号 |
| `vehicle_type` | VARCHAR(30) | NOT NULL | 车辆类型 |
| `user_id` | BIGINT | NOT NULL, FK | 所属用户 |
| `create_time` | DATETIME | NOT NULL | 创建时间 |
| `update_time` | DATETIME | NOT NULL | 更新时间 |

索引：

- `uk_vehicles_plate_number(plate_number)`
- `idx_vehicles_user_id(user_id)`

外键关系：

- `vehicles.user_id` 引用 `users.user_id`。
- 一个用户可以拥有多辆车。
- 后续 `reservations.vehicle_id` 和 `parking_records.vehicle_id` 将引用 `vehicles.vehicle_id`。

Phase 2.2 业务约束：

- `plate_number` 全局唯一。
- 普通用户只能新增、修改、删除、查询自己的车辆。
- 管理员可以分页查询全部车辆，也可以按用户 ID 查询车辆。
- 车辆删除时已预留后续业务占用校验；预约和停车记录模块落地后，需要阻止删除存在有效预约或进行中停车记录的车辆。

### 5.3 parking_spaces

车位表。

Phase 2.3 已落地脚本：

- `backend/src/main/resources/db/sql/V2_3__parking_spaces.sql`

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `space_id` | BIGINT | PK | 车位 ID |
| `area` | VARCHAR(50) | NOT NULL | 区域 |
| `type` | VARCHAR(30) | NOT NULL | 车位类型 |
| `status` | VARCHAR(20) | NOT NULL | 状态 |
| `version` | INT | NOT NULL | 乐观锁版本 |
| `create_time` | DATETIME | NOT NULL | 创建时间 |
| `update_time` | DATETIME | NOT NULL | 更新时间 |

索引：

- `idx_parking_spaces_status(status)`
- `idx_parking_spaces_area(area)`
- `idx_parking_spaces_area_status(area, status)`

外键关系：

- 当前阶段 `parking_spaces` 不依赖其他表。
- 后续 `reservations.space_id` 和 `parking_records.space_id` 将引用 `parking_spaces.space_id`。

Phase 2.3 业务约束：

- `status` 只允许 `FREE`、`RESERVED`、`OCCUPIED`。
- `version` 使用 MyBatis Plus `@Version` 乐观锁。
- 修改车位必须携带当前 `version`。
- 删除车位时仅允许删除 `FREE` 状态。
- 车位状态缓存 Key 为 `parking:space:{spaceId}`。

### 5.4 reservations

预约表。

Phase 2.4 已落地脚本：

- `backend/src/main/resources/db/sql/V2_4__reservations.sql`

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `reservation_id` | BIGINT | PK | 预约 ID |
| `user_id` | BIGINT | NOT NULL, FK | 用户 ID |
| `vehicle_id` | BIGINT | NOT NULL, FK | 车辆 ID |
| `space_id` | BIGINT | NOT NULL, FK | 车位 ID |
| `reserve_time` | DATETIME | NOT NULL | 预约提交时间 |
| `arrive_time` | DATETIME | NOT NULL | 预计到达时间 |
| `status` | VARCHAR(20) | NOT NULL | 预约状态 |
| `active_flag` | TINYINT | VIRTUAL 生成列 | `IF(status='ACTIVE',1,NULL)`，用于条件唯一约束 |
| `create_time` | DATETIME | NOT NULL | 创建时间 |
| `update_time` | DATETIME | NOT NULL | 更新时间 |

索引：

- `idx_reservations_user_id(user_id)`
- `idx_reservations_vehicle_id(vehicle_id)`
- `idx_reservations_space_id(space_id)`
- `idx_reservations_status(status)`
- `idx_reservations_arrive_time(arrive_time)`
- `idx_reservations_status_arrive_time(status, arrive_time)`
- `uk_reservations_user_active(user_id, active_flag)` — 生成列条件唯一约束，DB 层兜底"同一用户仅一条 ACTIVE 预约"

业务唯一约束：

- 同一用户同一时间只允许一个有效预约：应用层先查后插，并发场景由 `uk_reservations_user_active` 唯一键兜底（冲突返回 409）。
- 同一车位同一时间只允许一个有效预约，通过车位乐观锁（status+version 条件更新）控制。
- 预约过期：`arrive_time` 超过宽限期（默认 30 分钟，`RESERVATION_EXPIRATION_GRACE_MINUTES`）仍未入场的预约由定时任务（每 60 秒扫描）推进为 `EXPIRED` 并释放车位。

外键关系：

- `reservations.user_id` 引用 `users.user_id`。
- `reservations.vehicle_id` 引用 `vehicles.vehicle_id`。
- `reservations.space_id` 引用 `parking_spaces.space_id`。

Phase 2.4 业务约束：

- 创建预约时车辆必须属于当前用户。
- 创建预约时用户不能已有 `ACTIVE` 预约。
- 创建预约时车位必须为 `FREE`。
- 创建预约使用 `parking_spaces.status` 和 `parking_spaces.version` 条件更新，防止同一车位重复预约。
- 创建预约成功后车位状态更新为 `RESERVED`，并刷新 Redis `parking:space:{spaceId}`。
- 取消预约仅允许取消自己的 `ACTIVE` 预约。
- 取消预约成功后预约状态更新为 `CANCELLED`，车位状态恢复为 `FREE`，并刷新 Redis `parking:space:{spaceId}`。

### 5.5 parking_records

停车记录表。

Phase 2.5 已落地脚本：

- `backend/src/main/resources/db/sql/V2_5__parking_records.sql`

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `record_id` | BIGINT | PK | 记录 ID |
| `vehicle_id` | BIGINT | NOT NULL, FK | 车辆 ID |
| `space_id` | BIGINT | NOT NULL, FK | 车位 ID |
| `entry_time` | DATETIME | NOT NULL | 入场时间 |
| `exit_time` | DATETIME | NULL | 出场时间 |
| `duration` | INT | NULL | 停车时长，单位分钟 |
| `fee` | DECIMAL(10,2) | NULL | 停车费用 |
| `status` | VARCHAR(20) | NOT NULL | 停车状态 |
| `active_flag` | TINYINT | VIRTUAL 生成列 | `IF(status='PARKING',1,NULL)`，用于条件唯一约束 |
| `create_time` | DATETIME | NOT NULL | 创建时间 |
| `update_time` | DATETIME | NOT NULL | 更新时间 |

索引：

- `idx_parking_records_vehicle_id(vehicle_id)`
- `idx_parking_records_space_id(space_id)`
- `idx_parking_records_entry_time(entry_time)`
- `idx_parking_records_exit_time(exit_time)`
- `idx_parking_records_status(status)`
- `idx_parking_records_status_entry_time(status, entry_time)`
- `uk_parking_records_vehicle_active(vehicle_id, active_flag)` — 生成列条件唯一约束，DB 层兜底"同一车辆仅一条 PARKING 记录"

业务约束：

- 同一车辆只能存在一条 `PARKING` 状态记录：应用层先查后插，并发场景由 `uk_parking_records_vehicle_active` 唯一键兜底（冲突返回 409）。
- 同一车位只能存在一条进行中停车记录，业务层事务校验和车位状态控制。
- 人工入场时，车辆必须已存在于 `vehicles` 表。
- 人工入场无匹配预约时，车位必须为 `FREE`。
- 人工入场存在匹配 `ACTIVE` 预约时，车位必须为 `RESERVED`，预约未过期（`arrive_time` + 宽限期）方可入场，入场成功后预约以条件更新（CAS）置为 `USED`。
- 人工入场使用 `parking_spaces.status` 和 `parking_spaces.version` 条件更新，防止并发重复占用。
- 人工入场成功后车位状态更新为 `OCCUPIED`，并刷新 Redis `parking:space:{spaceId}`。
- 人工出场必须存在 `PARKING` 记录。
- 人工出场成功后记录状态更新为 `FINISHED`，写入 `exit_time` 和分钟级 `duration`，车位状态恢复为 `FREE`，并刷新 Redis `parking:space:{spaceId}`。
- Phase 2.6 起，人工出场会按启用计费规则计算费用并写入 `fee`。

外键关系：

- `parking_records.vehicle_id` 引用 `vehicles.vehicle_id`。
- `parking_records.space_id` 引用 `parking_spaces.space_id`。

### 5.6 fee_rules

计费规则表。

Phase 2.6 已落地脚本：

- `backend/src/main/resources/db/sql/V2_6__fee_rules.sql`

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `rule_id` | BIGINT | PK | 规则 ID |
| `rule_name` | VARCHAR(64) | NOT NULL | 规则名称 |
| `unit_price` | DECIMAL(10,2) | NOT NULL | 每小时单价 |
| `free_minute` | INT | NOT NULL | 免费时长，单位分钟 |
| `max_daily` | DECIMAL(10,2) | NOT NULL | 日封顶金额 |
| `enabled` | TINYINT | NOT NULL | 是否启用 |
| `create_time` | DATETIME | NOT NULL | 创建时间 |
| `update_time` | DATETIME | NOT NULL | 更新时间 |

索引：

- `idx_fee_rules_enabled(enabled)`
- `uk_fee_rules_enabled((IF(enabled=1,1,NULL)))` — 函数唯一索引（MySQL 8.0.13+），DB 层兜底"同一时间仅一条启用规则"

业务约束：

- 同一时间只允许一条启用规则：应用层先禁用其他规则再启用，并发场景由函数唯一索引兜底（冲突返回 409）。
- 新增启用规则时，会先禁用其他启用规则。
- 修改规则为启用状态时，会先禁用其他启用规则。
- 启用指定规则时，会先禁用其他启用规则。
- 出场计费必须存在一条启用规则；读取启用规则按 `update_time` 降序兜底排序。
- 金额字段统一保留两位小数。
- `max_daily` 语义为"每日封顶"：跨天停车按自然日拆分，每日费用分别封顶后累加；免费时长整个会话只扣一次。
- Phase 2.6 初始化默认规则：免费 15 分钟、每小时 5 元、日封顶 60 元。

### 5.7 plate_recognition_logs

车牌识别日志表。

Phase 2.8 已落地脚本：

- `backend/src/main/resources/db/sql/V2_8__plate_recognition_logs.sql`

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK | 日志 ID |
| `record_id` | BIGINT | NULL, FK | 关联停车记录 ID |
| `plate_number` | VARCHAR(20) | NULL | 识别出的车牌号 |
| `plate_color` | VARCHAR(30) | NULL | 百度返回的车牌颜色 |
| `confidence` | DECIMAL(5,4) | NULL | 置信度 |
| `image_url` | VARCHAR(255) | NOT NULL | 图片标识或地址 |
| `recognition_time` | DATETIME | NOT NULL | 识别时间 |
| `camera_type` | VARCHAR(20) | NOT NULL | 摄像头类型 |
| `provider` | VARCHAR(30) | NOT NULL | OCR 服务商，当前为 `BAIDU` |
| `provider_log_id` | VARCHAR(64) | NULL | 百度返回的 `log_id` |
| `result` | VARCHAR(20) | NOT NULL | 识别结果 |
| `error_code` | VARCHAR(50) | NULL | 百度 API 或系统错误码 |
| `error_message` | VARCHAR(500) | NULL | 百度 API 或系统错误信息 |
| `create_time` | DATETIME | NOT NULL | 创建时间 |

索引：

- `idx_plate_logs_record_id(record_id)`
- `idx_plate_logs_plate_number(plate_number)`
- `idx_plate_logs_recognition_time(recognition_time)`
- `idx_plate_logs_camera_type(camera_type)`
- `idx_plate_logs_provider(provider)`
- `idx_plate_logs_provider_log_id(provider_log_id)`
- `idx_plate_logs_result(result)`

外键关系：

- `plate_recognition_logs.record_id` 引用 `parking_records.record_id`。

业务约束：

- 图片上传识别、OCR 入场、OCR 出场都会写入识别日志。
- 识别成功、识别失败、低置信度都必须落库。
- OCR 入场成功后日志关联新建的 `parking_records.record_id`。
- OCR 出场成功后日志关联完成的 `parking_records.record_id`。
- 当前生产策略为百度 OCR，供应商字段写入 `BAIDU`。
- PaddleOCR、AliyunOCR 作为策略扩展点预留，当前不落地调用。

### 5.8 operation_logs

操作日志表。

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK | 日志 ID |
| `operator` | VARCHAR(64) | NOT NULL | 操作人 |
| `operation_type` | VARCHAR(50) | NOT NULL | 操作类型 |
| `operation_content` | VARCHAR(1000) | NOT NULL | 操作内容 |
| `operation_time` | DATETIME | NOT NULL | 操作时间 |
| `create_time` | DATETIME | NOT NULL | 创建时间 |

索引：

- `idx_operation_logs_operator(operator)`
- `idx_operation_logs_operation_type(operation_type)`
- `idx_operation_logs_operation_time(operation_time)`

### 5.9 ai_chat_sessions

AI 助手会话表。

Phase 2.9 已落地脚本：

- `backend/src/main/resources/db/sql/V2_9__ai_assistant.sql`

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `session_id` | BIGINT | PK | 会话 ID |
| `user_id` | BIGINT | NOT NULL, FK | 所属用户 |
| `title` | VARCHAR(128) | NOT NULL | 会话标题，取首个问题前 40 字 |
| `provider` | VARCHAR(32) | NOT NULL | 实际回答引擎：`rule-based` 或 `openai` |
| `message_count` | INT | NOT NULL, DEFAULT 0 | 消息数量 |
| `last_message_time` | DATETIME | NOT NULL | 最近消息时间 |
| `create_time` | DATETIME | NOT NULL | 创建时间 |
| `update_time` | DATETIME | NOT NULL | 更新时间 |

索引：

- `idx_ai_chat_sessions_user(user_id, last_message_time)`
- `idx_ai_chat_sessions_last_message(last_message_time)`

外键关系：

- `ai_chat_sessions.user_id` 引用 `users.user_id`。

业务约束：

- 会话只能由所属用户查看和删除，管理员可以查询全部会话用于服务审计。
- 会话标题在创建时由首个问题生成，最长 128 字符。

### 5.10 ai_chat_messages

AI 助手会话消息表。

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `message_id` | BIGINT | PK | 消息 ID |
| `session_id` | BIGINT | NOT NULL, FK | 所属会话 |
| `user_id` | BIGINT | NOT NULL, FK | 所属用户 |
| `role` | VARCHAR(16) | NOT NULL | 消息角色：`USER` 或 `ASSISTANT` |
| `content` | TEXT | NOT NULL | 消息内容 |
| `intent` | VARCHAR(32) | NULL | 识别出的业务意图 |
| `provider` | VARCHAR(32) | NULL | 生成回答的引擎 |
| `referenced_data` | TEXT | NULL | 回答引用的实时业务数据，JSON 数组 |
| `create_time` | DATETIME | NOT NULL | 消息时间 |

索引：

- `idx_ai_chat_messages_session(session_id, create_time)`
- `idx_ai_chat_messages_user(user_id, create_time)`

外键关系：

- `ai_chat_messages.session_id` 引用 `ai_chat_sessions.session_id`，级联删除。
- `ai_chat_messages.user_id` 引用 `users.user_id`。

业务约束：

- 每次问答写入一条用户消息和一条助手回答。
- `referenced_data` 只保存业务数据摘要，例如“空闲：4”“计费规则：默认计费规则”，不保存外部模型密钥或敏感信息。

### 5.11 user_suggestions

用户意见与建议表。

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `suggestion_id` | BIGINT | PK | 建议 ID |
| `user_id` | BIGINT | NOT NULL, FK | 提交用户 |
| `category` | VARCHAR(32) | NOT NULL | 分类：`FUNCTION`、`EXPERIENCE`、`BUG`、`CHARGE`、`OTHER` |
| `title` | VARCHAR(128) | NOT NULL | 标题 |
| `content` | VARCHAR(2000) | NOT NULL | 建议内容 |
| `contact` | VARCHAR(64) | NULL | 联系方式 |
| `status` | VARCHAR(16) | NOT NULL | 状态：`PENDING`、`PROCESSING`、`RESOLVED`、`REJECTED` |
| `reply` | VARCHAR(1000) | NULL | 管理员回复 |
| `handler_id` | BIGINT | NULL, FK | 处理人 |
| `handle_time` | DATETIME | NULL | 处理时间 |
| `create_time` | DATETIME | NOT NULL | 提交时间 |
| `update_time` | DATETIME | NOT NULL | 更新时间 |

索引：

- `idx_user_suggestions_user(user_id, create_time)`
- `idx_user_suggestions_status(status, create_time)`
- `idx_user_suggestions_category(category)`

外键关系：

- `user_suggestions.user_id` 引用 `users.user_id`。
- `user_suggestions.handler_id` 引用 `users.user_id`。

业务约束：

- 状态流转：`PENDING` → `PROCESSING`/`RESOLVED`/`REJECTED`；`PROCESSING` → `RESOLVED`/`REJECTED`。
- `RESOLVED` 和 `REJECTED` 为终态，不可再变更。
- 标记为终态时必须填写 `reply`。

### 5.12 ai_prompt_templates

AI 提示词模板表，将 AI 助手的系统提示词外部化，支持按业务意图配置并由管理员维护。

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `template_id` | BIGINT | PK | 模板 ID |
| `template_code` | VARCHAR(64) | NOT NULL, UNIQUE | 模板编码，如 `default_assistant`、`space_status` |
| `template_name` | VARCHAR(128) | NOT NULL | 模板名称 |
| `intent` | VARCHAR(32) | NULL | 关联的业务意图，对应 `AiIntent`；NULL 表示通用兜底模板 |
| `system_prompt` | TEXT | NOT NULL | 系统提示词内容 |
| `enabled` | TINYINT | NOT NULL, 默认 1 | 是否启用：1 启用、0 停用 |
| `create_time` | DATETIME | NOT NULL | 创建时间 |
| `update_time` | DATETIME | NOT NULL | 更新时间 |

索引：

- `uk_ai_prompt_templates_code(template_code)`
- `idx_ai_prompt_templates_intent(intent)`
- `idx_ai_prompt_templates_enabled(enabled)`

业务约束：

- 同一 `intent` 下应仅保留一个启用模板。
- `default_assistant`（`intent` 为空）是全局兜底模板。
- 表为空或查询为空时，`AiPromptTemplateService.resolveSystemPrompt` 使用代码内置默认提示词。
- 本表为独立配置表，不与其他业务表建立外键。

### 5.13 安全加固迭代表结构变更（老库升级）

本项目暂未接入 Flyway/Liquibase，建表 SQL 仅在 docker-compose 首次建库时执行。已存在的数据库需要手动执行以下 ALTER 完成安全加固迭代升级：

```sql
-- users：令牌版本号（改密/禁用用户踢会话）
ALTER TABLE users
  ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0;

-- reservations：同一用户仅一条 ACTIVE 预约的 DB 兜底约束
ALTER TABLE reservations
  ADD COLUMN active_flag TINYINT AS (IF(status = 'ACTIVE', 1, NULL)) VIRTUAL,
  ADD UNIQUE KEY uk_reservations_user_active (user_id, active_flag);

-- parking_records：同一车辆仅一条 PARKING 记录的 DB 兜底约束
ALTER TABLE parking_records
  ADD COLUMN active_flag TINYINT AS (IF(status = 'PARKING', 1, NULL)) VIRTUAL,
  ADD UNIQUE KEY uk_parking_records_vehicle_active (vehicle_id, active_flag);

-- fee_rules：同一时间仅一条启用规则的 DB 兜底约束（要求 MySQL 8.0.13+）
ALTER TABLE fee_rules
  ADD UNIQUE KEY uk_fee_rules_enabled ((IF(enabled = 1, 1, NULL)));
```

执行前提：老数据必须满足约束语义（同一用户最多一条 ACTIVE 预约、同一车辆最多一条 PARKING 记录、最多一条启用规则），否则 ALTER 会因重复数据失败，需先人工清理。

## 6. 建表 SQL 草案

> 说明：本节是数据库设计草案，用于后续实现迁移脚本时参考。当前阶段不执行 SQL。

```sql
CREATE DATABASE IF NOT EXISTS smart_parking
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE smart_parking;

CREATE TABLE users (
  user_id BIGINT PRIMARY KEY,
  username VARCHAR(64) NOT NULL,
  password VARCHAR(100) NOT NULL,
  name VARCHAR(64) NOT NULL,
  phone VARCHAR(20) NULL,
  role VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  UNIQUE KEY uk_users_username (username),
  UNIQUE KEY uk_users_phone (phone),
  KEY idx_users_role (role),
  KEY idx_users_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE vehicles (
  vehicle_id BIGINT PRIMARY KEY,
  plate_number VARCHAR(20) NOT NULL,
  vehicle_type VARCHAR(30) NOT NULL,
  user_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  UNIQUE KEY uk_vehicles_plate_number (plate_number),
  KEY idx_vehicles_user_id (user_id),
  CONSTRAINT fk_vehicles_user_id FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE parking_spaces (
  space_id BIGINT PRIMARY KEY,
  area VARCHAR(50) NOT NULL,
  type VARCHAR(30) NOT NULL,
  status VARCHAR(20) NOT NULL,
  version INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  KEY idx_parking_spaces_status (status),
  KEY idx_parking_spaces_area (area),
  KEY idx_parking_spaces_area_status (area, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reservations (
  reservation_id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  vehicle_id BIGINT NOT NULL,
  space_id BIGINT NOT NULL,
  reserve_time DATETIME NOT NULL,
  arrive_time DATETIME NOT NULL,
  status VARCHAR(20) NOT NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  KEY idx_reservations_user_id (user_id),
  KEY idx_reservations_vehicle_id (vehicle_id),
  KEY idx_reservations_space_id (space_id),
  KEY idx_reservations_status (status),
  KEY idx_reservations_arrive_time (arrive_time),
  KEY idx_reservations_status_arrive_time (status, arrive_time),
  CONSTRAINT fk_reservations_user_id FOREIGN KEY (user_id) REFERENCES users (user_id),
  CONSTRAINT fk_reservations_vehicle_id FOREIGN KEY (vehicle_id) REFERENCES vehicles (vehicle_id),
  CONSTRAINT fk_reservations_space_id FOREIGN KEY (space_id) REFERENCES parking_spaces (space_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE parking_records (
  record_id BIGINT PRIMARY KEY,
  vehicle_id BIGINT NOT NULL,
  space_id BIGINT NOT NULL,
  entry_time DATETIME NOT NULL,
  exit_time DATETIME NULL,
  duration INT NULL,
  fee DECIMAL(10,2) NULL,
  status VARCHAR(20) NOT NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  KEY idx_parking_records_vehicle_id (vehicle_id),
  KEY idx_parking_records_space_id (space_id),
  KEY idx_parking_records_entry_time (entry_time),
  KEY idx_parking_records_exit_time (exit_time),
  KEY idx_parking_records_status (status),
  KEY idx_parking_records_status_entry_time (status, entry_time),
  CONSTRAINT fk_parking_records_vehicle_id FOREIGN KEY (vehicle_id) REFERENCES vehicles (vehicle_id),
  CONSTRAINT fk_parking_records_space_id FOREIGN KEY (space_id) REFERENCES parking_spaces (space_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE fee_rules (
  rule_id BIGINT PRIMARY KEY,
  rule_name VARCHAR(64) NOT NULL,
  unit_price DECIMAL(10,2) NOT NULL,
  free_minute INT NOT NULL,
  max_daily DECIMAL(10,2) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  KEY idx_fee_rules_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO fee_rules (
  rule_id,
  rule_name,
  unit_price,
  free_minute,
  max_daily,
  enabled,
  create_time,
  update_time
)
SELECT 1001, '默认计费规则', 5.00, 15, 60.00, 1, NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM fee_rules
);

CREATE TABLE plate_recognition_logs (
  id BIGINT PRIMARY KEY,
  record_id BIGINT NULL,
  plate_number VARCHAR(20) NULL,
  plate_color VARCHAR(30) NULL,
  confidence DECIMAL(5,4) NULL,
  image_url VARCHAR(255) NOT NULL,
  recognition_time DATETIME NOT NULL,
  camera_type VARCHAR(20) NOT NULL,
  provider VARCHAR(30) NOT NULL,
  provider_log_id VARCHAR(64) NULL,
  result VARCHAR(20) NOT NULL,
  error_code VARCHAR(50) NULL,
  error_message VARCHAR(500) NULL,
  create_time DATETIME NOT NULL,
  KEY idx_plate_logs_record_id (record_id),
  KEY idx_plate_logs_plate_number (plate_number),
  KEY idx_plate_logs_recognition_time (recognition_time),
  KEY idx_plate_logs_camera_type (camera_type),
  KEY idx_plate_logs_provider (provider),
  KEY idx_plate_logs_provider_log_id (provider_log_id),
  KEY idx_plate_logs_result (result),
  CONSTRAINT fk_plate_logs_record_id FOREIGN KEY (record_id) REFERENCES parking_records (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE operation_logs (
  id BIGINT PRIMARY KEY,
  operator VARCHAR(64) NOT NULL,
  operation_type VARCHAR(50) NOT NULL,
  operation_content VARCHAR(1000) NOT NULL,
  operation_time DATETIME NOT NULL,
  create_time DATETIME NOT NULL,
  KEY idx_operation_logs_operator (operator),
  KEY idx_operation_logs_operation_type (operation_type),
  KEY idx_operation_logs_operation_time (operation_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ai_chat_sessions (
  session_id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  provider VARCHAR(32) NOT NULL,
  message_count INT NOT NULL DEFAULT 0,
  last_message_time DATETIME NOT NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  KEY idx_ai_chat_sessions_user (user_id, last_message_time),
  KEY idx_ai_chat_sessions_last_message (last_message_time),
  CONSTRAINT fk_ai_chat_sessions_user_id FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ai_chat_messages (
  message_id BIGINT PRIMARY KEY,
  session_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role VARCHAR(16) NOT NULL,
  content TEXT NOT NULL,
  intent VARCHAR(32) NULL,
  provider VARCHAR(32) NULL,
  referenced_data TEXT NULL,
  create_time DATETIME NOT NULL,
  KEY idx_ai_chat_messages_session (session_id, create_time),
  KEY idx_ai_chat_messages_user (user_id, create_time),
  CONSTRAINT fk_ai_chat_messages_session_id FOREIGN KEY (session_id) REFERENCES ai_chat_sessions (session_id) ON DELETE CASCADE,
  CONSTRAINT fk_ai_chat_messages_user_id FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_suggestions (
  suggestion_id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  category VARCHAR(32) NOT NULL,
  title VARCHAR(128) NOT NULL,
  content VARCHAR(2000) NOT NULL,
  contact VARCHAR(64) NULL,
  status VARCHAR(16) NOT NULL,
  reply VARCHAR(1000) NULL,
  handler_id BIGINT NULL,
  handle_time DATETIME NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  KEY idx_user_suggestions_user (user_id, create_time),
  KEY idx_user_suggestions_status (status, create_time),
  KEY idx_user_suggestions_category (category),
  CONSTRAINT fk_user_suggestions_user_id FOREIGN KEY (user_id) REFERENCES users (user_id),
  CONSTRAINT fk_user_suggestions_handler_id FOREIGN KEY (handler_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ai_prompt_templates (
  template_id BIGINT PRIMARY KEY,
  template_code VARCHAR(64) NOT NULL,
  template_name VARCHAR(128) NOT NULL,
  intent VARCHAR(32) NULL,
  system_prompt TEXT NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  UNIQUE KEY uk_ai_prompt_templates_code (template_code),
  KEY idx_ai_prompt_templates_intent (intent),
  KEY idx_ai_prompt_templates_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## 7. Redis 数据设计

### 7.1 车位状态

Key：

```text
parking:space:{id}
```

Value 示例：

```json
{
  "spaceId": 1001,
  "area": "A",
  "type": "STANDARD",
  "status": "FREE",
  "version": 3
}
```

用途：

- 车位列表快速展示。
- Dashboard 实时统计。

一致性：

- 车位状态变更事务提交后更新缓存。
- 更新失败时删除缓存，下一次查询回源数据库。

### 7.2 Dashboard

Key：

```text
parking:dashboard:statistics
```

Value：

```json
{
  "todayIncome": 1280.00,
  "todayParkingCount": 88,
  "totalSpaces": 100,
  "freeSpaces": 60,
  "reservedSpaces": 20,
  "occupiedSpaces": 20,
  "currentOccupancyRate": 20.00,
  "revenueTrend7Days": [],
  "revenueTrend30Days": [],
  "trafficTrend7Days": [],
  "trafficTrend30Days": []
}
```

TTL：

- 5 分钟。

数据来源：

- `parking_records.exit_time`、`parking_records.status`、`parking_records.fee` 用于收入统计。
- `parking_records.entry_time` 用于停车次数和车流量统计。
- `parking_spaces.status` 用于车位占用率统计。

### 7.3 用户 Token

Key：

```text
parking:token:{userId}:{tokenId}
```

用途：

- Token 主动失效。
- 登出。
- 禁用用户后清理 Token。

### 7.4 预约缓存

Key：

```text
parking:reservation:{id}
```

用途：

- 高频查询预约状态。
- 入场匹配预约时快速读取。

### 7.5 百度 OCR Token 缓存

Key：

```text
baidu:ocr:access-token
```

用途：

- 缓存百度 `access_token`，避免每次识别都重新获取。
- TTL 使用百度 `expires_in - 60s`，最低 60 秒。

## 8. 阶段数据库变更总览

| 阶段 | 数据库变更 | 是否破坏兼容 |
| --- | --- | --- |
| 阶段 1 | 创建 `users`、`operation_logs` | 否 |
| 阶段 2 | 创建 `vehicles`、`parking_spaces` | 否 |
| 阶段 3 | 创建 `reservations`，增加预约相关索引 | 否 |
| 阶段 4 | Phase 2.5 创建 `parking_records`，Phase 2.6 创建 `fee_rules` | 否 |
| 阶段 5 | 创建 `plate_recognition_logs` | 否 |
| 阶段 6 | Phase 2.7 不新增业务表，复用已有统计索引和 Redis 缓存 | 否 |
| 阶段 7 | 不新增业务表，补充部署初始化数据 | 否 |
| 阶段 8 | Phase 2.9 创建 `ai_chat_sessions`、`ai_chat_messages`、`user_suggestions` | 否 |
| 阶段 9 | Phase 2.10 创建 `ai_prompt_templates`，新增 Python RAG 服务（无 Java 表变更） | 否 |

## 9. 初始化数据建议

后续实现时可提供最小初始化数据：

- 管理员账号。
- 默认计费规则。
- 示例车位。

管理员密码必须存储为 BCrypt 密文，不能在 SQL 或文档中暴露生产密码。

Phase 2.1 说明：

- 当前仅提供 `users` 建表 SQL，不在仓库中写入生产管理员密码。
- 如需要默认管理员账号，应由部署环境或初始化脚本注入 BCrypt 密文。

Phase 2.2 说明：

- 当前已提供 `vehicles` 建表 SQL。
- `vehicles.user_id` 对 `users.user_id` 建立外键。
- 当前阶段不新增 `parking_spaces` 表；车位管理将在 Phase 2.3 独立实现。

Phase 2.3 说明：

- 当前已提供 `parking_spaces` 建表 SQL。
- 本阶段不新增外键。
- 后续预约和停车记录模块会引用 `parking_spaces.space_id`。

Phase 2.4 说明：

- 当前已提供 `reservations` 建表 SQL。
- 本阶段新增 `reservations.user_id`、`reservations.vehicle_id`、`reservations.space_id` 外键。
- `parking_spaces.status` 开始由预约流程写入 `RESERVED` 和 `FREE`。
- `parking_spaces.version` 开始用于预约并发控制。

Phase 2.5 说明：

- 当前已提供 `parking_records` 建表 SQL。
- 本阶段新增 `parking_records.vehicle_id`、`parking_records.space_id` 外键。
- `parking_spaces.status` 开始由停车记录流程写入 `OCCUPIED` 和 `FREE`。
- `parking_spaces.version` 继续用于入场占用和出场释放的并发控制。
- `parking_records.fee` 在 Phase 2.5 保持为空，后续由 Phase 2.6 计费模块计算。

Phase 2.6 说明：

- 当前已提供 `fee_rules` 建表 SQL。
- 本阶段新增默认计费规则初始化数据。
- 本阶段新增 `idx_fee_rules_enabled` 索引，用于查询启用计费规则。
- `parking_records.fee` 开始由人工出场流程写入。
- 本阶段不新增 `parking_records` 表结构字段，复用 Phase 2.5 预留的 `fee` 字段。

Phase 2.7 说明：

- 当前阶段不新增数据库表。
- Dashboard 今日收入和收入趋势复用 `parking_records.status`、`parking_records.exit_time`、`parking_records.fee`。
- Dashboard 今日停车次数和车流量趋势复用 `parking_records.entry_time`。
- Dashboard 当前车位占用率复用 `parking_spaces.status`。
- 当前已有 `idx_parking_records_status`、`idx_parking_records_exit_time`、`idx_parking_records_entry_time`、`idx_parking_records_status_entry_time` 支持统计查询。
- Dashboard 统计结果写入 Redis `parking:dashboard:statistics`，TTL 为 5 分钟。

Phase 2.8 说明：

- 当前已提供 `plate_recognition_logs` 建表 SQL。
- 本阶段新增 `plate_recognition_logs.record_id` 外键，用于关联 OCR 自动入出场停车记录。
- 本阶段新增识别日志索引：`idx_plate_logs_record_id`、`idx_plate_logs_plate_number`、`idx_plate_logs_recognition_time`、`idx_plate_logs_camera_type`、`idx_plate_logs_provider`、`idx_plate_logs_provider_log_id`、`idx_plate_logs_result`。
- 百度 OCR `access_token` 写入 Redis `baidu:ocr:access-token`。

Phase 2.9 说明：

- 当前已提供 `ai_chat_sessions`、`ai_chat_messages`、`user_suggestions` 建表 SQL。
- 本阶段新增 `ai_chat_sessions.user_id`、`ai_chat_messages.session_id`、`ai_chat_messages.user_id`、`user_suggestions.user_id`、`user_suggestions.handler_id` 外键。
- `ai_chat_messages` 随会话级联删除。
- 本阶段不新增 Redis 缓存；问答数据来自实时业务查询，规则引擎回答不引入额外缓存。
- 本阶段不新增初始化数据，推荐问题由 `AiKnowledgeBase` 内置知识库提供。

Phase 2.10 说明：

- 当前已提供 `ai_prompt_templates` 建表 SQL。
- 新增 5 条种子提示词模板（`template_id` 2001–2005）：`default_assistant`、`space_status`、`fee_rule`、`reservation_guide`、`suggestion_guide`，使用 `INSERT ... SELECT ... FROM DUAL WHERE NOT EXISTS` 保证幂等。
- 本表为独立配置表，不新增外键，不影响既有表结构。
- 本阶段不新增 Redis 缓存。
- RAG 知识库位于 Python 服务语料目录 `ai-service/data/knowledge`，不写入数据库；运行时实时数据仍由 Java 后端查询后通过请求上下文传给 Python 服务。

Phase 2.11 说明：

- 新增 `V2_11__ai_prompt_security.sql`，为已有部署的 5 条种子提示词模板追加防泄露约束（幂等 `UPDATE ... WHERE system_prompt NOT LIKE`），不新增表、不修改表结构。
- 配合代码侧输出拦截（Java `PromptLeakGuard`、Python `scrub_prompt_leak`）与问候语固定文案，确保系统提示词不会出现在回答中。
- 本阶段不新增 Redis 缓存。
