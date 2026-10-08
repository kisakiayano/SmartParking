# 开发计划

## 1. 阶段划分原则

项目按可运行业务切片分阶段开发。每个阶段都必须满足：

- 可以独立启动后端服务。
- 从前端阶段开始，可以独立启动前端页面。
- 有明确的数据库变更。
- 有可验证的接口或页面能力。
- 有验收标准。
- 有测试方案。
- 不依赖未完成的后续阶段。

后续开发在得到确认前不开始编码。

## 2. 阶段总览

| 阶段 | 名称 | 核心目标 | 独立运行方式 |
| --- | --- | --- | --- |
| 阶段 0 | 项目设计与基础约定 | 完成架构、数据库、接口、计划文档 | 文档可独立评审 |
| 阶段 1 | 后端基础与用户权限 | 搭建后端基础、认证鉴权、用户管理 | 启动 MySQL、Redis、Spring Boot |
| 阶段 2 | 车辆与车位管理 | 完成车辆、车位 CRUD 和车位状态展示 | 后端接口可独立完成车辆车位管理 |
| 阶段 3 | 在线预约与自动释放 | 完成预约、防超卖、Redis 车位缓存、定时释放 | 后端可独立完成预约闭环 |
| 阶段 4 | 入场出场与计费 | 完成人工入场、人工出场、计费规则、停车记录 | 后端可独立完成停车收费闭环 |
| 阶段 5 | 百度 OCR 识别与日志审计 | 接入百度车牌识别 API、完成自动识别入出场和识别日志 | 后端可独立完成识别入出场流程 |
| 阶段 6 | 前端管理端与用户端 | 完成 Vue 页面、路由、状态管理、图表 | 前后端可独立启动并联调 |
| 阶段 7 | 部署与交付文档 | 完成 Docker、Nginx、Swagger、Postman、部署说明 | Docker Compose 可运行完整系统 |
| 阶段 8 | AI 智能问答与建议反馈 | 完成领域问答、AI 会话审计、用户建议闭环 | 后端可独立完成问答与建议闭环 |

## 3. 阶段 0：项目设计与基础约定

### 3.1 目标

完成项目开发前的设计文档，明确架构、数据库、接口和阶段计划。

### 3.2 交付内容

- `docs/architecture.md`
- `docs/database-design.md`
- `docs/api-design.md`
- `docs/development-plan.md`

### 3.3 独立运行说明

本阶段不运行应用服务，交付物为可独立评审的文档集合。

### 3.4 验收标准

- 文档覆盖核心业务模块。
- 文档覆盖后端、前端、数据库、缓存、安全、部署设计。
- 开发阶段已拆分。
- 每个阶段包含验收标准、测试方案和数据库变更。
- 未生成业务代码。

### 3.5 测试方案

- 人工评审文档完整性。
- 对照 `demo.md` 检查需求覆盖。
- 检查阶段是否具备独立运行能力。
- 检查数据库变更是否与业务阶段匹配。

### 3.6 数据库变更

无。仅输出数据库设计草案，不执行 SQL。

## 4. 阶段 1：后端基础与用户权限

### 4.1 目标

搭建 Spring Boot 后端基础工程，实现认证、鉴权、用户管理、统一响应、统一异常、基础操作日志。

### 4.2 功能范围

- Maven 后端工程。
- Spring Boot 3.x 基础配置。
- MyBatis-Plus 配置。
- Redis 配置。
- Swagger / OpenAPI 配置。
- 统一响应结构。
- 全局异常处理。
- 用户注册。
- 用户登录。
- JWT 签发和校验。
- Spring Security 权限控制。
- 当前用户信息查询。
- 修改个人资料。
- 修改密码。
- 管理员分页查询用户。
- 管理员启用、禁用用户。
- 操作日志 AOP 基础能力。

### 4.3 独立运行说明

运行依赖：

- MySQL
- Redis
- Spring Boot 后端

可独立验证：

- 服务健康检查。
- Swagger 页面访问。
- 用户注册和登录。
- 使用 JWT 访问受保护接口。
- 管理员访问管理接口。

### 4.4 验收标准

- 后端服务可启动。
- 数据库连接正常。
- Redis 连接正常。
- 注册用户密码使用 BCrypt 存储。
- 登录成功返回 JWT。
- 未登录访问受保护接口返回 401。
- 普通用户访问管理员接口返回 403。
- 管理员可分页查询用户。
- 禁用用户不能继续登录。
- 接口响应符合统一结构。
- 业务异常由 `GlobalExceptionHandler` 统一处理。
- 管理员关键操作写入 `operation_logs`。

### 4.5 测试方案

单元测试：

- 密码加密和校验。
- JWT 生成和解析。
- 用户状态校验。

接口测试：

- 注册成功。
- 重复用户名注册失败。
- 登录成功。
- 密码错误登录失败。
- 禁用用户登录失败。
- 未登录访问当前用户接口失败。
- 普通用户访问管理员用户列表失败。
- 管理员访问用户列表成功。

集成测试：

- 使用测试数据库验证用户注册、登录、查询完整流程。
- 验证操作日志写入。

安全测试：

- SQL 注入参数测试。
- 缺失 Token、伪造 Token、过期 Token 测试。

### 4.6 数据库变更

新增表：

- `users`
- `operation_logs`

初始化数据：

- 默认管理员账号。

注意：

- 默认管理员密码必须为 BCrypt 密文。
- 不在代码或公开文档中保存生产密码。

## 4.7 Phase 2.1：用户认证模块实现记录

完成时间：

- 2026-06-08

已实现功能：

- 用户注册。
- 用户登录。
- JWT 签发。
- JWT 刷新。
- 获取当前用户信息。
- 修改密码。
- 修改个人资料。
- 管理员分页查询用户。
- 管理员启用、禁用用户。

后端交付：

- Entity：`User`
- DTO：`RegisterRequest`、`LoginRequest`、`RefreshTokenRequest`、`UpdateProfileRequest`、`ChangePasswordRequest`、`UserPageQuery`、`UpdateUserStatusRequest`
- VO：`UserVO`、`LoginVO`
- Mapper：`UserMapper`
- Service：`AuthService`、`UserService`
- ServiceImpl：`AuthServiceImpl`、`UserServiceImpl`
- Controller：`AuthController`、`UserController`、`AdminUserController`
- Convert：`UserConvert`

接口权限：

- 公开接口使用 `@PreAuthorize("permitAll()")`。
- 当前用户接口使用 `@PreAuthorize("isAuthenticated()")`。
- 管理员用户接口使用 `@PreAuthorize("hasRole('ADMIN')")`。
- JWT 中写入 `userId`、`username`、`role`，过滤器转换为 Spring Security 认证上下文。

数据库变更：

- 新增 SQL 脚本：`backend/src/main/resources/db/sql/V2_1__users.sql`
- 新增表：`users`
- 新增索引：`uk_users_username`、`uk_users_phone`、`idx_users_role`、`idx_users_status`
- 外键：本阶段 `users` 为基础父表，不依赖其他表；后续车辆、预约等表引用 `users.user_id`。

验收结果：

- 注册用户密码使用 BCrypt 加密。
- 登录成功返回 JWT。
- 刷新 Token 会重新校验 Token 对应用户状态。
- 禁用用户登录会被拒绝。
- 未登录访问 `/api/v1/users/me` 返回 401。
- 接口响应使用统一 `ApiResponse`。
- 参数校验使用 `@Valid`、`@NotBlank`、`@Size`、`@Pattern`。
- Swagger 注解已覆盖认证与用户接口。
- 管理员可分页查询用户。
- 管理员可启用、禁用用户。
- 管理员不能禁用当前登录管理员账号。

测试结果：

- 已执行 `mvn clean test`。
- 覆盖范围：JWT、统一响应、认证 Service、用户 Service、认证 Controller、用户 Controller、管理员用户 Controller。

暂未实现：

- `operation_logs` 表和操作日志 AOP。

操作日志能力属于原阶段 1 文档中的扩展审计能力，未包含在本次前端完整性补齐范围内。

## 5. 阶段 2：车辆与车位管理

### 5.1 目标

实现车辆管理和车位管理，建立停车资源基础数据。

### 5.2 功能范围

- 用户新增、修改、删除、查询自己的车辆。
- 管理员查询所有车辆。
- 车牌号全局唯一。
- 管理员新增、修改、删除、查询车位。
- 用户查询车位状态。
- 车位状态颜色映射由前端按状态展示。
- 车位表引入 `version` 乐观锁字段。

### 5.3 独立运行说明

运行依赖：

- 阶段 1 后端能力。
- MySQL。
- Redis 可启动但本阶段不强依赖缓存。

可独立验证：

- 用户登录后维护车辆。
- 管理员维护车位。
- 用户查询车位列表。
- 管理员修改车位时进行乐观锁校验。

### 5.4 验收标准

- 用户只能查看和操作自己的车辆。
- 管理员可以查询所有车辆。
- 重复车牌号新增失败。
- 存在业务占用时车辆不可删除的规则已预留。
- 管理员可新增、修改、删除车位。
- 删除非空闲车位失败。
- 车位状态只允许合法枚举值。
- 修改车位时版本号不一致会失败。
- 所有写操作有操作日志。

### 5.5 测试方案

单元测试：

- 车牌号格式校验。
- 车辆归属校验。
- 车位状态枚举校验。

接口测试：

- 用户新增车辆成功。
- 重复车牌新增失败。
- 用户修改他人车辆失败。
- 管理员新增车位成功。
- 管理员修改车位成功。
- 乐观锁版本冲突失败。
- 删除 `RESERVED` 或 `OCCUPIED` 车位失败。

集成测试：

- 用户车辆 CRUD 完整流程。
- 管理员车位 CRUD 完整流程。

### 5.6 数据库变更

新增表：

- `vehicles`
- `parking_spaces`

新增索引：

- `uk_vehicles_plate_number`
- `idx_vehicles_user_id`
- `idx_parking_spaces_status`
- `idx_parking_spaces_area`
- `idx_parking_spaces_area_status`

初始化数据：

- 示例车位数据，例如 A 区、B 区若干空闲车位。

### 5.7 Phase 2.2：车辆管理模块实现记录

完成时间：

- 2026-06-08

已实现功能：

- 新增车辆。
- 修改车辆。
- 删除车辆。
- 查询车辆详情。
- 查询当前用户车辆列表。
- 管理员分页查询车辆。
- 管理员根据用户 ID 查询车辆。

后端交付：

- Entity：`Vehicle`
- DTO：`CreateVehicleRequest`、`UpdateVehicleRequest`、`VehiclePageQuery`
- VO：`VehicleVO`、`PageResponse`
- Mapper：`VehicleMapper`
- Service：`VehicleService`
- ServiceImpl：`VehicleServiceImpl`
- Controller：`VehicleController`、`AdminVehicleController`
- Convert：`VehicleConvert`

接口权限：

- 用户车辆接口使用 `@PreAuthorize("hasRole('USER')")`。
- 管理员车辆接口使用 `@PreAuthorize("hasRole('ADMIN')")`。
- 普通用户只能操作自己的车辆。
- 管理员可查看全部车辆和按用户查询车辆。

数据库变更：

- 新增 SQL 脚本：`backend/src/main/resources/db/sql/V2_2__vehicles.sql`
- 新增表：`vehicles`
- 新增索引：`uk_vehicles_plate_number`、`idx_vehicles_user_id`
- 外键：`vehicles.user_id` 引用 `users.user_id`

验收结果：

- 新增车辆会绑定当前登录用户。
- 车牌号全局唯一。
- 修改车辆时校验车辆归属。
- 删除车辆时校验车辆归属。
- 查询车辆详情时校验车辆归属。
- 未登录访问车辆接口返回 401。
- 管理员访问用户车辆接口返回 403。
- 普通用户访问管理员车辆接口返回 403。
- 接口响应使用统一 `ApiResponse`。
- 参数校验使用 `@Valid`、`@NotNull`、`@NotBlank`、`@Pattern`。
- Swagger 注解已覆盖用户车辆接口和管理员车辆接口。

测试结果：

- 已执行 `mvn clean test`。
- 结果：36 个测试全部通过。
- 覆盖范围：车辆 Service、用户车辆 Controller、管理员车辆 Controller，以及既有用户认证测试。

暂未实现：

- 删除车辆时检查有效预约或进行中停车记录。
- 该能力依赖后续预约模块和停车记录模块的数据表，当前阶段仅在文档和服务边界中预留。

阶段说明：

- 本次仅完成 `AGENTS.md` 指定的 Phase 2.2 车辆管理模块。
- `parking_spaces` 车位管理不在本次范围内，将在 Phase 2.3 独立开发。

### 5.8 Phase 2.3：车位管理模块实现记录

完成时间：

- 2026-06-08

已实现功能：

- 新增车位。
- 修改车位。
- 删除车位。
- 查询车位详情。
- 分页查询车位。
- 按区域筛选。
- 按状态筛选。
- Redis 缓存车位实时状态。
- 查询车位实时状态接口。

后端交付：

- Entity：`ParkingSpace`
- DTO：`CreateParkingSpaceRequest`、`UpdateParkingSpaceRequest`、`ParkingSpaceQuery`
- VO：`ParkingSpaceVO`、`ParkingSpaceStatusVO`
- Mapper：`ParkingSpaceMapper`
- Service：`ParkingSpaceService`
- ServiceImpl：`ParkingSpaceServiceImpl`
- Controller：`ParkingSpaceController`、`AdminParkingSpaceController`
- Convert：`ParkingSpaceConvert`

接口权限：

- 登录用户车位查询接口使用 `@PreAuthorize("isAuthenticated()")`。
- 管理员车位维护接口使用 `@PreAuthorize("hasRole('ADMIN')")`。

数据库变更：

- 新增 SQL 脚本：`backend/src/main/resources/db/sql/V2_3__parking_spaces.sql`
- 新增表：`parking_spaces`
- 新增索引：`idx_parking_spaces_status`、`idx_parking_spaces_area`、`idx_parking_spaces_area_status`
- 外键：本阶段不新增外键；后续预约和停车记录引用 `parking_spaces.space_id`

缓存变更：

- 新增 Redis Key：`parking:space:{spaceId}`
- Value：`ParkingSpaceStatusVO`
- 新增、修改车位后写入缓存。
- 删除车位后删除缓存。
- 实时状态查询优先读缓存，未命中时回源数据库并写入缓存。

验收结果：

- 管理员可新增、修改、删除车位。
- 登录用户可分页查询车位和查看实时状态。
- 可按区域、状态筛选车位。
- 车位状态只允许 `FREE`、`RESERVED`、`OCCUPIED`。
- 修改车位使用 `version` 乐观锁。
- 乐观锁冲突返回 `409`。
- 删除 `RESERVED` 或 `OCCUPIED` 车位失败。
- 车位新增、修改、删除同步更新 Redis 缓存。
- 接口响应使用统一 `ApiResponse`。
- 参数校验使用 `@Valid`、`@NotNull`、`@NotBlank`、`@Pattern`。
- Swagger 注解已覆盖车位接口和管理员车位接口。

测试结果：

- 已执行 `mvn clean test`。
- 结果：53 个测试全部通过。
- 覆盖范围：车位 Service、车位查询 Controller、管理员车位 Controller，以及既有用户和车辆测试。

阶段说明：

- 本次仅完成 `AGENTS.md` 指定的 Phase 2.3 车位管理模块。
- 预约创建、入场出场对车位状态的业务流转将在后续阶段接入。

## 6. 阶段 3：在线预约与自动释放

### 6.1 目标

实现用户预约车位、防止超卖、防止重复预约、Redis 车位状态缓存和超时自动释放。

### 6.2 功能范围

- 用户查询空闲车位。
- 用户选择车辆和车位创建预约。
- 创建预约时锁定车位。
- 用户查询自己的预约记录。
- 用户取消预约。
- 管理员查询预约记录。
- Redis 缓存车位状态。
- 定时任务每 1 分钟扫描超时 `ACTIVE` 预约。
- 超时预约更新为 `EXPIRED` 并释放车位。

### 6.3 独立运行说明

运行依赖：

- 阶段 1 用户权限。
- 阶段 2 车辆和车位。
- MySQL。
- Redis。
- Spring Schedule 或 Quartz。

可独立验证：

- 用户登录后创建预约。
- 预约成功后车位变为 `RESERVED`。
- 取消预约后车位恢复 `FREE`。
- 定时任务释放过期预约。

### 6.4 验收标准

- 用户只能使用自己的车辆预约。
- 非 `FREE` 车位不可预约。
- 同一车辆不能存在多个 `ACTIVE` 预约。
- 同一车位不能被并发重复预约。
- 预约成功后数据库车位状态为 `RESERVED`。
- 预约成功后 Redis 车位缓存同步更新。
- 取消预约后预约状态为 `CANCELLED`。
- 取消预约后车位状态恢复 `FREE`。
- 超时预约自动更新为 `EXPIRED`。
- 超时释放后 Redis 缓存更新。
- 并发预约同一车位时只有一个请求成功。

### 6.5 测试方案

单元测试：

- 预约状态流转。
- 重复预约校验。
- 车位状态校验。

接口测试：

- 创建预约成功。
- 使用他人车辆预约失败。
- 预约已占用车位失败。
- 重复预约失败。
- 取消预约成功。
- 取消他人预约失败。

并发测试：

- 多线程同时预约同一车位，断言只有一个成功。
- 多线程取消和入场同一预约，断言状态一致。

集成测试：

- 创建预约后检查 `reservations` 和 `parking_spaces` 状态。
- 创建预约后检查 Redis Key。
- 构造过期预约，等待定时任务释放。

### 6.6 数据库变更

新增表：

- `reservations`

新增索引：

- `idx_reservations_user_id`
- `idx_reservations_vehicle_id`
- `idx_reservations_space_id`
- `idx_reservations_status`
- `idx_reservations_arrive_time`
- `idx_reservations_status_arrive_time`

数据变更：

- `parking_spaces.status` 开始由预约流程写入 `RESERVED`。
- `parking_spaces.version` 开始用于预约并发控制。

### 6.7 Phase 2.4：在线预约模块实现记录

完成时间：

- 2026-06-08

已实现功能：

- 创建预约。
- 查询预约详情。
- 取消预约。
- 查看个人预约记录。
- 管理员查看全部预约。
- 使用乐观锁防止同一车位重复预约。
- 创建和取消预约时更新 Redis 车位状态缓存。

后端交付：

- Entity：`Reservation`
- DTO：`CreateReservationRequest`、`ReservationPageQuery`
- VO：`ReservationVO`
- Mapper：`ReservationMapper`
- Service：`ReservationService`
- ServiceImpl：`ReservationServiceImpl`
- Controller：`ReservationController`、`AdminReservationController`
- Convert：`ReservationConvert`

接口权限：

- 用户预约接口使用 `@PreAuthorize("hasRole('USER')")`。
- 管理员预约接口使用 `@PreAuthorize("hasRole('ADMIN')")`。

数据库变更：

- 新增 SQL 脚本：`backend/src/main/resources/db/sql/V2_4__reservations.sql`
- 新增表：`reservations`
- 新增索引：`idx_reservations_user_id`、`idx_reservations_vehicle_id`、`idx_reservations_space_id`、`idx_reservations_status`、`idx_reservations_arrive_time`、`idx_reservations_status_arrive_time`
- 新增外键：`fk_reservations_user_id`、`fk_reservations_vehicle_id`、`fk_reservations_space_id`

缓存变更：

- 复用 Redis Key：`parking:space:{spaceId}`
- 创建预约成功后写入 `RESERVED`。
- 取消预约成功后写入 `FREE`。
- 并发冲突时删除缓存，后续查询回源数据库。

验收结果：

- 用户只能使用自己的车辆预约。
- 非 `FREE` 车位不可预约。
- 同一用户同时只能存在一个 `ACTIVE` 预约。
- 同一车位不能被并发重复预约。
- 预约成功后数据库车位状态为 `RESERVED`。
- 预约成功后 Redis 车位缓存同步更新。
- 取消预约后预约状态为 `CANCELLED`。
- 取消预约后车位状态恢复 `FREE`。
- 并发预约同一车位时只有一个请求成功。
- 接口响应使用统一 `ApiResponse`。
- 参数校验使用 `@Valid`、`@NotNull`、`@Future`、`@Pattern`。
- Swagger 注解已覆盖用户预约接口和管理员预约接口。

测试结果：

- 已执行 `mvn clean test`。
- 结果：70 个测试全部通过。
- 覆盖范围：预约 Service、用户预约 Controller、管理员预约 Controller、同一车位并发预约测试，以及既有用户、车辆、车位测试。

暂未实现：

- 超时预约自动释放定时任务。
- 该能力属于阶段 3 总规划中的扩展闭环，未包含在 `AGENTS.md` Phase 2.4 的明确实现清单中。

## 7. 阶段 4：入场出场与计费

### 7.1 目标

实现人工入场、人工出场、停车记录和计费规则，形成停车收费业务闭环。

### 7.2 功能范围

- 管理员维护计费规则。
- 查询当前启用计费规则。
- 人工入场登记。
- 入场时匹配有效预约。
- 入场后创建停车记录。
- 入场后更新车位为 `OCCUPIED`。
- 出场时查询进行中停车记录。
- 出场时计算停车时长。
- 出场时按计费规则计算费用。
- 出场后更新停车记录为 `FINISHED`。
- 出场后释放车位为 `FREE`。
- 用户查询自己的停车记录和费用。
- 管理员查询全部停车记录。

### 7.3 独立运行说明

运行依赖：

- 阶段 1 用户权限。
- 阶段 2 车辆和车位。
- 阶段 3 预约能力可选；没有预约时允许人工入场。
- MySQL。
- Redis。

可独立验证：

- 管理员配置计费规则。
- 管理员人工登记入场。
- 管理员人工登记出场。
- 用户查看停车记录和费用。

### 7.4 验收标准

- 只能有一个启用的默认计费规则。
- 入场时车位必须可用或已被当前预约锁定。
- 入场后车位状态变为 `OCCUPIED`。
- 入场后生成 `PARKING` 停车记录。
- 同一车辆不能重复入场。
- 出场时必须存在 `PARKING` 记录。
- 出场后停车记录状态为 `FINISHED`。
- 出场后车位状态恢复 `FREE`。
- 费用计算符合公式并保留两位小数。
- 免费时长内费用为 0。
- 超过日封顶时费用不超过 `maxDaily`。
- Redis 车位缓存与数据库状态一致。

### 7.5 测试方案

单元测试：

- 免费时长费用计算。
- 不足一小时向上取整。
- 日封顶费用计算。
- 预约入场状态流转。

接口测试：

- 新增计费规则成功。
- 启用计费规则成功。
- 人工入场成功。
- 重复入场失败。
- 无停车记录出场失败。
- 人工出场成功。
- 用户查询自己的停车记录成功。

集成测试：

- 预约后入场，预约状态变为 `USED`，车位状态变为 `OCCUPIED`。
- 无预约入场，直接创建停车记录。
- 出场后检查停车记录、车位、Redis 状态。

### 7.6 数据库变更

新增表：

- `parking_records`
- `fee_rules`

新增索引：

- `idx_parking_records_vehicle_id`
- `idx_parking_records_space_id`
- `idx_parking_records_entry_time`
- `idx_parking_records_exit_time`
- `idx_parking_records_status`
- `idx_parking_records_status_entry_time`
- `idx_fee_rules_enabled`

初始化数据：

- 默认计费规则，例如免费 15 分钟、每小时 5 元、日封顶 60 元。

### 7.7 Phase 2.5：停车记录模块实现记录

完成时间：

- 2026-06-08

已实现功能：

- 人工登记入场。
- 人工登记出场。
- 查询停车记录详情。
- 查询个人停车记录。
- 管理员分页查询全部停车记录。
- 入场时创建 `PARKING` 停车记录。
- 出场时更新停车记录为 `FINISHED`。
- 出场时释放车位为 `FREE`。
- 入场和出场时同步 Redis 车位状态缓存。

后端交付：

- Entity：`ParkingRecord`
- DTO：`ManualEntryRequest`、`ManualExitRequest`、`ParkingRecordPageQuery`
- VO：`ParkingRecordVO`
- Mapper：`ParkingRecordMapper`
- Service：`ParkingRecordService`
- ServiceImpl：`ParkingRecordServiceImpl`
- Controller：`ParkingRecordController`、`AdminParkingRecordController`
- Convert：`ParkingRecordConvert`
- Enum：`ParkingRecordStatus`

接口权限：

- 用户停车记录接口使用 `@PreAuthorize("hasRole('USER')")`。
- 管理员停车记录接口使用 `@PreAuthorize("hasRole('ADMIN')")`。

数据库变更：

- 新增 SQL 脚本：`backend/src/main/resources/db/sql/V2_5__parking_records.sql`
- 新增表：`parking_records`
- 新增索引：`idx_parking_records_vehicle_id`、`idx_parking_records_space_id`、`idx_parking_records_entry_time`、`idx_parking_records_exit_time`、`idx_parking_records_status`、`idx_parking_records_status_entry_time`
- 新增外键：`fk_parking_records_vehicle_id`、`fk_parking_records_space_id`

缓存变更：

- 复用 Redis Key：`parking:space:{spaceId}`
- 人工入场成功后写入 `OCCUPIED`。
- 人工出场成功后写入 `FREE`。
- 并发冲突时删除缓存，后续查询回源数据库。

验收结果：

- 人工入场要求车辆已登记。
- 同一车辆不能重复入场。
- 无匹配预约时，只有 `FREE` 车位可入场。
- 有匹配 `ACTIVE` 预约时，只有 `RESERVED` 车位可入场，入场成功后预约状态更新为 `USED`。
- 入场使用车位状态和 `version` 条件更新，防止重复占用。
- 入场成功后数据库车位状态为 `OCCUPIED`。
- 入场成功后创建 `PARKING` 停车记录。
- 出场要求存在 `PARKING` 停车记录。
- 出场成功后停车记录状态为 `FINISHED`。
- 出场成功后写入 `exitTime` 和分钟级 `duration`。
- 出场成功后车位状态恢复为 `FREE`。
- 用户只能查询自己车辆关联的停车记录。
- 管理员可分页查询全部停车记录。
- 接口响应使用统一 `ApiResponse` 和 `PageResponse`。
- 参数校验使用 `@Valid`、`@NotBlank`、`@NotNull`、`@Pattern`、`@Min`。
- Swagger 注解已覆盖用户停车记录接口和管理员停车记录接口。

测试结果：

- 已执行 `mvn clean test`。
- 结果：87 个测试全部通过。
- 覆盖范围：停车记录 Service、用户停车记录 Controller、管理员停车记录 Controller，以及既有用户、车辆、车位、预约测试。

暂未实现：

- 停车费用计算。
- 计费规则表和计费规则接口。
- 上述能力属于 `AGENTS.md` Phase 2.6 计费模块范围，未在 Phase 2.5 中提前实现。

### 7.8 Phase 2.6：计费模块实现记录

完成时间：

- 2026-06-08

已实现功能：

- 新增计费规则。
- 修改计费规则。
- 查询启用计费规则。
- 查询计费规则详情。
- 管理员分页查询计费规则。
- 启用指定计费规则。
- 启用规则时自动禁用其他规则。
- 人工出场时自动计算停车费用。
- 停车费用写入 `parking_records.fee`。

后端交付：

- Entity：`FeeRule`
- DTO：`CreateFeeRuleRequest`、`UpdateFeeRuleRequest`、`FeeRulePageQuery`
- VO：`FeeRuleVO`
- Mapper：`FeeRuleMapper`
- Service：`FeeRuleService`、`FeeCalculatorService`
- ServiceImpl：`FeeRuleServiceImpl`、`FeeCalculatorServiceImpl`
- Controller：`FeeRuleController`、`AdminFeeRuleController`
- Convert：`FeeRuleConvert`

接口权限：

- 查询启用计费规则使用 `@PreAuthorize("isAuthenticated()")`。
- 管理员计费规则接口使用 `@PreAuthorize("hasRole('ADMIN')")`。

数据库变更：

- 新增 SQL 脚本：`backend/src/main/resources/db/sql/V2_6__fee_rules.sql`
- 新增表：`fee_rules`
- 新增索引：`idx_fee_rules_enabled`
- 初始化默认计费规则：免费 15 分钟、每小时 5 元、日封顶 60 元。
- 复用 `parking_records.fee` 字段，不新增停车记录表字段。

计费规则：

```text
duration <= freeMinute:
  fee = 0

duration > freeMinute:
  fee = min(ceil((duration - freeMinute) / 60) * unitPrice, maxDaily)
```

验收结果：

- 免费时长内费用为 0。
- 超过免费时长后按小时向上取整。
- 费用不超过日封顶金额。
- 没有启用计费规则时，出场计费失败并返回业务错误。
- 新增启用规则时会禁用其他启用规则。
- 修改规则为启用状态时会禁用其他启用规则。
- 启用指定规则时会禁用其他启用规则。
- 人工出场成功后停车记录状态为 `FINISHED`。
- 人工出场成功后写入 `duration` 和 `fee`。
- 接口响应使用统一 `ApiResponse` 和 `PageResponse`。
- 参数校验使用 `@Valid`、`@NotBlank`、`@NotNull`、`@DecimalMin`、`@Digits`、`@Min`。
- Swagger 注解已覆盖用户计费规则接口和管理员计费规则接口。

测试结果：

- 已执行 `mvn clean test`。
- 结果：108 个测试全部通过。
- 覆盖范围：计费计算 Service、计费规则 Service、用户计费规则 Controller、管理员计费规则 Controller、停车出场费用写入，以及既有用户、车辆、车位、预约、停车记录测试。

### 7.9 Phase 2.7：Dashboard 统计模块实现记录

完成时间：

- 2026-06-08

已实现功能：

- 今日收入统计。
- 今日停车次数统计。
- 当前车位占用率统计。
- 最近 7 天收入趋势。
- 最近 30 天收入趋势。
- 最近 7 天车流量趋势。
- 最近 30 天车流量趋势。
- Redis 缓存 Dashboard 统计数据。

后端交付：

- VO：`DashboardStatisticsVO`、`TrendPointVO`
- Service：`DashboardService`
- ServiceImpl：`DashboardServiceImpl`
- Controller：`AdminDashboardController`

接口权限：

- Dashboard 统计接口使用 `@PreAuthorize("hasRole('ADMIN')")`。

接口：

- `GET /api/v1/admin/dashboard/statistics`

缓存变更：

- 新增 Redis Key：`parking:dashboard:statistics`
- TTL：5 分钟
- Redis 读取或写入失败时回源 MySQL 计算。

数据库变更：

- 本阶段不新增数据库表。
- 复用 `parking_records` 和 `parking_spaces`。
- 今日收入和收入趋势使用 `parking_records.status`、`parking_records.exit_time`、`parking_records.fee`。
- 今日停车次数和车流量趋势使用 `parking_records.entry_time`。
- 当前车位占用率使用 `parking_spaces.status`。

验收结果：

- 今日收入按当天出场完成记录费用汇总。
- 今日停车次数按当天入场记录数量统计。
- 当前车位占用率按 `OCCUPIED / total * 100` 计算并保留两位小数。
- 最近 7 天收入趋势返回 7 个日期数据点。
- 最近 30 天收入趋势返回 30 个日期数据点。
- 最近 7 天车流量趋势返回 7 个日期数据点。
- 最近 30 天车流量趋势返回 30 个日期数据点。
- Dashboard 统计结果写入 Redis 并设置 5 分钟 TTL。
- 管理员可访问 Dashboard 接口。
- 普通用户和匿名用户不可访问 Dashboard 接口。

测试结果：

- 已执行 `mvn clean test`。
- 结果：114 个测试全部通过。
- 覆盖范围：Dashboard Service 缓存命中、回源统计、占用率计算、趋势长度、Redis 5 分钟 TTL、Dashboard Controller 权限控制，以及既有用户、车辆、车位、预约、停车记录、计费测试。

## 8. 阶段 5：百度 OCR 识别与日志审计

### 8.1 目标

实现车牌识别模块，直接接入百度智能云车牌识别 API，支持自动入场、自动出场和识别日志审计。

### 8.2 功能范围

- 定义 `PlateRecognitionService` 策略接口。
- 实现 `BaiduPlateRecognitionService`。
- 通过百度 `API Key` 和 `Secret Key` 获取 `access_token`。
- 缓存百度 `access_token`，避免每次识别都重新获取。
- 将上传图片转换为百度 API 所需的 Base64 `image` 参数。
- 调用百度车牌识别接口：`https://aip.baidubce.com/rest/2.0/ocr/v1/license_plate`。
- 解析百度返回的车牌号、车牌颜色、`log_id` 和字符置信度。
- OCR 入场接口。
- OCR 出场接口。
- 识别日志记录。
- 管理员查询识别日志。
- 识别失败处理。
- 低置信度处理。
- 百度 API 错误码映射为系统业务异常。

### 8.3 独立运行说明

运行依赖：

- 阶段 4 入场出场和计费能力。
- MySQL。
- Redis。
- 百度智能云车牌识别 API。
- 环境变量：`BAIDU_OCR_API_KEY`、`BAIDU_OCR_SECRET_KEY`。

可独立验证：

- 上传图片后调用百度 API 识别车牌。
- 自动执行入场流程。
- 自动执行出场流程。
- 识别日志可查询。

### 8.4 验收标准

- OCR 策略接口与业务流程解耦。
- 默认生产实现为 `BaiduPlateRecognitionService`。
- 百度密钥通过环境变量或部署密钥注入，不写入代码仓库。
- 百度 `access_token` 正确获取并缓存。
- 百度 API 调用失败时返回明确业务错误。
- OCR 入场成功后创建停车记录。
- OCR 出场成功后计算费用并释放车位。
- 识别成功和失败都写入 `plate_recognition_logs`。
- 识别日志记录百度 `log_id`、车牌颜色、供应商、错误码和错误信息。
- 低置信度识别结果不直接自动放行。
- 百度配置缺失时应用启动或健康检查能明确提示配置问题。
- 上传文件大小和类型有限制。

### 8.5 测试方案

单元测试：

- 百度 Token 响应解析。
- 百度车牌识别成功响应解析。
- 百度车牌识别失败响应解析。
- 字符置信度平均值计算。
- 低置信度判断。
- 百度错误码到系统异常的映射。

接口测试：

- OCR 入场成功。
- OCR 出场成功。
- 无法识别车牌返回业务错误。
- 百度 API 调用失败返回业务错误。
- 百度配置缺失返回明确错误。
- 上传非图片文件失败。
- 上传超大文件失败。
- 查询识别日志成功。

集成测试：

- 使用 Mock HTTP 服务模拟百度 Token 和车牌识别接口，验证 OCR 入场完整流程。
- 使用 Mock HTTP 服务模拟百度 Token 和车牌识别接口，验证 OCR 出场完整流程。
- 识别日志与停车记录关联校验。

外部服务冒烟测试：

- 在测试环境配置百度 OCR 密钥。
- 使用测试车辆图片调用一次真实百度车牌识别 API。
- 验证返回车牌号、置信度和日志落库。
- 冒烟测试不作为默认 CI 必跑项，避免依赖外部网络和账户额度。

### 8.6 数据库变更

新增表：

- `plate_recognition_logs`

新增索引：

- `idx_plate_logs_plate_number`
- `idx_plate_logs_recognition_time`
- `idx_plate_logs_camera_type`
- `idx_plate_logs_result`

数据变更：

- 停车记录可由人工流程或 OCR 流程创建。
- 操作日志记录 OCR 自动入场和自动出场事件。

### 8.7 Phase 2.8：OCR 车牌识别模块实现记录

完成时间：

- 2026-06-08

已实现功能：

- 图片上传识别。
- OCR 自动入场。
- OCR 自动出场。
- 管理员分页查询识别日志。
- 接入百度车牌识别 API。
- 通过百度 `API Key` 和 `Secret Key` 获取 `access_token`。
- 使用 Redis 缓存百度 `access_token`。
- 将上传图片转换为 Base64 后提交百度接口。
- 解析百度返回的车牌号、车牌颜色、`log_id` 和字符置信度。
- 低置信度识别结果写日志但不自动入场或出场。
- 识别失败写日志并返回业务错误。
- 自动入场优先使用指定车位，其次使用有效预约车位，最后自动选择第一个 `FREE` 车位。
- 自动出场按车牌查询进行中停车记录，计算费用并释放车位。

后端交付：

- Entity：`PlateRecognitionLog`
- DTO：`PlateRecognitionLogQuery`
- VO：`PlateRecognitionVO`、`PlateRecognitionLogVO`
- Mapper：`PlateRecognitionLogMapper`
- Convert：`PlateRecognitionLogConvert`
- Service：`PlateRecognitionService`、`PlateRecognitionApplicationService`
- ServiceImpl：`BaiduPlateRecognitionService`、`PlateRecognitionApplicationServiceImpl`
- Client：`BaiduOcrClient`
- Config：`BaiduOcrProperties`、`RecognitionConfig`
- Controller：`AdminPlateRecognitionController`
- 策略预留：`PaddlePlateRecognitionService`、`AliyunPlateRecognitionService`

接口权限：

- 车牌识别接口使用 `@PreAuthorize("hasRole('ADMIN')")`。

接口：

- `POST /api/v1/admin/plate-recognition/recognize`
- `POST /api/v1/admin/plate-recognition/entry`
- `POST /api/v1/admin/plate-recognition/exit`
- `GET /api/v1/admin/plate-recognition/logs`

数据库变更：

- 新增 SQL 脚本：`backend/src/main/resources/db/sql/V2_8__plate_recognition_logs.sql`
- 新增表：`plate_recognition_logs`
- 新增外键：`fk_plate_logs_record_id`
- 新增索引：`idx_plate_logs_record_id`、`idx_plate_logs_plate_number`、`idx_plate_logs_recognition_time`、`idx_plate_logs_camera_type`、`idx_plate_logs_provider`、`idx_plate_logs_provider_log_id`、`idx_plate_logs_result`

配置变更：

- `BAIDU_OCR_API_KEY`
- `BAIDU_OCR_SECRET_KEY`
- `BAIDU_OCR_LICENSE_PLATE_URL`
- `BAIDU_OCR_TOKEN_URL`
- `BAIDU_OCR_CONFIDENCE_THRESHOLD`

缓存变更：

- 新增 Redis Key：`baidu:ocr:access-token`
- TTL：百度 `expires_in - 60s`，最低 60 秒。

验收结果：

- 默认生产策略为 `BaiduPlateRecognitionService`。
- OCR 策略接口与业务流程解耦。
- 百度密钥通过环境变量注入，不写入代码仓库。
- 识别成功、识别失败、低置信度都会写入 `plate_recognition_logs`。
- OCR 入场成功后创建 `PARKING` 停车记录并关联日志。
- OCR 出场成功后更新停车记录为 `FINISHED`，写入费用并关联日志。
- 管理员可查询识别日志。
- 普通用户和匿名用户不可访问 OCR 接口。

测试结果：

- 已执行 `mvn clean test`。
- 结果：128 个测试全部通过。
- 覆盖范围：OCR Service 图片识别、自动入场、自动出场、低置信度、识别失败、日志分页，OCR Controller 上传接口和权限控制，停车记录自动入场车位选择，以及既有用户、车辆、车位、预约、停车记录、计费、Dashboard 测试。

暂未实现：

- 上传文件大小和图片 MIME 类型白名单校验。
- 使用 Mock HTTP 服务覆盖 `BaiduOcrClient` 的真实 HTTP 解析流程。
- 上述能力作为后续安全加固和外部服务集成测试补充。

## 9. 阶段 6：前端管理端与用户端

### 9.1 目标

实现 Vue 3 前端应用，覆盖管理员和车主用户核心操作页面，并通过 ECharts 展示统计分析。

### 9.2 功能范围

基础能力：

- Vite + Vue 3 + TypeScript 工程。
- Element Plus 组件体系。
- Vue Router。
- Pinia。
- Axios 封装。
- Token 存储和请求拦截。
- 路由权限守卫。

用户端页面：

- 注册。
- 登录。
- 个人信息。
- 修改密码。
- 我的车辆。
- 车位查询。
- 在线预约。
- 预约记录。
- 停车记录。
- 停车费用。

管理端页面：

- Dashboard。
- 用户管理。
- 车辆管理。
- 车位管理。
- 预约管理。
- 入场管理。
- 出场管理。
- 计费规则管理。
- 车牌识别日志。
- 操作日志。

图表：

- 最近 7 天收入趋势。
- 最近 30 天收入趋势。
- 最近 7 天车流量。
- 最近 30 天车流量。
- 车位占用率饼图。
- 区域车位分布图。
- 车牌识别成功率统计图。

### 9.3 独立运行说明

运行依赖：

- 阶段 5 后端服务。
- Vite 前端服务。

可独立验证：

- 前端登录。
- 不同角色进入不同菜单。
- 管理员完成车位、预约、入出场和统计查看。
- 用户完成车辆、车位查询、预约、记录查询。

### 9.4 验收标准

- 前端项目可启动。
- 登录后 Token 自动附加到请求头。
- Token 过期后跳转登录页。
- 普通用户看不到管理员菜单。
- 用户端核心流程可操作。
- 管理端核心流程可操作。
- 车位状态按颜色展示：绿色、橙色、红色。
- 表格支持分页和筛选。
- 表单有前端校验。
- API 错误有统一提示。
- Dashboard 图表渲染正常。
- 页面在常见桌面尺寸下布局稳定。

### 9.5 测试方案

单元测试：

- Axios 拦截器。
- Pinia 用户状态。
- 权限路由判断。
- 常用格式化函数。

组件测试：

- 登录表单校验。
- 车辆表单校验。
- 预约表单校验。
- 车位状态展示。

端到端测试：

- 用户注册、登录、添加车辆、预约、取消预约。
- 管理员登录、维护车位、人工入场、人工出场。
- Dashboard 页面加载并显示图表。

兼容性测试：

- Chrome 最新版。
- Edge 最新版。
- 常见 1366x768 和 1920x1080 桌面尺寸。

### 9.6 数据库变更

不新增核心业务表。

可能新增或调整：

- 为 Dashboard 查询补充复合索引。
- 根据慢查询结果优化 `parking_records`、`reservations`、`plate_recognition_logs` 索引。

## 10. 阶段 7：部署与交付文档

### 10.1 目标

完成系统容器化部署、反向代理配置、接口文档、测试集合和项目说明，使系统具备交付能力。

### 10.2 功能范围

- 后端 Dockerfile。
- 前端 Dockerfile。
- Docker Compose。
- MySQL 初始化脚本。
- Redis 配置。
- Nginx 配置。
- Swagger / OpenAPI 文档确认。
- Postman 测试集合。
- Linux 部署说明。
- 项目运行说明。
- 项目答辩文档。

### 10.3 独立运行说明

运行依赖：

- Docker。
- Docker Compose。

可独立验证：

- 一条 Docker Compose 命令启动完整系统。
- 浏览器访问前端。
- Nginx 反向代理后端 API。
- Swagger 可访问。
- Postman 集合可执行。

### 10.4 验收标准

- Docker Compose 可启动 MySQL、Redis、后端、前端、Nginx。
- 服务重启后数据不丢失。
- Nginx 正确代理 `/api`。
- 前端静态资源可访问。
- Swagger 文档与实际接口一致。
- Postman 集合覆盖核心接口。
- 部署文档包含环境要求、启动步骤、停止步骤、日志查看、常见问题。
- 生产配置不包含明文敏感信息。

### 10.5 测试方案

部署测试：

- 全量构建镜像。
- Docker Compose 启动。
- 服务健康检查。
- Nginx 访问测试。

接口回归：

- 执行 Postman 集合。
- 验证认证、车辆、车位、预约、入出场、OCR、Dashboard 接口。

数据持久化测试：

- 创建用户和车位。
- 重启容器。
- 验证数据仍存在。

安全检查：

- 检查配置文件是否包含明文生产密码。
- 检查管理接口是否必须登录。
- 检查跨域配置是否符合部署域名。

### 10.6 数据库变更

不新增业务表。

交付内容：

- 初始化数据库脚本。
- 初始管理员账号脚本。
- 默认计费规则脚本。
- 示例车位数据脚本。

## 11. 阶段 8：AI 智能问答与建议反馈

Phase 2.9 交付阶段。

### 11.1 目标

在现有停车业务基础上增加 AI 智能问答与用户建议闭环，让用户可以用自然语言查询业务数据、获取流程指引并提交建议，管理员可以统一处理建议并审计问答记录。

### 11.2 功能范围

AI 智能助手：

- 智能问答接口 `POST /api/v1/ai/chat`。
- 意图识别：车位、车辆、预约、停车记录、计费、预约流程、出入场流程、建议引导、系统帮助。
- 实时业务数据上下文：车辆、生效预约、最近停车记录、车位状态、当前计费规则。
- 双引擎策略：内置规则引擎（默认）与 OpenAI 兼容大模型（可选）。
- 外部大模型失败自动降级到规则引擎。
- 推荐问题接口 `GET /api/v1/ai/faq`。
- 会话与消息持久化：`ai_chat_sessions`、`ai_chat_messages`。
- 用户会话列表、会话消息、会话删除。
- 管理员会话审计：分页查询全部会话、查看会话消息。

用户建议：

- 提交建议 `POST /api/v1/suggestions`。
- 个人建议分页查询与详情。
- 管理员建议分页筛选、详情查看。
- 管理员处理建议并回复用户 `PUT /api/v1/admin/suggestions/{suggestionId}`。
- 建议状态机校验：`PENDING` → `PROCESSING`/`RESOLVED`/`REJECTED`；`PROCESSING` → `RESOLVED`/`REJECTED`；终态不可回退，终态必须填写回复。

### 11.3 独立运行说明

运行依赖：

- MySQL
- Redis
- Spring Boot 后端

可独立验证：

- 默认 `rule-based` 引擎不依赖任何外部服务，登录后即可进行问答。
- 配置 `AI_PROVIDER=openai` 与 `AI_API_KEY` 后启用 OpenAI 兼容大模型；未配置或调用失败时自动使用规则引擎。
- 用户提交建议后，管理员可在管理端查看并处理，用户在意见反馈页面查看回复。

### 11.4 验收标准

- AI 模块具备 Entity、DTO、VO、Mapper、Service、ServiceImpl、Controller、Convert。
- 所有接口具备 `@PreAuthorize`、Jakarta Validation 参数校验和 Swagger 注解。
- 未登录访问返回 401，普通用户访问管理接口返回 403。
- 用户不能访问或删除他人会话与建议。
- 问答回答引用实时业务数据，未知问题有兜底回答。
- 建议状态机与终态回复校验生效。
- Service 与 Controller 单元测试通过。
- `mvn clean test`、`mvn clean package` 通过，前端 `npm run build` 通过。
- 前端页面可用：AI 智能助手、意见反馈、用户建议管理、AI 对话记录。

### 11.5 测试方案

后端单元测试：

- `AiIntentResolverTest`：意图识别与兜底。
- `RuleBasedAiChatProviderTest`：规则引擎回答与引用。
- `AssistantContextBuilderTest`：实时数据上下文组装。
- `AssistantServiceImplTest`：会话创建、复用、越权访问、删除。
- `SuggestionServiceImplTest`：创建、状态流转、终态校验、越权访问。
- Controller 测试：问答成功、空问题校验、未登录、越权、建议创建与处理。

验收命令：

```bash
mvn clean test
mvn clean package
cd frontend && npm run build
```

### 11.6 数据库变更

- 新增 `ai_chat_sessions`。
- 新增 `ai_chat_messages`。
- 新增 `user_suggestions`。
- 迁移脚本：`backend/src/main/resources/db/sql/V2_9__ai_assistant.sql`。
- 不新增 Redis 缓存，不修改既有表结构。

### 11.7 配置变更

新增环境变量：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `AI_PROVIDER` | `rule-based` | 回答引擎，支持 `rule-based` 与 `openai` |
| `AI_BASE_URL` | `https://api.openai.com/v1` | OpenAI 兼容接口地址 |
| `AI_API_KEY` | 空 | 大模型密钥，未配置时使用规则引擎 |
| `AI_MODEL` | `gpt-4o-mini` | 模型名称 |
| `AI_TIMEOUT_SECONDS` | `20` | 外部接口超时时间 |
| `AI_MAX_HISTORY_MESSAGES` | `6` | 历史消息条数 |
| `AI_TEMPERATURE` | `0.3` | 采样温度 |

`application.yml`、`.env.example`、`docker-compose.yml` 已同步。

### 11.8 交付文件清单

后端：

- `backend/src/main/java/com/smartparking/ai/**`
- `backend/src/main/resources/db/sql/V2_9__ai_assistant.sql`
- `backend/src/test/java/com/smartparking/ai/**`

前端：

- `frontend/src/views/user/UserAiAssistantView.vue`
- `frontend/src/views/user/UserSuggestionsView.vue`
- `frontend/src/views/admin/AdminSuggestionsView.vue`
- `frontend/src/views/admin/AdminAiChatsView.vue`
- `frontend/src/api/types.ts`、`frontend/src/api/modules.ts`
- `frontend/src/router/index.ts`、`frontend/src/views/layouts/AppLayout.vue`
- `frontend/src/utils/format.ts`

文档：

- `docs/api-design.md` 第 16、17 节。
- `docs/database-design.md` 第 5.9、5.10、5.11 节。
- `docs/development-plan.md` 本节。

### 11.9 Phase 2.10：AI 提示词模板与 Python RAG 服务

目标：

- 将 AI 助手的系统提示词从代码外部化到 `ai_prompt_templates` 表，支持管理员按业务意图在线维护。
- 新增独立的 Python FastAPI RAG 服务，基于知识库检索 + 运行时上下文生成回答。
- 后端新增 `python` provider，将问答路由到 Python 服务，并在调用失败时自动降级回规则引擎。
- 通过 Nginx 与 Docker Compose 完成 Python 服务的部署接入。

功能范围：

- 提示词模板管理：管理员列表/详情/更新接口 `GET /api/v1/admin/ai/prompts`、`GET/PUT /api/v1/admin/ai/prompts/{templateId}`；`AiPromptTemplateService.resolveSystemPrompt` 按“意图启用模板 → `default_assistant` → 内置默认”顺序解析系统提示词。
- Python RAG 服务：`ai-service` 提供 `GET /health`、`GET /v1/prompts`、`POST /v1/rag/search`、`POST /v1/chat`；markdown 知识库分块 + BM25 词法检索 + 实时上下文合并 + 可选大模型/抽取式回答。
- Java python provider：`PythonAiChatProvider` 以 `RestClient` 调用 Python 服务 `/v1/chat`；`AiChatProviderRouter` 按 `AI_PROVIDER` 选择 provider，任一异常回退到 `rule-based`。
- 部署接入：Nginx `location /ai-api/` 反向代理到 `http://ai-service:8000/`，除 `/ai-api/health` 运维探针外，其余路径经 `auth_request` 复用后端 `/api/v1/users/me` 校验 JWT；Compose 新增 `ai-service` 服务及健康检查，宿主机端口仅绑定 `127.0.0.1`，避免绕过网关直连。
- 管理端页面：`AdminAiPromptsView.vue` 及导航入口、列表/编辑 API。

独立运行说明：

- 后端与前端沿用既有启动方式；`ai-service` 可单独构建运行：

```bash
cd ai-service
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
.venv/bin/uvicorn app.main:app --host 0.0.0.0 --port 8000
```

- 全量部署：先 `mvn clean package`，再 `docker compose --profile frontend up --build -d`。
- 仅启用 Python provider：`.env` 中设置 `AI_PROVIDER=python`、`AI_PYTHON_BASE_URL=http://ai-service:8000`（容器内）。
- 不启动 Python 服务时，后端 `isAvailable()` 判定不可用自动回退，规则引擎仍可正常问答。

验收标准：

- 管理员可查看/编辑提示词模板，非法参数（空白名称、空白提示词、空 `enabled`、超长字段）返回 400。
- `resolveSystemPrompt` 优先级正确：意图启用模板 > `default_assistant` > 内置默认；表为空不抛异常。
- `AI_PROVIDER=python` 时问答由 Python 服务回答（`provider` 为 `openai` 或 `extractive`）；服务不可用时接口仍返回 200 且 `provider=rule-based`。
- `curl http://localhost:8000/health` 返回 `status: ok`；经 Nginx 访问 `curl http://localhost:<FRONTEND_PORT>/ai-api/health` 同样成功。
- `docker compose config -q` 与 `nginx -t` 校验通过。

测试方案：

- 后端 AI 测试：`mvn -pl backend test -Dtest='com.smartparking.ai.**'`，覆盖 `AiPromptTemplateServiceImplTest`、`AdminAiPromptControllerTest`、`PythonAiChatProviderTest`。
- Python 服务测试：`cd ai-service && .venv/bin/python -m pytest -q`，覆盖提示词解析、检索相关性、API 契约与空字段容忍。
- 部署健康检查：`docker compose --profile frontend ps` 确认 `ai-service` 为 `healthy`；`curl http://localhost:8000/health`。

数据库变更：

- 新增迁移脚本 `backend/src/main/resources/db/sql/V2_10__ai_prompts.sql`，创建 `ai_prompt_templates` 表并写入 5 条幂等种子（`template_id` 2001–2005）。
- 不修改既有表，不新增外键，不新增 Redis 缓存。

配置变更：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `AI_PROVIDER` | `rule-based` | 新增 `python` 可选值 |
| `AI_PYTHON_BASE_URL` | `http://ai-service:8000` | Python RAG 服务地址 |
| `AI_PYTHON_TIMEOUT_SECONDS` | `30` | 调用 Python 服务超时 |
| `AI_SERVICE_PORT` | `8000` | Python 服务宿主机映射端口 |
| `AI_RAG_TOP_K` | `4` | 检索返回分块数 |
| `AI_RAG_EMBEDDING_ENABLED` | `false` | 预留向量检索开关，当前为 BM25 词法检索 |

交付文件清单：

后端：

- `backend/src/main/java/com/smartparking/ai/**`（提示词服务/控制器/provider 等）
- `backend/src/main/resources/db/sql/V2_10__ai_prompts.sql`
- `backend/src/test/java/com/smartparking/ai/**`
- `backend/src/main/resources/application.yml`（`app.ai.python.*`）

Python 服务：

- `ai-service/app/**`、`ai-service/data/knowledge/**`、`ai-service/tests/**`
- `ai-service/Dockerfile`、`ai-service/requirements.txt`、`ai-service/pytest.ini`、`ai-service/README.md`

前端：

- `frontend/src/views/admin/AdminAiPromptsView.vue`
- `frontend/src/api/modules.ts`、`frontend/src/api/types.ts`
- `frontend/src/router/index.ts`、`frontend/src/views/layouts/AppLayout.vue`

部署与文档：

- `frontend/nginx.conf`、`docker-compose.yml`、`.env.example`、`.gitignore`
- `docs/api-design.md` 第 16.8、18 节。
- `docs/database-design.md` 第 5.12 节、第 6 节 SQL 草案、第 8/9 节。
- `docs/architecture.md` 第 6.10 节。
- `docs/docker-deployment.md` 环境变量与故障排查。
- `docs/development-plan.md` 本节。

## 12. 跨阶段质量要求

### 11.1 代码质量

后续编码阶段必须遵守：

- Controller 不写复杂业务逻辑。
- Service 负责事务和业务编排。
- Mapper 只处理数据库访问。
- DTO 和 VO 分离。
- Entity 不直接暴露给前端。
- 使用 MapStruct 做对象转换。
- 使用 Lombok 减少样板代码。
- 关键逻辑有单元测试。

### 11.2 数据一致性

涉及以下流程必须使用事务：

- 创建预约。
- 取消预约。
- 自动释放预约。
- 入场登记。
- 出场登记。
- 启用计费规则。

涉及车位状态变更必须同步处理：

- 数据库状态。
- 乐观锁版本。
- Redis 缓存。
- 操作日志。

### 11.3 安全要求

每个阶段必须保持：

- 密码 BCrypt 加密。
- JWT 认证。
- 角色鉴权。
- 参数校验。
- 统一异常。
- 管理操作审计。

### 11.4 性能要求

重点关注：

- 车位列表查询。
- 预约并发创建。
- Dashboard 聚合查询。
- 停车记录分页查询。
- 识别日志分页查询。

性能优化顺序：

1. 正确索引。
2. 分页查询。
3. Redis 缓存。
4. 慢查询分析。
5. 必要时做统计预聚合。

## 13. 后续执行规则

在用户确认前，仅完成文档与分析，不开始编码。

用户确认进入开发后，按阶段顺序执行：

1. 先实现当前阶段最小可运行版本。
2. 再补齐验收标准。
3. 再补测试。
4. 最后更新阶段文档和运行说明。

任何跨阶段新增需求都先评估：

- 是否影响数据库结构。
- 是否影响已有 API。
- 是否影响权限模型。
- 是否影响缓存一致性。
- 是否影响部署配置。

## 14. 安全加固与业务闭环迭代实现记录（2026-10-07）

审查发现两类系统性短板后完成的专项迭代：业务闭环（预约过期永不推进、计费封顶语义错误、多处并发竞态）与安全认证（refresh 机制形同虚设、改密不踢会话、登录可暴力破解、ADMIN 被角色挡住）。`mvn clean test`（250 个用例）与 `mvn clean package` 全部通过。

### 14.1 业务闭环修复

1. **预约过期定时任务**：新增 `ReservationExpirationService(Impl)` 与 `ReservationExpirationTask`（`@EnableScheduling`，默认 60 秒扫描、启动延迟 30 秒）。`arrive_time` 超过宽限期（`RESERVATION_EXPIRATION_GRACE_MINUTES`，默认 30 分钟）仍未入场的 ACTIVE 预约以 CAS（status 条件更新）推进为 `EXPIRED`，随后 CAS 释放车位 RESERVED→FREE 并刷新缓存；车位已被占用时仅记日志跳过。每条记录在独立事务（`TransactionTemplate`）中处理。入场校验同步补充：过期预约不可用于入场（`ParkingRecordServiceImpl`），自动入场选位时跳过过期预约，预约置 `USED` 也改为 CAS。
2. **maxDaily 按日封顶**：`FeeCalculatorService.calculate` 签名改为 `(entryTime, exitTime)`，按自然日拆分计费区间，免费时长整个会话只扣一次，每日 `min(ceil(当日计费分钟/60)×unitPrice, maxDaily)` 后累加——停 3 天收 3×maxDaily，不再被整段封顶少收。
3. **并发竞态 DB 兜底**：
   - `reservations` 增加生成列 `active_flag=IF(status='ACTIVE',1,NULL)` + 唯一键 `(user_id, active_flag)`；
   - `parking_records` 增加生成列 `active_flag=IF(status='PARKING',1,NULL)` + 唯一键 `(vehicle_id, active_flag)`；
   - `fee_rules` 增加函数唯一索引 `((IF(enabled=1,1,NULL)))`（MySQL 8.0.13+）；
   - `GlobalExceptionHandler` 新增 `DuplicateKeyException → 409`，并发冲突不再返回 500；
   - 生效计费规则读取增加 `update_time` 降序兜底排序。
4. **删除校验**：删除车辆前检查预约/停车记录引用（任一引用返回 409，杜绝 FK 500）；删除车位改为条件删除（status=FREE + version）并与入场占用并发互斥，同时检查引用。
5. **种子数据兼容**：`sql/init-data.sql` 现有数据满足新唯一约束，无需调整。

### 14.2 安全与认证加固

1. **双令牌机制**：`JwtService` 拆分 `generateAccessToken`/`generateRefreshToken`，token 携带 `typ`（access/refresh）、`jti`、`ver`（tokenVersion）claims；`jwt.refresh-expiration` 默认 7 天。
2. **refresh 轮换**：`/auth/refresh` 只接受 refresh token；成功后旧 refresh token 的 `jti` 写入 Redis 黑名单 `auth:jwt:deny:{jti}`（TTL=剩余有效期），不能重复使用。`LoginVO` 扩展为 `token + refreshToken + user`。
3. **登出**：新增 `POST /api/v1/auth/logout`，吊销 Authorization 头中的 access token 与可选的 refresh token。
4. **改密/禁用踢会话**：`users` 表新增 `token_version`；改密与管理员禁用用户时原子递增并清 Redis 版本缓存 `auth:tokenver:{userId}`（TTL 5 分钟）；`JwtAuthenticationFilter` 校验 token 版本，不匹配返回 401。Filter 对缺少 `typ`/`jti`/`ver` 的旧格式 token 保持兼容（仅签名校验），升级不强制全体重新登录。
5. **登录防暴力破解**：新增 `LoginRateLimiter`（Redis 固定窗口），同一用户名连续失败 5 次锁定 15 分钟，返回 `429 TOO_MANY_REQUESTS`；Redis 故障时降级放行。
6. **CORS**：`SecurityConfig` 注册 `CorsConfigurationSource`，来源配置化（`CORS_ALLOWED_ORIGINS`，默认 localhost:5173/3000）。
7. **角色层级**：`RoleHierarchy` bean（ADMIN 隐含 USER），管理员可访问全部个人业务接口，无需逐个改 `@PreAuthorize`。
8. **部署安全**：`docker-compose.yml` 的 `JWT_SECRET` 改为必填变量；新增 `.env.example`；`JwtService` 启动时校验 secret 长度（≥32 字符），检测到默认占位值输出 WARN 日志。

### 14.3 数据库变更（老库需手动 ALTER）

本项目未接入 Flyway，建表 SQL 仅首次建库生效。老库升级语句见 `docs/database-design.md` 5.13 节（users.token_version、两张表的生成列唯一约束、fee_rules 函数唯一索引）。

### 14.4 前端适配提醒（未改前端）

- 登录/刷新响应新增 `refreshToken` 字段（`token` 字段不变，旧逻辑仍可用）。
- 建议前端接入：401 时用 refresh token 调 `/auth/refresh` 续期；登出时调 `/auth/logout`。

## 15. 硬件接入规划（设计完成，未实现）

硬件接入（海康威视等停车相机）属于超出原 Phase 2.1~2.8 范围的扩展项。已完成对接设计：

- 设计文档：`docs/device-integration-design.md`
- 核心结论：硬件通道下识别在相机端完成（相机内置 LPR 算法），服务端职责变为"接收 ISAPI 事件推送 → 幂等/校验 → 复用 autoEntry/autoExit 业务管线 → 放行决策与道闸控制"。现有百度 OCR 通道保留为无硬件环境的人工补录通道。
- 阶段划分：P0 设备事件接入层地基（devices/device_events 表、免 JWT 回调端点、HIKVISION_ISAPI/GENERIC_JSON/SIMULATION 三种适配器、幂等与安全）→ P1 真机联调（抓真实报文核对字段映射）→ P2 道闸联动（GateControlService 海康 IO 实现）。
- 随附的既有模块加固项（车牌格式校验、OCR 客户端超时、multipart 限制、客户端日志）见设计文档第 12 节。

### 14.5 业务流程实测验证（2026-10-07）

搭建真实环境（MySQL/Redis/后端 jar + 老库执行 5.13 节迁移）对核心业务链路做了 31 项端到端实测，30 项通过、1 项为测试脚本断言过严（时长 179 分钟，费用计算正确）。实测通过项：注册/登录双令牌、refresh 轮换与旧令牌作废、车辆注册（车牌唯一 409）、预约创建与车位 RESERVED、重复预约 409、入场（车位 OCCUPIED + 预约 USED）、重复入场 409、占用中删车位/车辆 409、出场计费（180 分钟、免费 15 分钟、5 元/时 → 15.00 元）、出场释放车位、预约过期定时任务推进 EXPIRED + 释放车位 + 可再次预约闭环、Dashboard 统计、登录限流 429、改密踢会话、登出吊销。

实测过程中修复两个真实缺陷（`mvn clean test` 250 项全部回归通过）：

1. **Redis 缓存 record 读回 500**（流程阻断级）：`ParkingSpaceStatusVO` 等缓存值为 record（final 类），`GenericJackson2JsonRedisSerializer` 默认 NON_FINAL 多态序列化不写入 `@class`，导致缓存写入成功但读回反序列化失败，车位实时状态接口在命中缓存时必 500。修复：`RedisConfig` 改用 `BasicPolymorphicTypeValidator` 白名单 + `DefaultTyping.EVERYTHING`（同时收紧了原 `LaissezFaireSubTypeValidator` 的多态反序列化安全隐患）；`ParkingSpaceServiceImpl.getRealtimeStatus` 缓存读失败时降级回数据库并清缓存。
2. **全局异常处理器兜底分支无日志**：500 错误完全静默，线上无法排障。修复：兜底 `handleException` 增加 `log.error`。实测中曾出现"注册 500 但日志零痕迹"，正是该缺陷的实证。

遗留发现（未修复，待后续迭代）：

- `HttpMessageNotReadableException`（畸形 JSON）与 `HttpRequestMethodNotSupportedException`（方法不匹配）落入兜底返回 500，应分别映射 400/405（`GlobalExceptionHandler`）。
- OCR 自动入场要求车辆已登记，未登记的访客车牌会直接 NOT_FOUND，访客通道需产品层面决策（与硬件接入设计一并考虑）。
- 计费规则可被全部禁用（无"至少保留一条启用"守卫），禁用后所有出场会因无规则而失败，建议加校验或兜底默认规则。

### 14.6 冗余代码清理（2026-10-07）

依据框架审计报告（docs/framework-audit.md）清理确认无引用的冗余项，250 个单元测试回归通过：

- 移除 `pom.xml` 中零引用的 lombok（依赖、注解处理器、打包排除配置三处）与 hutool-all（5.8.34 全量包），产物 jar 不再包含 hutool；
- 移除 `application.yml` 中 `logic-delete-field: deleted` 等 MP 逻辑删除死配置（全部表无 deleted 列、实体无映射，从未生效）；
- 删除 `application-test.yml`（H2 连接配置，pom 无 H2 驱动且从未被任何测试加载）。

保留项说明：`PaddlePlateRecognitionService`/`AliyunPlateRecognitionService` 两个占位类虽未注册为 Bean，但属于 AGENTS.md Phase 2.8 要求的策略模式预留扩展点，予以保留。
