# Lacinia + GraphQL 学習環境

Clojure の GraphQL ライブラリ [Lacinia](https://github.com/walmartlabs/lacinia) を学ぶための最小構成。
DB・認証は無し。ブラウザ (GraphiQL) からクエリを試せる状態がゴール。

## 1. インストールするパッケージ

| パッケージ | 内容 | インストール例 (macOS / Homebrew) |
| --- | --- | --- |
| JDK 21+ | Clojure の実行基盤 | `brew install temurin` |
| Clojure CLI | 依存解決・REPL (`clj` / `clojure` コマンド) | `brew install clojure/tools/clojure` |
| rlwrap (任意) | REPL の履歴・カーソル操作を快適に | `brew install rlwrap` |

Lacinia 自体は OS パッケージではなく Clojure ライブラリなので、個別インストールは不要。
`deps.edn` に書いてあるので初回起動時に Maven / Clojars から自動でダウンロードされる。

確認:

```sh
java -version   # 21 以上
clj -version
```

## 2. 起動

```sh
clj -M -m gql.core
```

起動したらブラウザで <http://localhost:8888/ide> を開く (GraphiQL)。
GraphQL エンドポイント自体は `POST http://localhost:8888/api`。

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

ブラウザを使わずに確認する場合:

```sh
curl -s -X POST http://localhost:8888/api \
  -H 'content-type: application/json' \
  -d '{"query":"{ hello greet(name: \"test\") }"}'
```

## 4. ファイル構成

```
deps.edn              依存とエイリアス (node でいう package.json)
resources/schema.edn  スキーマ定義 (どんな型・クエリがあるか)
src/gql/schema.clj    resolver の実装と、スキーマへの紐付け + compile
src/gql/core.clj      lacinia-pedestal でサーバを起動 (GraphiQL 有効化)
memo.md               Clojure の勉強メモ
```

Lacinia のコアは「スキーマ定義 → resolver 紐付け → compile → execute」の流れ。
この 4 ステップが `resources/schema.edn` と `src/gql/schema.clj` に収まっている。

- **スキーマ定義**: `:queries` の下に `hello` / `greet` を定義。`:resolve` にはキーワードだけ書く。
- **resolver**: `(context args value)` の 3 引数を取る関数。`args` にクエリの引数が入る。
- **紐付け**: `util/attach-resolvers` でキーワードと関数を対応させる。
- **compile**: `schema/compile` で実行可能なスキーマになる。あとは lacinia-pedestal が
  HTTP リクエストごとに `execute` を呼んでくれる。

## 5. バージョンについて

`deps.edn` のバージョンは以下。

| ライブラリ | バージョン |
| --- | --- |
| org.clojure/clojure | 1.12.5 |
| com.walmartlabs/lacinia | 1.3.0 |
| com.walmartlabs/lacinia-pedestal | 1.3 |
| io.pedestal/pedestal.service | 0.7.2 |
| io.pedestal/pedestal.jetty | 0.7.2 |

lacinia-pedestal / pedestal は Lacinia 本体とは別管理なので、依存解決に失敗した場合は
lacinia 1.3.0 と互換のあるバージョンに合わせて調整する。
