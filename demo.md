# 企业级智慧停车场管理系统（Smart Parking Management System)

## 项目背景

你是一名拥有15年以上经验的资深系统架构师、Java技术专家、全栈开发工程师和产品经理。

请按照企业级项目开发标准，设计并开发一套完整的《智慧停车场管理系统（Smart Parking Management System）》。

该系统面向中小型停车场运营场景，采用前后端分离架构，实现：

- 用户管理
- 车辆管理
- 车位管理
- 在线预约
- 车牌自动识别
- 自动入场
- 自动出场
- 停车计费
- 数据统计分析
- 权限管理
- 日志审计
- 实时状态监控

系统需具备高可用、高并发、高扩展性和企业级代码规范。

------

# 一、技术架构要求

## 后端技术栈

- Java 17
- Spring Boot 3.x
- Spring Security
- JWT
- MyBatis-Plus
- MySQL 8
- Redis
- Maven
- Lombok
- Swagger/OpenAPI 3
- Quartz 或 Spring Schedule
- MapStruct
- Hutool

## 前端技术栈

- Vue 3
- Vite
- TypeScript
- Pinia
- Vue Router
- Axios
- Element Plus
- ECharts

## 部署环境

- Linux Ubuntu 22+
- Docker
- Docker Compose
- Nginx

------

# 二、系统角色设计

## 管理员（ADMIN）

功能权限：

- 登录
- 修改密码
- 用户管理
- 车辆管理
- 车位管理
- 预约管理
- 入场管理
- 出场管理
- 计费规则管理
- 数据统计分析
- 车牌识别日志管理
- 系统日志管理

------

## 车主用户（USER）

功能权限：

- 注册
- 登录
- 修改个人信息
- 修改密码
- 添加车辆
- 查询车位
- 在线预约
- 查看预约记录
- 查看停车记录
- 查看停车费用
- 查看个人车辆信息

------

# 三、核心业务模块

## 1. 用户管理模块

功能：

- 用户注册
- 用户登录
- JWT认证
- 修改密码
- 修改个人资料
- 用户状态管理
- 角色管理

密码必须使用：

BCryptPasswordEncoder

------

## 2. 车辆管理模块

功能：

- 添加车辆
- 编辑车辆
- 删除车辆
- 查询车辆

字段：

- vehicleId
- plateNumber
- vehicleType
- userId

要求：

车牌号全局唯一。

------

## 3. 车位管理模块

功能：

- 新增车位
- 修改车位
- 删除车位
- 查询车位

字段：

- spaceId
- area
- type
- status
- version

车位状态：

FREE

RESERVED

OCCUPIED

颜色显示：

绿色：

FREE

橙色：

RESERVED

红色：

OCCUPIED

使用：

MyBatis Plus Optimistic Locker

实现乐观锁。

------

## 4. 在线预约模块

功能：

用户可预约空闲车位。

预约流程：

用户选择车位

↓

选择车辆

↓

填写预计到达时间

↓

提交预约

↓

锁定车位

↓

生成预约记录

预约状态：

ACTIVE

USED

EXPIRED

CANCELLED

要求：

- 防止超卖
- 防止重复预约
- 乐观锁控制并发
- Redis缓存车位状态

------

## 5. 自动释放预约模块

定时任务：

每1分钟执行一次。

逻辑：

查询ACTIVE预约

↓

判断是否超时

↓

更新为EXPIRED

↓

释放车位

↓

更新Redis缓存

------

## 6. 人工登记模块

管理员功能：

### 入场登记

录入：

- 车牌号
- 车辆类型
- 分配车位

自动：

- 生成停车记录
- 更新车位状态

------

### 出场登记

输入：

车牌号

自动：

- 计算停车时间
- 计算停车费用
- 更新停车记录
- 释放车位

------

## 7. 智能车牌识别模块

### 功能目标

实现车辆自动识别。

支持：

- 自动入场
- 自动出场
- 自动匹配预约
- 自动计费

------

### 识别方案

优先实现：

OpenCV + PaddleOCR

同时预留：

- 百度OCR
- 阿里云OCR
- 腾讯云OCR
- 华为OCR

采用策略模式：

PlateRecognitionService

实现：

- PaddlePlateRecognitionService
- BaiduPlateRecognitionService
- AliyunPlateRecognitionService

------

### 入场识别流程

车辆到达入口

↓

摄像头抓拍

↓

上传图片

↓

OCR识别车牌

↓

查询预约

↓

分配车位

↓

创建停车记录

↓

更新车位状态

↓

记录识别日志

------

### 出场识别流程

车辆到达出口

↓

摄像头抓拍

↓

OCR识别

↓

查询停车记录

↓

计算费用

↓

更新停车记录

↓

释放车位

↓

记录识别日志

------

### 识别日志表

plate_recognition_logs

字段：

- id
- plateNumber
- confidence
- imageUrl
- recognitionTime
- cameraType
- result

------

## 8. 停车计费模块

管理员可配置：

- 首免时长
- 单位价格
- 日封顶价格

字段：

- ruleId
- ruleName
- unitPrice
- freeMinute
- maxDaily

计费公式：

停车时长 ≤ freeMinute

费用 = 0

否则：

fee = min(
ceil((duration-freeMinute)/60)
× unitPrice,
maxDaily
)

保留两位小数。

------

## 9. 数据统计分析模块

Dashboard展示：

### 实时统计

- 总车位数
- 空闲车位数
- 已预约车位数
- 占用车位数
- 今日收入
- 今日停车次数
- 当前停车车辆数

------

### 图表统计

使用ECharts：

1. 最近7天收入趋势图
2. 最近30天收入趋势图
3. 最近7天车流量统计图
4. 最近30天车流量统计图
5. 车位占用率饼图
6. 区域车位分布图
7. 车牌识别成功率统计图

------

# 四、数据库设计

请输出完整MySQL数据库设计。

包括：

## users

字段：

- userId
- username
- password
- name
- phone
- role
- status
- createTime

------

## vehicles

字段：

- vehicleId
- plateNumber
- vehicleType
- userId

------

## parking_spaces

字段：

- spaceId
- area
- type
- status
- version

------

## reservations

字段：

- reservationId
- userId
- vehicleId
- spaceId
- reserveTime
- arriveTime
- status

------

## parking_records

字段：

- recordId
- vehicleId
- spaceId
- entryTime
- exitTime
- duration
- fee
- status

------

## fee_rules

字段：

- ruleId
- ruleName
- unitPrice
- freeMinute
- maxDaily

------

## plate_recognition_logs

字段：

- id
- plateNumber
- confidence
- imageUrl
- recognitionTime
- cameraType
- result

------

## operation_logs

字段：

- id
- operator
- operationType
- operationContent
- operationTime

------

# 五、缓存设计

Redis缓存：

缓存内容：

- 车位状态
- 仪表盘统计数据
- 用户Token
- 热门查询数据

缓存Key设计：

parking:space:{id}

parking:dashboard

parking:user:{id}

parking:reservation:{id}

------

# 六、安全设计

必须实现：

## Spring Security

角色：

ROLE_ADMIN

ROLE_USER

------

## JWT认证

包含：

- 用户ID
- 用户角色
- 过期时间

------

## 安全防护

实现：

- SQL注入防护
- XSS防护
- CSRF防护
- 接口限流
- 重复提交校验

------

# 七、接口规范

采用RESTful风格。

统一返回结构：

{
"code": 200,
"message": "success",
"data": {}
}

统一异常处理：

GlobalExceptionHandler

统一日志处理：

AOP + OperationLog

------

# 八、系统扩展设计

预留接口：

## 第三方扩展

- 微信支付
- 支付宝支付
- ETC支付
- 电子发票

------

## 硬件扩展

- 道闸控制器
- LED显示屏
- RFID识别
- 摄像头设备

------

## 通知扩展

- 短信通知
- 邮件通知
- 微信消息推送

------

# 九、项目输出要求

请按照企业级开发流程分阶段输出：

第一阶段：

- 系统需求分析
- 系统架构设计
- UML用例图
- ER图
- 数据库设计

第二阶段：

- Spring Boot完整后端代码
- Entity
- DTO
- VO
- Mapper
- Service
- Controller
- Security配置

第三阶段：

- Vue3完整前端代码
- 页面设计
- 路由设计
- API封装
- 状态管理

第四阶段：

- Redis缓存实现
- JWT实现
- 定时任务实现
- OCR识别实现

第五阶段：

- Docker部署
- Docker Compose配置
- Nginx配置
- Linux部署文档

第六阶段：

- Swagger接口文档
- Postman测试集合
- 项目运行说明
- 项目答辩文档

所有代码必须符合企业级开发规范，具备良好的可读性、扩展性、可维护性和安全性。
