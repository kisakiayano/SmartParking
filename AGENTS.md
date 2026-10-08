# Phase 2：业务模块开发规范

请严格按照以下规则开发业务模块。

## 通用要求

每开发一个模块，必须完成：

### 1. 数据库层

输出：

* 表结构SQL
* 索引设计
* 外键关系说明

### 2. 后端代码

必须生成：

* Entity
* DTO
* VO
* Mapper
* Service
* ServiceImpl
* Controller
* Convert(MapStruct)

### 3. 接口文档

输出：

* 接口路径
* 请求方式
* 请求参数
* 返回参数
* Swagger注解

### 4. 权限控制

所有接口必须添加：

```java
@PreAuthorize(...)
```

符合RBAC权限模型。

### 5. 参数校验

使用：

```java
@Valid
```

```java
@NotNull
```

```java
@NotBlank
```

```java
@Pattern
```

进行校验。

### 6. 单元测试

生成：

* Service测试
* Controller测试

### 7. 验收要求

完成后执行：

mvn clean test

mvn clean package

确保编译通过。

开发完成后更新：

docs/api-design.md

docs/database-design.md

docs/development-plan.md

Phase 2.1 用户认证模块
开始开发用户认证模块。

实现功能：

1. 用户注册
2. 用户登录
3. JWT签发
4. JWT刷新
5. 获取当前用户信息
6. 修改密码
7. 修改个人资料

数据库表：

users

要求：

1. Spring Security + JWT
2. BCryptPasswordEncoder
3. DTO与VO分离
4. Swagger文档
5. 单元测试
6. RBAC权限控制

开发完成后：

执行测试并更新文档。

Phase 2.2 车辆管理模块

开始开发车辆管理模块。

数据库表：

vehicles

功能：

1. 新增车辆
2. 修改车辆
3. 删除车辆
4. 查询车辆
5. 分页查询
6. 根据用户查询车辆

规则：

1. 车牌号唯一
2. 用户只能管理自己的车辆
3. 管理员可查看全部车辆

完成后：

执行测试并更新文档。

Phase 2.3 车位管理模块
开始开发车位管理模块。

数据库表：

parking_spaces

功能：

1. 新增车位
2. 修改车位
3. 删除车位
4. 查询车位
5. 按区域筛选
6. 按状态筛选

状态：

FREE
RESERVED
OCCUPIED

要求：

1. 使用MyBatis Plus乐观锁
2. version字段启用
3. Redis缓存车位状态
4. 提供实时状态查询接口

缓存Key：

parking:space:{spaceId}

完成后执行测试并更新文档。

Phase 2.4 在线预约模块
开始开发预约模块。

数据库表：

reservations

功能：

1. 创建预约
2. 查询预约
3. 取消预约
4. 查看个人预约记录
5. 管理员查看全部预约

预约状态：

ACTIVE
USED
EXPIRED
CANCELLED

业务规则：

1. 同一车位不能重复预约
2. 同一用户同时只能存在一个ACTIVE预约
3. 创建预约时必须校验车位状态
4. 使用乐观锁防止超卖
5. 更新Redis缓存

完成后：

编写并发测试验证预约安全性。

Phase 2.5 停车记录模块
开始开发停车记录模块。

数据库表：

parking_records

功能：

1. 人工登记入场
2. 人工登记出场
3. 查询停车记录
4. 查询个人停车记录
5. 分页查询

规则：

入场：

创建停车记录

状态：

PARKING

出场：

更新状态：

FINISHED

同时释放车位。

完成后执行测试。

Phase 2.6 计费模块
开始开发计费模块。

数据库表：

fee_rules

功能：

1. 新增计费规则
2. 修改计费规则
3. 查询计费规则
4. 自动计算停车费用

规则：

停车时间 <= freeMinute

费用 = 0

否则：

fee = min(
ceil((duration-freeMinute)/60)
× unitPrice,
maxDaily
)

要求：

封装独立FeeCalculatorService。

必须编写计算单元测试。

Phase 2.7 Dashboard统计模块
开始开发统计分析模块。

功能：

1. 今日收入统计
2. 今日停车次数统计
3. 当前车位占用率统计
4. 最近7天收入趋势
5. 最近30天收入趋势
6. 最近7天车流量趋势
7. 最近30天车流量趋势

输出：

DashboardStatisticsVO

要求：

Redis缓存统计数据。

缓存时间：

5分钟。

Phase 2.8 OCR车牌识别模块
开始开发OCR车牌识别模块。

数据库表：

plate_recognition_logs

实现：

PlateRecognitionService

接口：

1. 图片上传识别
2. 自动入场
3. 自动出场

优先实现：

BaiduOCR

同时预留：

PaddleOCR
AliyunOCR

采用策略模式实现。

识别结果记录到：

plate_recognition_logs

完成后编写接口测试。

