# TODO アプリ設計 (React + Apollo / Lacinia GraphQL / MySQL)

- 日付: 2026-09-12
- ステータス: 承認済み (実装計画待ち)

## 目的

既存の Lacinia 学習サンドボックスを、ブラウザから MySQL まで一本の線が通った
フルスタック構成に拡張する。題材は TODO リスト 1 テーブル。
学習が目的であり、認証・権限・複数ユーザ・ページングは範囲外。

## 全体構成

```
ブラウザ
  └ React + Vite + Apollo Client   :5173   frontend/
        │  POST /api   ← Vite dev サーバが :8888 へ proxy
        ▼
     Lacinia + Pedestal            :8888   src/gql/
        │  next.jdbc
        ▼
     MySQL 8.4 (Docker)            host :3307 → container 3306
```

ホスト側を 3307 にするのは、ローカルに MySQL が入っていた場合のポート衝突を避けるため。

フロントとサーバでポートが分かれるため、そのままでは別オリジンとなり
`fetch` / Apollo の POST が CORS で失敗する。Vite の `server.proxy` で `/api` を
:8888 に中継し、ブラウザから見て同一オリジンにすることで解決する。
サーバ側に CORS 設定は入れない。

## コンポーネント

### 1. MySQL コンテナ

`docker-compose.yml` に `mysql:8.4` を 1 サービス。

- データベース名: `todo`
- ユーザ: アプリ用ユーザを 1 つ作成 (root は使わない)
- 認証情報は compose ファイルに直書き (ローカル学習用のため)
- データは名前付きボリュームに永続化。初期化し直す場合は `docker compose down -v`

スキーマは `db/init/01-schema.sql` を `/docker-entrypoint-initdb.d` にマウントし、
コンテナ初回起動時に自動適用する。マイグレーションツール (migratus 等) は導入しない。

```sql
CREATE TABLE todos (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  title      VARCHAR(255) NOT NULL,
  done       BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

シードデータは入れない (空状態から動作を確認できるようにするため)。

### 2. DB アクセス層 `src/gql/db.clj`

依存に `com.github.seancorfield/next.jdbc` と `com.mysql/mysql-connector-j` を追加する。

このネームスペースに SQL を閉じ込め、resolver からは関数呼び出しのみとする。
resolver に SQL が散らばるのを防ぐため。

公開する関数:

| 関数 | 返り値 |
| --- | --- |
| `(list-todos)` | 全 todo を `created_at` 降順で |
| `(create-todo! title)` | 挿入した 1 行 |
| `(toggle-todo! id)` | `done` を反転した後の 1 行。該当なしなら `nil` |
| `(delete-todo! id)` | 削除できたら `true`、該当なしなら `false` |

返す行は next.jdbc の既定どおりテーブル名付きキーワード (`:todos/id`, `:todos/created_at`)
のまま。GraphQL のフィールド名への変換は resolver 側 (4.) の責務とする。

DataSource は `defonce` で 1 度だけ生成して使い回す。
接続プール (HikariCP) は学習用の単一ユーザ想定では不要なため入れない。

### 3. GraphQL スキーマ `resources/schema.edn`

型 `Todo`:

| フィールド | 型 |
| --- | --- |
| `id` | `Int!` |
| `title` | `String!` |
| `done` | `Boolean!` |
| `createdAt` | `String` (ISO-8601 文字列) |

操作:

| 種別 | 名前 | 説明 |
| --- | --- | --- |
| Query | `todos: [Todo!]!` | 全件を作成日時降順で返す |
| Mutation | `createTodo(title: String!): Todo` | 追加して作成された Todo を返す |
| Mutation | `toggleTodo(id: Int!): Todo` | `done` を反転して返す |
| Mutation | `deleteTodo(id: Int!): Boolean` | 削除の成否を返す |

既存の `hello` / `greet` は学習メモとしての価値があるため残す。

### 4. resolver `src/gql/schema.clj`

既存の `resolver-map` に 4 つを追加し、`db.clj` の関数へ委譲する。

- DB の行 (`:todos/id` 等) を GraphQL のフィールド名 (`:id`, `:createdAt`) に変換する
  private ヘルパを 1 つ置く
- 存在しない id を指定された `toggleTodo` / `deleteTodo` は
  `com.walmartlabs.lacinia.resolve/resolve-as` で `nil` + エラーメッセージを返す

### 5. フロントエンド `frontend/`

`npm create vite` は対話が入るため使わず、必要なファイルを直接配置する。

```
frontend/
  package.json        react, react-dom, @apollo/client, graphql, vite, @vitejs/plugin-react
  vite.config.js      server.proxy で /api -> http://localhost:8888
  index.html
  src/main.jsx        ApolloProvider で App を包む
  src/apollo.js       new ApolloClient({ uri: '/api', cache: new InMemoryCache() })
  src/queries.js      gql で GET_TODOS / CREATE_TODO / TOGGLE_TODO / DELETE_TODO
  src/App.jsx         useQuery / useMutation。入力欄・一覧・チェックボックス・削除ボタン
  src/index.css       最小限
```

画面は 1 つ。ルーティングは入れない。

更新後の一覧反映は各 mutation の `refetchQueries: [GET_TODOS]` で行う。
Apollo のキャッシュ手動更新 (`update` / `cache.modify`) は今回の学習範囲から外れ、
複雑さに見合わないため使わない。

ローディングとエラーは `useQuery` の `loading` / `error` をそのまま文言表示する。

## テスト

`test/gql/db_test.clj` に `clojure.test` で CRUD 一巡を 1 本
(作成 → 一覧に現れる → toggle で done 反転 → 削除で消える)。

Docker の MySQL が起動していることを前提とした統合テストとする。
Testcontainers は導入しない (学習用構成に対して重すぎるため)。
テストは各ケースの終わりに自分が作った行を削除し、DB を元の状態に戻す。

`deps.edn` に `:test` エイリアスを追加して `clj -M:test` で実行できるようにする。

フロントエンドにはテストを置かない。

## エラー処理

| 状況 | 挙動 |
| --- | --- |
| MySQL 未起動でサーバ起動 | 最初のクエリで JDBC の例外。GraphQL の `errors` として返る |
| 存在しない id への toggle / delete | `resolve-as` で GraphQL エラー |
| 空文字の title | GraphQL 層では許容。フロント側で送信ボタンを無効化する |

例外の握り潰しはしない。学習用途では素の例外が見えたほうが原因を追いやすいため。

## 起動手順 (README に追記する)

```sh
docker compose up -d                        # MySQL
clj -M -m gql.core                          # GraphQL サーバ :8888
cd frontend && npm install && npm run dev   # フロント :5173
```

`http://localhost:5173` でアプリ、`http://localhost:8888/ide` で GraphiQL。

## 範囲外

認証、複数ユーザ、ページング、TODO の編集 (title 変更)、
本番向けビルド・デプロイ構成、マイグレーションツール、CI。
