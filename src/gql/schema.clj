(ns gql.schema
  "スキーマ定義 (EDN) と resolver を結び付けて、実行可能なスキーマにコンパイルする。

  Lacinia のコアはこの 3 ステップ。
    1. スキーマ定義  : どんな型・クエリがあるか (resources/schema.edn)
    2. resolver 紐付け: 各フィールドの値を実際に返す関数を割り当てる
    3. compile       : 実行可能なスキーマにする"
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.walmartlabs.lacinia.resolve :as resolve]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.util :as util]
            [gql.db :as db]))

;; resolver は (context args value) の 3 引数を取る関数。
;;   context : リクエスト全体で共有される情報 (DB 接続などを入れる場所)
;;   args    : クエリで渡された引数のマップ
;;   value   : 親フィールドが返した値 (ルートの Query では nil)
(defn- resolve-hello
  [_context _args _value]
  "Hello, Lacinia!")

(defn- resolve-greet
  [_context {:keys [name]} _value]
  (str "Hello, " name "!"))

;; ---------- DB の行を GraphQL のフィールドへ変換する ----------

(defn- ->boolean
  "JDBC ドライバによっては BOOLEAN (TINYINT(1)) が数値で返るため、両方を受ける。
  Task 2 で測定した実際の型は java.lang.Boolean だが、ドライバ/バージョン差異への
  防御として数値のケースも残す。"
  [v]
  (if (number? v) (pos? v) (boolean v)))

(defn- ->iso-string
  "java.sql.Timestamp / java.time.LocalDateTime のどちらでも ISO-8601 文字列にする。
  Task 2 で測定した実際の型は java.sql.Timestamp だが、ドライバ/バージョン差異への
  防御として LocalDateTime 等の他の型も str で素通しできるようにしておく。"
  [v]
  (when v
    (if (instance? java.sql.Timestamp v)
      (str (.toInstant ^java.sql.Timestamp v))
      (str v))))

(defn- row->todo
  "next.jdbc が返す :todos/xxx のマップを、GraphQL の Todo 型に合わせて詰め替える。"
  [row]
  {:id        (:todos/id row)
   :title     (:todos/title row)
   :done      (->boolean (:todos/done row))
   :createdAt (->iso-string (:todos/created_at row))})

;; ---------- TODO の resolver ----------

(defn- resolve-todos
  [_context _args _value]
  (mapv row->todo (db/list-todos)))

(defn- resolve-create-todo
  [_context {:keys [title]} _value]
  (row->todo (db/create-todo! title)))

(defn- resolve-toggle-todo
  [_context {:keys [id]} _value]
  (if-let [row (db/toggle-todo! id)]
    (row->todo row)
    ;; resolve-as で nil + GraphQL エラーを返す。例外にはしない。
    (resolve/resolve-as nil {:message "指定された TODO が見つかりません" :id id})))

(defn- resolve-delete-todo
  [_context {:keys [id]} _value]
  (if (db/delete-todo! id)
    true
    (resolve/resolve-as nil {:message "指定された TODO が見つかりません" :id id})))

(def ^:private resolver-map
  {:query/hello           resolve-hello
   :query/greet           resolve-greet
   :query/todos           resolve-todos
   :mutation/create-todo  resolve-create-todo
   :mutation/toggle-todo  resolve-toggle-todo
   :mutation/delete-todo  resolve-delete-todo})

(defn load-schema
  "resources/schema.edn を読み込み、resolver を紐付けてコンパイルしたスキーマを返す。"
  []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers resolver-map)
      schema/compile))
