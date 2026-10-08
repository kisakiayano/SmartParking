# Smart Parking AI Service (`ai-service`)

A small, dependency-light **FastAPI** service that answers smart-parking
questions using **retrieval-augmented generation (RAG)** over a parking-domain
markdown knowledge base, optional **OpenAI-compatible** LLM completion, and an
**extractive fallback** that works fully offline.

It is called server-to-server by the Java (Spring Boot) backend and exposed to
the browser through Nginx at `/ai-api/`. There is intentionally **no CORS**
configuration because the service is not called directly by browsers.

- Pure-Python BM25 lexical retrieval — **no numpy, no LangChain, no vector DB**.
- Preset Chinese prompt templates per business intent.
- Runs without any API key: every request is answered in `extractive` mode.
- When `OPENAI_API_KEY` is set, answers come from the LLM (`provider="openai"`)
  and any failure silently falls back to `extractive`.

---

## HTTP contract

| Method | Path | Purpose |
| ------ | ---- | ------- |
| `GET`  | `/health`        | Liveness/readiness + knowledge chunk count |
| `GET`  | `/v1/prompts`    | List preset prompt templates |
| `POST` | `/v1/rag/search` | Lexical retrieval over the knowledge base |
| `POST` | `/v1/chat`       | RAG chat (LLM or extractive) |

All error responses are JSON `{"detail": ...}` with an appropriate status code.
An ordinary unknown question never returns HTTP 500.

### `GET /health`

```json
{ "status": "ok", "llm_enabled": false, "knowledge_chunks": 10 }
```

### `GET /v1/prompts`

```json
[
  { "code": "default_assistant", "name": "通用助手", "intent": null },
  { "code": "SPACE_STATUS", "name": "车位状态助手", "intent": "SPACE_STATUS" }
]
```

Preset codes: `default_assistant` (intent `null`), `SPACE_STATUS`, `FEE_RULE`,
`RESERVATION_GUIDE`, `ENTRY_EXIT_GUIDE`, `SUGGESTION_GUIDE`, `SYSTEM_FAQ` and
`fallback`.

### `POST /v1/rag/search`

Request:

```json
{ "query": "怎么在线预约车位", "top_k": 3 }
```

Response:

```json
{
  "results": [
    { "doc_id": "parking_faq", "title": "… - 如何在线预约车位", "score": 0.87, "snippet": "…" }
  ]
}
```

### `POST /v1/chat`

Request — only `question` is required; every other field is optional:

```json
{
  "session_id": null,
  "user_id": 10,
  "username": "user01",
  "question": "现在还有多少空位",
  "intent": "SPACE_STATUS",
  "history": [{ "role": "user", "content": "你好" }],
  "context_snippets": ["实时空闲车位：4 个"],
  "referenced_data": ["空闲：4"],
  "system_prompt": null
}
```

Response:

```json
{
  "answer": "根据当前实时数据：实时空闲车位：4 个。…",
  "provider": "extractive",
  "referenced_data": ["空闲：4"],
  "sources": [{ "title": "实时数据", "score": 1.0, "snippet": "实时空闲车位：4 个" }]
}
```

`provider` is `extractive` or `openai`. Entries from `context_snippets` are
treated as highest-priority retrieved items (`score = 1.0`) so live Java-side
data always wins over static documentation.

Prompt precedence: request `system_prompt` **>** preset by `intent` **>**
`default_assistant`.

---

## Configuration

All configuration is read from environment variables on each request.

| Variable | Default | Description |
| -------- | ------- | ----------- |
| `AI_SERVICE_PORT` | `8000` | Uvicorn port for the `__main__` entrypoint |
| `OPENAI_BASE_URL` | `https://api.openai.com/v1` | OpenAI-compatible base URL |
| `OPENAI_API_KEY` | *(empty)* | Empty → extractive mode, no startup failure |
| `OPENAI_MODEL` | `gpt-4o-mini` | Chat completion model |
| `REQUEST_TIMEOUT_SECONDS` | `20` | Outbound LLM request timeout；Compose 未注入，仅在手动运行容器或扩展 compose 时生效 |
| `RAG_TOP_K` | `4` | Default number of retrieved chunks for chat |
| `RAG_MIN_SCORE` | `0.0` | Minimum normalized score (`0..1`) to keep a hit；Compose 未注入，仅在手动运行容器或扩展 compose 时生效 |
| `RAG_EMBEDDING_ENABLED` | `false` | Reserved for a future embedding path; lexical BM25 is the only active strategy |

---

## Local development

```bash
cd ai-service
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
.venv/bin/uvicorn app.main:app --host 0.0.0.0 --port 8000
```

Then, for example:

```bash
curl -s http://127.0.0.1:8000/health
curl -s http://127.0.0.1:8000/v1/prompts
curl -s -X POST http://127.0.0.1:8000/v1/rag/search \
  -H 'Content-Type: application/json' \
  -d '{"query":"怎么在线预约车位","top_k":3}'
curl -s -X POST http://127.0.0.1:8000/v1/chat \
  -H 'Content-Type: application/json' \
  -d '{"question":"现在还有多少空位","context_snippets":["实时空闲车位：4 个"]}'
```

You can also run the module entrypoint, which honours `AI_SERVICE_PORT`:

```bash
AI_SERVICE_PORT=8000 .venv/bin/python -m app.main
```

### Tests

Tests are fully offline (no network, no API key required) and use
`fastapi.testclient`:

```bash
cd ai-service
.venv/bin/python -m pytest -q
```

---

## Knowledge base

Markdown sources live in `data/knowledge/`:

| File | Topics |
| ---- | ------ |
| `parking_faq.md` | 在线预约流程、预约超时自动释放、取消预约 |
| `parking_rules.md` | 计费规则公式（免费时长、按小时向上取整、日封顶）、入场出场流程、车位状态含义 |
| `parking_service.md` | 用户可用的查询功能、意见反馈流程、联系管理员 |

`app/knowledge.py` splits each file by markdown headings, then groups paragraphs
into chunks of roughly 120–400 characters, keeping the document id and a
readable section title per chunk. The corpus is loaded once and cached.

`app/retrieval.py` builds an in-memory BM25 index (tokenized as CJK characters,
CJK bigrams and lowercased ASCII words) and returns the top matches with a
normalized `0..1` score and a short snippet.

---

## Docker

The compose build context is the repository root, so the Dockerfile copies
`ai-service/...` paths.

```bash
# from the repository root
docker build -f ai-service/Dockerfile -t smart-parking-ai-service .
docker run --rm -p 8000:8000 smart-parking-ai-service
```

`docker-compose.yml` (owned by the deployment step) is expected to add a
service such as:

```yaml
  ai-service:
    build:
      context: .
      dockerfile: ai-service/Dockerfile
    container_name: smart-parking-ai-service
    restart: unless-stopped
    environment:
      AI_SERVICE_PORT: 8000
      OPENAI_BASE_URL: ${AI_BASE_URL:-https://api.openai.com/v1}
      OPENAI_API_KEY: ${AI_API_KEY:-}
      OPENAI_MODEL: ${AI_MODEL:-gpt-4o-mini}
      RAG_TOP_K: ${AI_RAG_TOP_K:-4}
      RAG_EMBEDDING_ENABLED: ${AI_RAG_EMBEDDING_ENABLED:-false}
      TZ: Asia/Shanghai
    expose:
      - "8000"
```

Compose 只注入上列变量（均从根目录 `.env` 读取）。`REQUEST_TIMEOUT_SECONDS` 与
`RAG_MIN_SCORE` 由服务支持但 Compose 未接线，属于可选变量，仅在手动 `docker run`
或自行扩展 compose 时才需要设置。

### Nginx `/ai-api/` route

The frontend Nginx proxies `/ai-api/` to this service. `/ai-api/health` is a
public ops probe; every other `/ai-api/**` path is gated by JWT verification in
Nginx (`auth_request` against the Java backend `/api/v1/users/me`). Because
`/ai-api/` is stripped before reaching the app, the upstream paths are
`/health`, `/v1/prompts`, `/v1/rag/search` and `/v1/chat`:

```nginx
location = /ai-api/health {
    proxy_pass http://ai-service:8000/health;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
}

location = /ai-api-auth {
    internal;
    proxy_pass http://backend:8080/api/v1/users/me;
    proxy_method GET;
    proxy_pass_request_body off;
    proxy_set_header Content-Length "";
    proxy_set_header Authorization $http_authorization;
    proxy_set_header X-Original-URI $request_uri;
}

location /ai-api/ {
    auth_request /ai-api-auth;
    proxy_pass http://ai-service:8000/;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

So from a browser an authenticated request targets `POST /ai-api/v1/chat`
(public probe: `GET /ai-api/health`). The Java backend itself calls the service
internally at `http://ai-service:8000` without going through Nginx.
