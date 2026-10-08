SET NAMES utf8mb4;

-- AI 智能问答会话表
CREATE TABLE IF NOT EXISTS ai_chat_sessions (
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

-- AI 智能问答消息表
CREATE TABLE IF NOT EXISTS ai_chat_messages (
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

-- 用户意见与建议表
CREATE TABLE IF NOT EXISTS user_suggestions (
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

-- 表关系说明：
-- ai_chat_sessions.user_id -> users.user_id
-- ai_chat_messages.session_id -> ai_chat_sessions.session_id（级联删除）
-- ai_chat_messages.user_id -> users.user_id
-- user_suggestions.user_id -> users.user_id
-- user_suggestions.handler_id -> users.user_id（处理该建议的管理员）

-- 索引设计说明：
-- idx_ai_chat_sessions_user 支撑“我的会话”分页查询
-- idx_ai_chat_messages_session 支撑会话内消息按时间升序读取
-- idx_user_suggestions_user 支撑“我的建议”分页查询
-- idx_user_suggestions_status 支撑管理员按状态筛选
