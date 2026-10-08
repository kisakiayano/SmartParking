# Docker 部署说明

本文档用于在本机或服务器上通过 Docker Compose 启动智慧停车系统，包含：

- MySQL
- Redis
- Spring Boot 后端
- Vue3 前端

## 1. 服务拓扑

| 服务 | 容器名 | 镜像/构建方式 | 默认端口 | 说明 |
|---|---|---|---|---|
| MySQL | `smart-parking-mysql` | `mysql:8.4` | `3306` | 业务数据库，首次启动自动执行建表 SQL |
| Redis | `smart-parking-redis` | `redis:7.4-alpine` | `6379` | 缓存车位实时状态 |
| Backend | `smart-parking-backend` | `backend/Dockerfile` | `8080` | Spring Boot API 服务 |
| AI Service | `smart-parking-ai-service` | `ai-service/Dockerfile` | `8000` | Python FastAPI RAG 服务，供后端 `AI_PROVIDER=python` 调用 |
| Frontend | `smart-parking-frontend` | `frontend/Dockerfile` | `5173` | Nginx 托管 Vue3 静态资源并反向代理 API |

前端服务配置在 Compose 的 `frontend` profile 中，启动全量系统时需要带上：

```bash
docker compose --profile frontend ...
```

## 2. 前置要求

本机需要安装：

- Docker
- Docker Compose v2
- JDK 17
- Maven 3.9+

当前 `backend/Dockerfile` 会复制本机已经构建好的 jar：

```text
backend/target/smart-parking-backend-0.0.1-SNAPSHOT.jar
```

所以首次启动前需要先在项目根目录构建后端：

```bash
mvn clean package
```

如果只希望打包镜像、不运行测试，可以使用：

```bash
mvn clean package -DskipTests
```

## 3. 环境变量

复制环境变量模板：

```bash
cp .env.example .env
```

`.env` 默认值如下：

```properties
MYSQL_ROOT_PASSWORD=root
MYSQL_DATABASE=smart_parking
MYSQL_PORT=3306

REDIS_PORT=6379

BACKEND_PORT=8080
FRONTEND_PORT=5173

JWT_SECRET=change-me-to-a-long-random-secret-at-least-32-chars
JWT_EXPIRATION=7200
JWT_REFRESH_EXPIRATION=604800
JWT_ISSUER=smart-parking

CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000

RESERVATION_EXPIRATION_GRACE_MINUTES=30

BAIDU_OCR_API_KEY=
BAIDU_OCR_SECRET_KEY=
BAIDU_OCR_CONFIDENCE_THRESHOLD=0.8000

# AI 助手：provider 取 rule-based（默认，内置规则引擎，无需外部服务）或 openai
AI_PROVIDER=rule-based
AI_BASE_URL=https://api.openai.com/v1
AI_API_KEY=
AI_MODEL=gpt-4o-mini
AI_TIMEOUT_SECONDS=20
AI_MAX_HISTORY_MESSAGES=6
AI_TEMPERATURE=0.3

# Python RAG 服务：AI_PROVIDER=python 时后端改用 ai-service 回答，失败自动回退 rule-based
# Python AI 服务地址：
# - docker compose 部署：保持注释即可，后端容器默认使用服务名 http://ai-service:8000
# - 宿主机直接运行后端：取消注释并使用 http://localhost:8000
# AI_PYTHON_BASE_URL=http://localhost:8000
AI_PYTHON_TIMEOUT_SECONDS=30
AI_SERVICE_PORT=8000
AI_RAG_TOP_K=4
AI_RAG_EMBEDDING_ENABLED=false
```

部署到服务器时至少应修改：

- `MYSQL_ROOT_PASSWORD`
- `JWT_SECRET`（必填，docker compose 未设置该变量会直接启动失败；生成方式：`openssl rand -base64 48`）
- `BAIDU_OCR_API_KEY`
- `BAIDU_OCR_SECRET_KEY`

AI 助手默认使用内置规则引擎，`AI_API_KEY` 可留空；如需启用 OpenAI 兼容大模型，将 `AI_PROVIDER` 改为 `openai` 并填写 `AI_BASE_URL`、`AI_API_KEY`、`AI_MODEL`。

如果不配置百度 OCR Key，OCR 入场/出场接口会返回：

```json
{"code":400,"message":"baidu OCR credentials are not configured"}
```

如果前端必须挂载到宿主机 `8080`，不要同时把后端也映射到宿主机 `8080`。推荐配置：

```properties
BACKEND_PORT=18080
FRONTEND_PORT=8080
```

此时浏览器访问：

```text
http://localhost:8080
```

前端容器会通过 Docker 内部网络访问后端 `backend:8080`，不受宿主机 `BACKEND_PORT` 影响。后端直连调试地址改为：

```text
http://localhost:18080
```

## 4. 启动全量系统

在项目根目录执行：

```bash
docker compose --profile frontend up --build -d
```

查看容器状态：

```bash
docker compose --profile frontend ps
```

查看日志：

```bash
docker compose --profile frontend logs -f
```

只查看后端日志：

```bash
docker compose logs -f backend
```

## 5. 访问地址

| 功能 | 地址 |
|---|---|
| 前端页面 | `http://localhost:5173` |
| 后端 API | `http://localhost:8080` |
| Swagger | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| MySQL | `localhost:3306` |
| Redis | `localhost:6379` |

前端容器内置 Nginx 代理：

- `/api/*` 转发到后端 `backend:8080`
- `/swagger-ui.html` 转发到后端
- `/v3/api-docs` 转发到后端
- `/ai-api/health` 为公开探针，转发到 `ai-service:8000/health`，无需登录
- 其余 `/ai-api/**` 需要携带有效 Bearer JWT，Nginx 通过 `auth_request` 调用后端 `/api/v1/users/me` 校验后才转发；Java 后端自身通过 Docker 内网 `http://ai-service:8000` 直连该服务，不经过 Nginx。

## 6. 数据库初始化

MySQL 容器首次创建数据卷时会自动执行：

```text
backend/src/main/resources/db/sql/*.sql
```

这些脚本会创建业务表，并插入默认计费规则。

注意：MySQL 官方镜像只会在数据目录为空时执行 `/docker-entrypoint-initdb.d`。如果数据卷已经存在，修改 SQL 后不会自动重新执行。

### 6.1 初始化开发管理员

开发环境可导入默认管理员：

```bash
docker exec -i smart-parking-mysql mysql -uroot -proot smart_parking < docs/dev-admin.sql
```

默认账号：

```text
用户名：admin
密码：Password@123
```

如果 `.env` 中修改了数据库密码或库名，请同步替换命令中的 `-proot` 和 `smart_parking`。

### 6.2 导入测试数据

如需导入测试数据：

```bash
docker exec -i smart-parking-mysql mysql -uroot -proot smart_parking < sql/init-data.sql
```

测试数据包括：

- 10 个用户
- 30 个车辆
- 100 个车位
- 20 条预约记录
- 50 条停车记录
- 3 条计费规则

种子用户密码均为：

```text
Password@123
```

`sql/init-data.sql` 使用固定 ID 段，重复执行会先清理本脚本生成的数据。

## 7. 常用运维命令

停止容器但保留数据：

```bash
docker compose --profile frontend down
```

停止容器并删除 MySQL/Redis 数据卷：

```bash
docker compose --profile frontend down -v
```

重启后端：

```bash
docker compose restart backend
```

重新构建并启动后端：

```bash
mvn clean package
docker compose up --build -d backend
```

重新构建并启动全量系统：

```bash
mvn clean package
docker compose --profile frontend up --build -d
```

进入 MySQL：

```bash
docker exec -it smart-parking-mysql mysql -uroot -proot smart_parking
```

进入 Redis：

```bash
docker exec -it smart-parking-redis redis-cli
```

## 8. 验证流程

启动后建议按以下顺序验证：

```bash
docker compose --profile frontend ps
curl http://localhost:8080/v3/api-docs
curl http://localhost:5173
curl http://localhost:8000/health
curl http://localhost:5173/ai-api/health
```

预期结果：

- `mysql` 为 `healthy`
- `redis` 为 `healthy`
- `backend` 为 `Up`
- `ai-service` 为 `healthy`
- `frontend` 为 `Up`
- `http://localhost:8080/v3/api-docs` 返回 OpenAPI JSON
- `http://localhost:5173` 返回前端 HTML
- `curl http://localhost:8000/health` 返回 `{"status":"ok",...}`

## 9. 外部连接配置

如果在 IDEA 中运行后端，但继续使用 Docker 中的 MySQL 和 Redis，可配置：

```text
DB_URL=jdbc:mysql://localhost:3306/smart_parking?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=root
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_DATABASE=0
```

如果 `.env` 修改了端口或密码，请同步调整。

在宿主机（例如 IDEA）直接运行后端、仅用 Docker 承载 MySQL/Redis/ai-service 时，后端容器外的 `localhost` 指向宿主机，需显式设置：

```properties
AI_PYTHON_BASE_URL=http://localhost:8000
```

## 10. 可选：Docker 内构建后端

当前默认后端镜像依赖本机 Maven 构建结果，优点是 Docker 构建速度快。

如果希望完全在 Docker 内构建后端镜像，可在网络稳定时将 `docker-compose.yml` 中后端构建配置改为：

```yaml
backend:
  build:
    context: .
    dockerfile: backend/Dockerfile.build
```

然后执行：

```bash
docker compose --profile frontend up --build -d
```

## 11. 常见问题

### 11.1 访问 `http://localhost:5173` 拒绝连接

通常是前端 profile 没有启动。使用：

```bash
docker compose --profile frontend up --build -d
```

并检查：

```bash
docker compose --profile frontend ps
docker compose logs -f frontend
```

### 11.2 接口返回 `401 unauthorized`

说明当前请求没有携带 JWT，或 token 已失效。先登录获取 token，或在前端页面重新登录。

如果没有管理员账号，先执行：

```bash
docker exec -i smart-parking-mysql mysql -uroot -proot smart_parking < docs/dev-admin.sql
```

### 11.3 修改 SQL 后数据库没有变化

MySQL 初始化脚本只会在数据卷为空时执行。已存在的部署升级时，不要直接 `down -v`（会清空数据），应先对已有库手动执行增量迁移脚本。

以 Phase 2.10 的 `V2_10__ai_prompts.sql` 为例：

```bash
cd /home/ayano/project
docker exec -i smart-parking-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" smart_parking' \
  < backend/src/main/resources/db/sql/V2_10__ai_prompts.sql
```

迁移脚本使用 `CREATE TABLE IF NOT EXISTS` 和 `INSERT ... WHERE NOT EXISTS`，可重复执行。执行后可用 `SHOW TABLES` 和目标表的 `SELECT` 确认。

安全加固迭代（2026-10）修改的是已有表的 ALTER 语句（users/reservations/parking_records/fee_rules），不能直接执行建表 SQL，完整语句见 `docs/database-design.md` 5.13 节，示例：

```bash
docker exec -i smart-parking-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" smart_parking' <<'SQL'
ALTER TABLE users ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE reservations
  ADD COLUMN active_flag TINYINT AS (IF(status = 'ACTIVE', 1, NULL)) VIRTUAL,
  ADD UNIQUE KEY uk_reservations_user_active (user_id, active_flag);
ALTER TABLE parking_records
  ADD COLUMN active_flag TINYINT AS (IF(status = 'PARKING', 1, NULL)) VIRTUAL,
  ADD UNIQUE KEY uk_parking_records_vehicle_active (vehicle_id, active_flag);
ALTER TABLE fee_rules
  ADD UNIQUE KEY uk_fee_rules_enabled ((IF(enabled = 1, 1, NULL)));
SQL
```

若 ALTER 因老数据违反唯一约束失败，需先按约束语义清理数据再执行。

只有在确认可以清空数据、需要全新初始化时，才使用重建方式：

```bash
docker compose --profile frontend down -v
docker compose --profile frontend up --build -d
```

### 11.4 后端镜像构建失败，提示 jar 不存在

先执行：

```bash
mvn clean package
```

然后重新启动：

```bash
docker compose --profile frontend up --build -d
```

### 11.5 OCR 接口返回百度凭证未配置

在 `.env` 中配置：

```properties
BAIDU_OCR_API_KEY=你的百度API Key
BAIDU_OCR_SECRET_KEY=你的百度Secret Key
```

然后重启后端：

```bash
docker compose up -d --force-recreate backend
```

### 11.6 AI 助手固定使用规则引擎回答

当 `AI_PROVIDER=openai` 但未配置 `AI_API_KEY`，或外部大模型接口调用失败时，后端会自动降级到内置规则引擎，这是预期行为。如需排查大模型调用，查看后端日志中的 `AI provider [openai] failed` 提示，并确认：

- `AI_PROVIDER=openai`
- `AI_BASE_URL` 可被容器访问
- `AI_API_KEY` 正确
- `AI_MODEL` 与接口兼容

### 11.7 AI 助手未使用 Python RAG

当 `AI_PROVIDER=python` 但 Python RAG 服务不可达、提示词模板表缺失或调用失败时，后端会自动降级回规则引擎（`provider=rule-based`），这是预期行为。排查步骤：

1. 确认 `.env` 中 `AI_PROVIDER=python`，`AI_PYTHON_BASE_URL` 可从后端容器访问；后端容器内必须使用 Compose 默认的服务名 `http://ai-service:8000`，使用 `localhost` 会指向后端容器自身、必然失败，属于错误配置。
2. 查看后端降级日志（已包含异常类与原因）：

```bash
cd /home/ayano/project
docker compose logs backend | grep -i "AI provider"
```

3. 查询运行状态接口（需登录，返回配置提供方、生效提供方与可用性）：

```bash
curl -H "Authorization: Bearer <token>" http://localhost:<FRONTEND_PORT>/api/v1/ai/status
```

4. 确认提示词模板表存在（缺失时按 11.3 执行迁移）：

```bash
docker exec smart-parking-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SHOW TABLES FROM smart_parking LIKE \"ai_prompt_templates\""'
```

5. 查看 Python 服务日志与健康状态：

```bash
docker compose logs ai-service
curl http://localhost:8000/health              # 宿主机直连（仅回环地址）
curl http://localhost:<FRONTEND_PORT>/ai-api/health   # 经 Nginx 公开探针
```

6. 确认 `ai-service` 容器已启动且为 `healthy`：`docker compose --profile frontend ps`。
