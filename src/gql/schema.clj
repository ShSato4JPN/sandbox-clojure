(ns gql.schema
  "スキーマ定義 (EDN) と resolver を結び付けて、実行可能なスキーマにコンパイルする。

  Lacinia のコアはこの 3 ステップ。
    1. スキーマ定義  : どんな型・クエリがあるか (resources/schema.edn)
    2. resolver 紐付け: 各フィールドの値を実際に返す関数を割り当てる
    3. compile       : 実行可能なスキーマにする"
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.util :as util]))

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

(def ^:private resolver-map
  {:query/hello resolve-hello
   :query/greet resolve-greet})

(defn load-schema
  "resources/schema.edn を読み込み、resolver を紐付けてコンパイルしたスキーマを返す。"
  []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers resolver-map)
      schema/compile))
