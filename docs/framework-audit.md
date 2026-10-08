# 智慧停车场管理系统 · 框架审计报告（代码层运行逻辑版）

> 审计基准：2026-10-07 代码库（含安全加固与业务闭环迭代后的版本）。
> 方法：三大模块全量源码走读（前端 30 文件 3692 行、Java 221 文件、Python 8 模块）+ 250 单元测试 + 真实环境 31 项接口实测与 14 项界面实测。
> 所有结论均附文件路径与方法名，可对照源码逐行验证。

---

## 1. 系统全景：三大模块划分

| 模块 | 技术 | 角色 | 代码位置 | 规模 |
|---|---|---|---|---|
| **前端 Vue 应用** | Vue3.5 + TS + Vite6 + Element Plus + Pinia + ECharts | 页面渲染、用户交互、路由与状态管理、请求封装 | `frontend/` | 30 文件 3692 行 |
| **后端 Java 主服务** | Spring Boot 3.3 + Spring Security/JWT + MyBatis Plus + Redis | 全部业务规则、认证授权、数据一致性、外部服务编排 | `backend/` | 221 文件 |
| **后端 Python 副服务** | FastAPI + httpx + 纯 Python BM25 + DeepSeek(OpenAI 协议) | 知识检索增强生成（RAG）、大模型调用、答案兜底 | `ai-service/` | 8 模块 3 测试文件 |

**模块间通信契约**（唯一的跨模块通道，全部经 nginx）：

```
浏览器 ──5173──▶ Nginx(frontend 容器)
    ├─ /api/v1/**          ──▶ Java 主服务 :8080     （REST + JWT）
    ├─ /ai-api/**          ──▶ Python 副服务 :8000    （nginx auth_request 先向 Java 校验 JWT）
    └─ 其余路径             ──▶ Vue 单页应用（静态资源）
Java 主服务 ──内网 HTTP──▶ Python 副服务 /v1/chat     （服务器间调用，绕过 nginx）
Java 主服务 ──外网 HTTPS─▶ 百度 OCR API
```

三大模块的职责边界一句话：**Vue 只管"看得见"的交互，Java 管"所有业务规则与数据"，Python 只管"把业务知识变成自然语言回答"**——Python 不直接碰业务数据库，业务数据由 Java 以实时快照形式注入给它。

---

## 2. 模块一：前端 Vue 应用（frontend/）

### 2.1 目录结构与职责

```
frontend/src/
├── main.ts                 # 应用入口：装配 Pinia、Router、Element Plus
├── App.vue                 # 根组件（仅 <router-view>）
├── router/index.ts         # 路由表 + 全局导航守卫（鉴权核心）
├── stores/auth.ts          # 认证状态（Pinia）：token/user + localStorage 持久化
├── api/http.ts             # axios 实例、拦截器、request<T> 统一封装
├── api/modules.ts          # 按业务分组的全部接口调用（auth/user/vehicle/...）
├── api/types.ts            # 与后端 DTO/VO 对应的 TS 类型
├── utils/format.ts         # 时间格式化、状态标签映射
└── views/
    ├── auth/               # 登录、注册（guestOnly）
    ├── layouts/AppLayout.vue   # 双端共用布局壳（侧边菜单按角色渲染）
    ├── admin/              # 管理员 10 个页面（/admin/**）
    └── user/               # 用户 7 个页面（/user/**）
```

### 2.2 启动流程（main.ts）

```
createApp(App) → use(createPinia()) → use(router) → use(ElementPlus) → mount('#app')
```

1. Pinia 先于 Router 装配——因为导航守卫里要用 `useAuthStore()`。
2. Router 的路由组件全部懒加载（`() => import(...)`），构建时自动代码分割，首屏只加载当前页面 chunk。
3. `App.vue` 挂载后由 URL 命中具体路由；部署态所有未匹配路径由 Nginx `try_files → index.html` 兜底回 Vue Router（history 模式）。

### 2.3 路由与导航守卫运行逻辑（`router/index.ts:67-95`）

路由表用 `meta` 声明三类约束：`requiresAuth`（需登录）、`guestOnly`（已登录禁入，如登录页）、`role`（ADMIN/USER 工作台隔离）。`/` 的 redirect 是一个函数：按 `auth.homePath` 把 ADMIN 送到 `/admin/dashboard`、USER 送到 `/user/spaces`。

`router.beforeEach` 每次导航按序执行四步：

1. **无 token 访问受保护页** → 跳 `/login?redirect=目标路径`（登录后可回跳）；
2. **有 token 但内存中无 user**（典型场景：刷新页面，Pinia 状态清空但 localStorage 还有 token）→ `auth.loadProfile()` 调 `GET /users/me` 重建用户态；该调用失败（如用户已被删除）→ `auth.logout()` 清凭证并踢回登录——这就是"删除用户后旧令牌失效"在前端的完整闭环；
3. **已登录访问 guestOnly 页** → 送回各自工作台首页；
4. **角色不匹配**（USER 闯 /admin/**）→ 送回自己工作台（前端防越权；后端 `@PreAuthorize` 是真正的防线）。

### 2.4 认证状态管理（`stores/auth.ts`）

- **持久化**：token 存 `localStorage('token')`，user 存 `localStorage('currentUser')`，Pinia state 初始化时从 localStorage 读回——刷新页面不丢登录态。
- **login action**：调 `authApi.login` → `saveSession(token, user)` 双写 Pinia 与 localStorage。
- **logout action**：只清本地 token 与 user（⚠️ 遗留：未调用服务端 `/auth/logout`，服务端令牌仍有效至过期，见 §8 遗留表 R12）。

### 2.5 请求链运行逻辑（`api/http.ts`）

```
组件调用 modules.ts 的 api 函数
  → request<T>(config)
    → axios 实例（baseURL='/api/v1'，相对路径=同源=5173，由 Nginx 代理到后端）
      → 请求拦截器：localStorage 读 token，写入 Authorization: Bearer xxx
      → 响应拦截器：HTTP 401 时清 token + currentUser（会话失效统一出口）
    → 业务判定：response.data.code !== 200 一律转 throw（后端 ApiResponse 包装）
    → 成功返回 response.data.data（组件只拿业务数据）
```

`api/modules.ts` 的 `cleanParams()` 在发请求前剔除空字符串/null/undefined 参数，避免 `?status=&pageNum=` 这类脏查询串。`apiErrorMessage()` 统一从 `ApiResponse.message` 提取后端错误文案供 `ElMessage` 展示——这就是实测中"密码长度不足"能弹出后端原文提示的原因。

### 2.6 页面运行逻辑示例：在线预约（`views/user/UserReservationCreateView.vue`）

一个标准页面的完整数据流：

1. `onMounted(load)` → `Promise.all([vehicleApi.listMine(), parkingSpaceApi.page({status:'FREE'})])` **并行**拉取我的车辆与空闲车位；
2. `computed freeSpaces` 再按 `status==='FREE'` 过滤一次——即使后端列表有变化也只展示可预约车位（前后端双保险）；
3. `submit()` 前端先做必填校验 → `reservationApi.create(form)` → 成功 `ElMessage.success` + `router.push('/user/reservations')`，失败把后端 message（如"user already has an ACTIVE reservation"）弹给用户；
4. 提交成功跳走的页面（我的预约）再查 `GET /reservations` 渲染新状态。

其余 16 个页面都是同一模式（load → 表格/表单 → submit → 刷新列表），Dashboard 页面额外用 ECharts 渲染统计卡片与趋势。

---

## 3. 模块二：后端 Java 主服务（backend/）

### 3.1 启动装配流程

`SmartParkingApplication` 一个注解启动，真正的装配由 7 个 `@Configuration` 类完成，各自向容器注册：

| 配置类 | 注册内容 | 运行时作用 |
|---|---|---|
| `SecurityConfig` | SecurityFilterChain、RoleHierarchy(ADMIN⊇USER)、CorsConfigurationSource、BCryptPasswordEncoder | 请求过滤链与方法级鉴权 |
| `JwtProperties` (@EnableConfigurationProperties) | jwt.secret/expiration/refresh-expiration/issuer 绑定 | JwtService 参数 |
| `MybatisPlusConfig` | PaginationInnerInterceptor、OptimisticLockerInnerInterceptor | 分页；`@Version` 实体的 updateById 乐观锁 |
| `RedisConfig` | RedisTemplate（String key + 白名单多态 JSON value） | 全部 Redis 读写 |
| `SchedulingConfig` | @EnableScheduling | 驱动过期任务 |
| `ReservationConfig` | ReservationProperties 绑定 | 过期宽限期/扫描间隔 |
| `RecognitionConfig` | RestClient bean | 调百度 OCR |
| `OpenApiConfig` | Bearer SecurityScheme | Swagger 调试 |

### 3.2 一次 HTTP 请求的完整生命周期

```
Tomcat :8080
 → JwtAuthenticationFilter.doFilterInternal
     resolveToken（Bearer 前缀解析）→ parseClaims（验签+issuer）
     → isUsableAccessToken 三重校验：
         ① typ=access   ② jti 不在 auth:jwt:deny:*   ③ claims.ver == auth:tokenver:{userId}
     → 通过则 new AuthUser(userId, username, role) 写入 SecurityContext
 → Spring Security 过滤链（公开路径放行 / 其余需认证）+ CORS 检查
 → DispatcherServlet → @Valid 参数校验（失败→MethodArgumentNotValid→400）
 → @PreAuthorize 方法级鉴权（失败→AccessDeniedHandler→403）
 → Service（@Transactional）→ Mapper（MP 改写 SQL：分页 LIMIT、雪花 ID 回填）
 → Controller 包 ApiResponse.success(data) → Jackson 序列化（NON_NULL）
 → 任何未捕获异常 → GlobalExceptionHandler（兜底分支 log.error + 500）
```

### 3.3 认证子系统运行逻辑（方法级调用链）

**登录**（`AuthServiceImpl.login`，`user/service/impl/`）：

```
loginRateLimiter.checkAllowed(username)        # Redis GET auth:login:fail:{u}，≥5 抛 429
→ selectByUsername → passwordEncoder.matches   # 任何一步失败：onFailure(Redis INCR+EXPIRE 15min) → 401
→ ensureEnabled(status=ENABLED)                # 禁用账号 403
→ loginRateLimiter.onSuccess(DEL 计数)
→ buildLoginVO: generateAccessToken(typ=access, jti, ver) + generateRefreshToken(typ=refresh, jti, ver)
```

**刷新**（`AuthServiceImpl.refresh`，轮换设计核心）：

```
parseClaims(refreshToken)                       # 验签失败 401
→ typ 必须 = refresh                            # access 冒充 401
→ isTokenDenied(jti)                            # 已轮换过的旧令牌 401
→ selectById + ensureEnabled
→ currentVersion(userId) == claims.ver          # 版本不符 401（改密/禁用后）
→ denyToken(旧 jti, 剩余有效期)                  # 一次性使用：旧 refresh 进黑名单
→ 返回全新令牌对
```

**踢会话**（`UserServiceImpl.changePassword` / `updateStatus`）：`UPDATE users SET password=?, token_version=token_version+1`（原子同语句）→ `TokenStateService.evictVersionCache` 清 Redis 缓存 → 所有已签发令牌的 `ver` 立即对不上。

### 3.4 预约子系统运行逻辑

**创建**（`ReservationServiceImpl.create`）五步校验-执行链：

```
requireOwnedVehicle          # 车辆属于当前用户（数据级越权防线）
→ existsActiveReservationForUser  # 业务层先查
→ requireParkingSpace(status=FREE)
→ lockParkingSpace           # CAS：UPDATE ... WHERE space_id=? AND status=FREE AND version=? → 0 行抛 409
→ insert(reservation ACTIVE) # 数据库 uk_reservations_user_active 唯一键二次兜底
→ cacheParkingSpaceStatus    # Redis 车位状态刷新
```

**过期闭环**（`reservation/task/ReservationExpirationTask` + `ReservationExpirationServiceImpl`）：

```
@Scheduled 每 60s（启动延迟 30s）
→ 查询 status=ACTIVE AND arrive_time < now-30min（LIMIT 200，走 status+arrive_time 组合索引）
→ 逐条 transactionTemplate.execute(expireOne)：          # 单条独立事务，一条失败不拖垮整批
     CAS: UPDATE reservations SET status=EXPIRED WHERE id=? AND status=ACTIVE   # 0 行=已被入场/取消，让位
     → releaseSpaceQuietly: CAS 释放车位 RESERVED→FREE    # 车位已被占用(OCCUPIED)则记日志跳过
     → 刷新/清除车位缓存
```

与用户入场的关系：入场把预约推进为 USED 也是同款条件更新（`ParkingRecordServiceImpl.markReservationUsed`），**两个 CAS 对同一行竞争，谁先成功谁赢，另一方 0 行自动让位**——无需分布式锁。

### 3.5 停车记录与计费运行逻辑

**入场**（`ParkingRecordServiceImpl.entry`，人工/OCR 共用）：

```
requireVehicleByPlateNumber → existsParkingRecord（在场查重，唯一键兜底）
→ findActiveReservation(user, vehicle, space)
→ 有预约：校验 arrive_time+宽限期 未过（isReservationExpired）→ 期望状态必须 RESERVED
→ occupyParkingSpace(CAS →OCCUPIED) → markReservationUsed(CAS ACTIVE→USED，0 行抛冲突回滚)
→ insert(PARKING 记录) → 刷缓存
```

**出场**（`exit`）：

```
查该车辆 PARKING 记录（404 兜底）
→ feeCalculatorService.calculate(entryTime, exitTime)   # 按日封顶：免费时长扣一次→自然日拆分→每日 min(ceil(分/60)×单价, maxDaily) 累加
→ releaseParkingSpace(CAS OCCUPIED→FREE)
→ 记录写 exit_time/duration/fee/status=FINISHED → 刷缓存
```

计费专项测试 8 例：免费时长内 0 元、16 分钟向上取整、跨天 3 天 = 3×maxDaily 累计、单日封顶、免费只扣一次、无规则 404、非法时间区间。

### 3.6 OCR 识别运行逻辑

`PlateRecognitionApplicationServiceImpl.recognizeEntry/Exit`：

```
multipart 文件 → file.getBytes → BaiduPlateRecognitionService.recognize
  → BaiduOcrClient.getAccessToken          # Redis 缓存 baidu:ocr:access-token，miss 才调百度 token 接口
  → POST 百度 license_plate（Base64 图）
→ confidence ≥ 阈值(0.8)?  否→LOW_CONFIDENCE 日志+拒绝
→ autoEntry(plate)/autoExit(plate) 联动停车记录子系统
→ 全过程写 plate_recognition_logs（结果/置信度/错误码）
```

策略接口 `PlateRecognitionService` 预留了 Paddle/Aliyun 占位实现（未注册 Bean，属 AGENTS.md 要求的扩展点）。

### 3.7 AI 网关子系统运行逻辑（Java 侧）

`AssistantServiceImpl.chat`：

```
会话持久化（ai_chat_sessions / ai_chat_messages）
→ AiIntentResolver 意图识别（11 个专项测试）→ 命中意图：Java 直接查业务库组装确定性回答
→ 未命中：组装 payload {question, intent, system_prompt(取自 ai_prompt_templates 表), history(≤6),
     context_snippets(实时业务快照，如"空闲85个"), referenced_data} → HTTP POST ai-service /v1/chat
→ ai-service 不可用/超时 → RuleBasedAiChatProvider 本地规则引擎兜底
→ 回答标注 provider（openai / extractive / rule-based）与 sources 返回前端
```

### 3.8 数据写入矩阵（哪个操作动哪些数据）

| 操作 | MySQL | Redis |
|---|---|---|
| 注册/登录 | users INSERT / SELECT | login:fail 计数、tokenver 缓存 |
| 创建预约 | reservations INSERT；parking_spaces CAS | parking:space:{id} SET |
| 入场(预约) | parking_spaces CAS；reservations CAS(USED)；parking_records INSERT | parking:space:{id} SET |
| 出场 | parking_spaces CAS；parking_records UPDATE(FINISHED+fee) | parking:space:{id} SET |
| 预约过期 | reservations CAS(EXPIRED)；parking_spaces CAS(FREE) | parking:space:{id} SET/DEL |
| 改密/禁用/登出 | users token_version+1 | auth:tokenver DEL、auth:jwt:deny:{jti} SET |
| 登录失败 | — | auth:login:fail INCR/DEL |
| OCR 识别 | plate_recognition_logs INSERT | baidu:ocr:access-token GET/SET |
| Dashboard | 停车记录/车位聚合查询 | 统计结果 SET(5min) |

---

## 4. 模块三：后端 Python 副服务（ai-service/）

### 4.1 为什么独立成服务

三个理由：① LLM/RAG 生态在 Python 侧最成熟，Java 不引入重依赖；② 故障隔离——大模型服务崩溃不影响停车业务主流程（Java 有规则引擎兜底）；③ 复用鉴权——Nginx 对 `/ai-api/` 用 `auth_request` 内部子请求先问 Java `/users/me`，Python 自身零鉴权代码。

### 4.2 对外契约（`app/main.py`，4 个端点）

| 端点 | 作用 |
|---|---|
| `GET /health` | 健康检查（返回 llm_enabled 与知识块数量，供 compose healthcheck） |
| `GET /v1/prompts` | 预设提示词清单 |
| `POST /v1/rag/search` | 纯检索调试 |
| `POST /v1/chat` | 主问答端点（Java 调用） |

### 4.3 配置运行逻辑（`app/config.py`）

`get_settings()` **每次调用从环境变量重建**不可变 `Settings`——改环境变量无需重启进程（运维友好设计）。`llm_enabled` 属性 = 是否配置了 `OPENAI_API_KEY`：未配置时服务**永远不会调用大模型**，只走抽取式回答，天然可用。

### 4.4 知识库与分块（`app/knowledge.py`）

`data/knowledge/` 下 3 个 Markdown 文档（parking_faq / parking_rules / parking_service）→ 按标题切节 → 每节段落聚合成 120~400 字符的知识块 → `lru_cache` 进程级单例。改知识库文件需重启进程（块数在 /health 可观测）。

### 4.5 BM25 检索运行逻辑（`app/retrieval.py`）——纯 Python 实现，无任何检索中间件

```
tokenize：中文按"单字 + 相邻二元组"切词（免分词器的中文匹配技巧），ASCII 按小写整词
BM25Index：启动时对全部知识块建倒排词频表 + IDF（K1=1.5, B=0.75）
search：查询打分 → score/(score+1) 归一化到 [0,1) → 过滤 min_score → 取 top_k
merge_context_snippets：把 Java 注入的实时数据（context_snippets）置顶，
                        score 恒为 1.0 —— 实时数据永远压过静态文档
```

最后一条是关键设计：Java 每次请求都会把"空闲 85 个"这类**实时快照**作为 context_snippets 传入，保证模型回答的是"现在"的停车场状态而不是文档里的旧数字。

### 4.6 `/v1/chat` 主流程（`main.py:120-172`）

```
intent=GREETING → 直接返回确定性问候（不依赖检索与大模型）
→ resolve_prompt(intent, system_prompt)     # Java 可传数据库模板覆盖预设
→ retrieval.search(question, top_k) → merge_context_snippets(实时数据置顶)
→ settings.llm_enabled？
    是 → llm.chat_completion(...)            # 成功则 provider="openai"
         任何失败(LlmError) → 记 warning → 落入抽取式
    否 → 抽取式
→ _build_extractive_answer：实时数据前缀 + 最优知识块拼接 + 参考数据（无大模型也能答）
→ prompts.scrub_prompt_leak(answer)          # 回答若复述了系统提示词，整体替换为安全回答
→ 返回 {answer, provider, referenced_data, sources}
```

### 4.7 LLM 调用细节（`app/llm.py`）

`build_messages` 组装顺序：system（含检索上下文块，上限 2400 字符）→ history 末 6 条 → 本次 question。调用参数 temperature=0.3、max_tokens=600（压低幻觉与成本）。所有失败路径（无 Key/超时/非 2xx/空回答）统一抛 `LlmError`，由主流程转抽取式兜底——**失败永远不会变成 500 打到用户脸上**。

### 4.8 测试

`tests/` 三个文件：`test_retrieval`（BM25 打分/合并）、`test_prompts`（模板解析/防泄漏）、`test_api`（FastAPI 端点契约）。

---

## 5. 三模块协作全景：一次 AI 问答的完整穿越

```
① 用户在 Vue 页面发送"停车费用怎么算？"
② UserAiAssistantView → POST /ai-api/v1/chat（携带 JWT）
③ Nginx：location /ai-api/ → auth_request 内部子请求 GET Java /users/me（JWT 校验）
   → 通过 → 代理到 ai-service:8000
④ 同时 Java 侧 /ai/chat（前端主链路）：意图识别命中 FEE_RULE → 查 fee_rules 组装
   context_snippets("免费15分钟，5元/时，日封顶60") + referenced_data → POST ai-service /v1/chat
⑤ ai-service：BM25 检索知识文档 → 实时数据置顶 → DeepSeek 生成（或抽取式）
⑥ 回答带 provider 标注与 sources 回到 Vue → 渲染气泡 + 徽标 + 参考数据标签
```

前端实际走的是 Java `/ai/chat` 主链路（③ 的 nginx 直连是 Python 服务的独立入口，两条链路共用同一个 ai-service）。

---

## 6. 依赖与配置审计

### 6.1 Java 依赖（backend/pom.xml）

| 依赖 | 版本 | 状态 |
|---|---|---|
| spring-boot-starter-web/security/validation/data-redis | 3.3.7 | ✅ |
| mybatis-plus-spring-boot3-starter + jsqlparser | 3.5.9 | ✅ |
| mysql-connector-j | runtime | ✅ |
| jjwt-api/impl/jackson | 0.12.6 | ✅ 当前版本线 |
| springdoc-openapi-starter-webmvc-ui | 2.6.0 | ✅ |
| mapstruct + processor | 1.6.3 | ✅ 全部 Convert 使用 |
| ~~lombok / hutool-all~~ | — | ✅ 已清理（2026-10-07，零引用） |
| starter-test + security-test | — | ✅ |
| starter-actuator | — | ❌ 缺失：yml 配了 health/info 端点但依赖未引入 |
| flyway/liquibase、testcontainers、h2 | — | ❌ 缺失：无迁移引擎、无集成测试（H2 死配置文件已删） |

### 6.2 关键配置（application.yml）

| 段 | 要点 |
|---|---|
| `spring.datasource` / `data.redis` | 全环境变量化；Redis 未配 Lettuce 连接池（当前量级可接受） |
| `mybatis-plus` | 雪花 ID；~~logic-delete 死配置~~ 已移除 |
| `jwt` | access 7200s / refresh 604800s；secret 有开发默认值（启动 WARN，compose 必填） |
| `reservation` | 宽限期 30min / 扫描 60s / 启动延迟 30s |
| `app.cors` | 白名单来源（5173/3000） |
| `app.ai` | provider 三态 + python 子配置 |
| profile | 仅 default+test（test profile 文件已删），无 dev/prod 区分 |

---

## 7. 横切审计（跨三模块的公共设施）

### 7.1 安全总览

| 防线 | 前端 Vue | Java 主服务 | Python 副服务 |
|---|---|---|---|
| 身份 | 路由守卫 + 角色分流（可绕过，仅体验层） | JWT 三重校验 + @PreAuthorize + Service 数据过滤（真防线） | 零鉴权代码，靠 nginx auth_request |
| 会话失效 | 401 拦截器清凭证 | token_version + jti 黑名单 + 轮换 | — |
| 输入 | 表单校验（体验） | @Valid/@Pattern + 提示注入防护 PromptLeakGuard | 输入长度约束 + scrub_prompt_leak |
| 并发 | — | CAS + 生成列唯一键 + 函数索引 | — |

### 7.2 Redis Key 全景

| Key | TTL | 用途 |
|---|---|---|
| `parking:space:{id}` | **无 TTL** ⚠️ | 车位实时状态 |
| `dashboard:*` | 5min | 统计缓存 |
| `baidu:ocr:access-token` | expires_in−60s | 百度令牌 |
| `auth:jwt:deny:{jti}` | 令牌剩余期 | 吊销黑名单 |
| `auth:tokenver:{userId}` | 5min | 会话版本缓存 |
| `auth:login:fail:{u}` | 15min | 登录限流 |

### 7.3 测试体系

| 模块 | 框架 | 数量 |
|---|---|---|
| Java | JUnit5 + Mockito + MockMvc | 44 类 250 例全绿 |
| Python | pytest | 3 文件（api/prompts/retrieval） |
| 前端 | 无 | — |
| 集成 | 无 @SpringBootTest/Testcontainers（人工环境实测补位：31 接口 + 14 界面） | — |

### 7.4 可观测性

日志仅 4 处 Logger（AI 路由/模板、OCR、过期任务、全局异常兜底）；Swagger 双入口 `8080/swagger-ui.html` 与 `5173/swagger-ui.html`（nginx `$http_host` 透传修复后两侧基地址均正确）；actuator 依赖缺失致健康检查 404。

---

## 8. 遗留问题与风险总表

| 编号 | 级别 | 问题 | 位置 | 建议 |
|---|---|---|---|---|
| R2 | 高 | ~~Redis record 缓存反序列化 500~~ | RedisConfig | ✅ 已修复 |
| R3 | 高 | 缓存写在事务提交前 + `parking:space:*` 无 TTL | Reservation/Record/Space ServiceImpl | afterCommit 写缓存 + TTL |
| R1 | 高 | 无迁移引擎，SQL 仅首次建库生效（V2_7 空缺） | db/sql/ | 接入 Flyway；老库按 database-design.md 5.13 手工 ALTER |
| R5 | 中 | Dashboard 内存聚合 + 趋势逐日查询（约 74 条 SQL） | DashboardServiceImpl | SQL SUM/GROUP BY |
| R6 | 中 | 无 MetaObjectHandler，时间字段手工 set | 各 ServiceImpl | 自动填充 |
| R7 | 中 | OCR RestClient 无超时、同步阻塞 | RecognitionConfig | 配置化 timeout |
| R10 | 中 | 畸形 JSON→500、方法不匹配→500 | GlobalExceptionHandler | 映射 400/405 |
| R11 | 中 | actuator 依赖缺失 | pom/yml | 补 starter |
| R12 | 中 | 前端登出未调 `/auth/logout`、未接 refresh 续期 | stores/auth.ts | 联调补齐 |
| R13 | 中 | 计费规则可全部禁用 → 出场无规则可用 | FeeRuleServiceImpl | 守卫或兜底规则 |
| R14 | 中 | 无集成测试（~~H2 死配置~~已删） | pom | Testcontainers |
| R9 | 低 | 访客车牌无入场通道 | ParkingRecordServiceImpl | 临时车牌方案 |
| R15 | 低 | ~~Lombok/hutool 冗余~~ | pom | ✅ 已清理 |
| R16 | 低 | 枚举与 @Pattern 双写、normalizeStatus 4 处复制 | DTO/ServiceImpl | 公共化 |
| R17 | 低 | `LocalDateTime.now()` 无 Clock 不可测 | ServiceImpl | 注入 Clock |
| R18 | 低 | 密码仅长度校验 | RegisterRequest | 复杂度校验 |
| R8 | 提醒 | 非 git 仓库、无 README/CI、`.env` 真实第三方密钥 | 项目根 | git init + 轮换密钥 |

---

## 9. 吃透本项目：推荐学习路径

### 9.1 按模块递进的十步（每步附自测问题）

| 步 | 模块 | 读什么 | 自测问题 |
|---|---|---|---|
| 1 | 部署 | docker-compose.yml + nginx.conf | 一次请求从浏览器到 MySQL 经几跳？/ai-api 的鉴权子请求怎么工作？ |
| 2 | 前端 | main.ts → router/index.ts → stores/auth.ts | 刷新页面后登录态如何恢复？删除用户后前端会发生什么？ |
| 3 | 前端 | api/http.ts → modules.ts → 任一页面 | 后端 ApiResponse 的 code!=200 在哪一层被转成异常？ |
| 4 | Java 装配 | 7 个 @Configuration | SecurityConfig 与 MybatisPlusConfig 各自往容器里放了什么？ |
| 5 | Java 请求生命周期 | JwtAuthenticationFilter → SecurityConfig | 双令牌的三重校验分别是哪三重？ |
| 6 | Java 认证 | AuthServiceImpl（login/refresh/logout）+ UserServiceImpl | 改密为什么能踢掉所有会话？轮换为什么能防重放？ |
| 7 | Java 预约闭环 | ReservationServiceImpl → ReservationExpirationServiceImpl → ParkingRecordServiceImpl | 过期任务与用户入场如何无锁互斥？ |
| 8 | Java 计费 | FeeCalculatorServiceImpl + 它的 8 个测试 | 停 3 天为什么收 3×maxDaily？免费时长扣几次？ |
| 9 | Python | main.py /v1/chat → retrieval.py → llm.py | 实时数据为什么永远压过文档？LLM 挂了用户看到什么？ |
| 10 | 三模块串联 | §5 时序 + ai 包 | 一次 AI 问答经过几跳？provider 徽标有哪些取值？ |

### 9.2 核心类 22 个清单

```
前端:   router/index.ts · stores/auth.ts · api/http.ts · UserReservationCreateView.vue
Java:   SecurityConfig · JwtAuthenticationFilter · JwtService · TokenStateService · LoginRateLimiter
        AuthServiceImpl · UserServiceImpl · ReservationServiceImpl · ReservationExpirationServiceImpl
        ParkingRecordServiceImpl · FeeCalculatorServiceImpl · ParkingSpaceServiceImpl · VehicleServiceImpl
        PlateRecognitionApplicationServiceImpl · GlobalExceptionHandler · RedisConfig
Python: main.py(/v1/chat) · retrieval.py(BM25) · llm.py · knowledge.py
```

### 9.3 动手实验

1. **并发**：双终端同车位抢预约，看一方 409；删掉 CAS 的 version 条件再试。
2. **会话**：改密后验证另一终端 token 立即 401；删除 Redis `auth:tokenver:*` 观察缓存回填。
3. **过期**：`RESERVATION_EXPIRATION_GRACE_MINUTES=0` 重启，创建 1 分钟后到达的预约，看 60 秒内自动释放。
4. **降级**：`docker stop smart-parking-ai-service` 再问 AI，确认落到 extractive/rule-based；`docker start` 恢复。
5. **检索**：直接 `POST ai-service:8000/v1/rag/search`（容器内网）对比不同问法的 BM25 得分。

---

*本报告与 `docs/development-plan.md` §14/§15 迭代记录、`docs/database-design.md` 表结构说明、`docs/device-integration-design.md` 硬件接入设计互为补充。遗留问题建议按 R3 → R1 → R10/R11 → R5 顺序推进。*
