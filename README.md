# Lacinia + GraphQL 学習環境

Clojure の GraphQL ライブラリ [Lacinia](https://github.com/walmartlabs/lacinia) を学ぶための最小構成。
MySQL (Docker) と React フロントエンドを繋いだ TODO アプリまでを一通り動かせる構成。認証は無し。

## 1. インストールするパッケージ

| パッケージ | 内容 | インストール例 (macOS / Homebrew) |
| --- | --- | --- |
| JDK 21+ | Clojure の実行基盤 | `brew install temurin` |
| Clojure CLI | 依存解決・REPL (`clj` / `clojure` コマンド) | `brew install clojure/tools/clojure` |
| rlwrap (任意) | REPL の履歴・カーソル操作を快適に | `brew install rlwrap` |
| Docker Desktop | MySQL をコンテナで動かす | `brew install --cask docker` |
| Node.js 20.19+ | フロントエンド (Vite) のビルド・dev サーバ | `brew install node` |

Lacinia 自体は OS パッケージではなく Clojure ライブラリなので、個別インストールは不要。
`deps.edn` に書いてあるので初回起動時に Maven / Clojars から自動でダウンロードされる。

確認:

```sh
java -version   # 21 以上
clj -version
```

## 2. 起動

3 つを順に起動する。

```sh
docker compose up -d                        # MySQL (:3307)
clj -M -m gql.core                          # GraphQL サーバ (:8888)
cd frontend && npm install && npm run dev   # フロントエンド (:5173)
```

| URL | 内容 |
| --- | --- |
| <http://localhost:5173> | TODO アプリ |
| <http://localhost:8888/ide> | GraphiQL |
| <http://localhost:8888/api> | GraphQL エンドポイント (POST) |

フロント (:5173) と GraphQL サーバ (:8888) はポートが違うため、そのまま fetch すると
別オリジン扱いで CORS に弾かれる。`frontend/vite.config.js` の `server.proxy` で
`/api` を :8888 に中継しているので、ブラウザからは同一オリジンに見える。
サーバ側に CORS 設定は入れていない。

DB を初期状態に戻したいときは `docker compose down -v` でボリュームごと削除する。

REPL から起動・停止する場合:

```sh
clj -M:dev          # nREPL (エディタから接続する場合)
# もしくは
rlwrap clj
```

```clojure
(require '[gql.core :as core])
(core/start!)    ; 起動 (ブロックしない)
(core/restart!)  ; スキーマ・resolver を変更したら作り直す
(core/stop!)     ; 停止
```

## 3. 動作確認

GraphiQL で以下を実行する。

```graphql
{ hello }
```

```json
{ "data": { "hello": "Hello, Lacinia!" } }
```

```graphql
{ greet(name: "test") }
```

```json
{ "data": { "greet": "Hello, test!" } }
```

TODO の操作は以下で試せる。

```graphql
mutation { createTodo(title: "牛乳を買う") { id title done } }
```

```graphql
{ todos { id title done createdAt } }
```

ブラウザを使わずに確認する場合:

```sh
curl -s -X POST http://localhost:8888/api \
  -H 'content-type: application/json' \
  -d '{"query":"{ hello greet(name: \"test\") }"}'
```

## 4. ファイル構成

```
deps.edn              依存とエイリアス (node でいう package.json)
docker-compose.yml    MySQL コンテナ
db/init/01-schema.sql todos テーブル定義 (コンテナ初回起動時に適用)
resources/schema.edn  スキーマ定義 (どんな型・クエリがあるか)
src/gql/schema.clj    resolver の実装と、スキーマへの紐付け + compile
src/gql/db.clj        MySQL への接続と SQL
src/gql/core.clj      lacinia-pedestal でサーバを起動 (GraphiQL 有効化)
test/gql/             結合テスト (MySQL 起動が前提)
frontend/             React + Vite + Apollo Client
memo.md               Clojure の勉強メモ
```

Lacinia のコアは「スキーマ定義 → resolver 紐付け → compile → execute」の流れ。
この 4 ステップが `resources/schema.edn` と `src/gql/schema.clj` に収まっている。

- **スキーマ定義**: `:queries` の下に `hello` / `greet` を定義。`:resolve` にはキーワードだけ書く。
- **resolver**: `(context args value)` の 3 引数を取る関数。`args` にクエリの引数が入る。
- **紐付け**: `util/attach-resolvers` でキーワードと関数を対応させる。
- **compile**: `schema/compile` で実行可能なスキーマになる。あとは lacinia-pedestal が
  HTTP リクエストごとに `execute` を呼んでくれる。

SQL は `src/gql/db.clj` にだけ置いて、resolver からは関数を呼ぶだけにしている。
DB の行 (`:todos/created_at` など) を GraphQL のフィールド (`createdAt`) に詰め替えるのは
`src/gql/schema.clj` の `row->todo` の仕事。

## 5. テスト

MySQL が起動している状態で実行する。

```sh
docker compose up -d
clj -M:test
```

## 6. バージョンについて

`deps.edn` のバージョンは以下。

| ライブラリ | バージョン |
| --- | --- |
| org.clojure/clojure | 1.12.5 |
| com.walmartlabs/lacinia | 1.3.0 |
| com.walmartlabs/lacinia-pedestal | 1.3 |
| io.pedestal/pedestal.service | 0.7.2 |
| io.pedestal/pedestal.jetty | 0.7.2 |
| com.github.seancorfield/next.jdbc | 1.3.1118 |
| com.mysql/mysql-connector-j | 9.3.0 |

lacinia-pedestal / pedestal は Lacinia 本体とは別管理なので、依存解決に失敗した場合は
lacinia 1.3.0 と互換のあるバージョンに合わせて調整する。
