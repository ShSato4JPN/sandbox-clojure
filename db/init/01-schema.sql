-- コンテナの初回起動時にだけ実行される (docker-entrypoint-initdb.d)。
-- 作り直したいときは `docker compose down -v` でボリュームごと消すこと。
CREATE TABLE IF NOT EXISTS todos (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  title      VARCHAR(255) NOT NULL,
  done       BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
