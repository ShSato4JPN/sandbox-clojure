(ns gql.core
  "GraphiQL 付きの HTTP サーバ (lacinia-pedestal) を起動するエントリポイント。

  起動方法
    clj -M -m gql.core          ; ターミナルから起動 (Ctrl-C で停止)
    (start!) / (stop!)          ; REPL から起動・停止

  起動後
    http://localhost:8888/ide   ; GraphiQL (ブラウザ)
    http://localhost:8888/api   ; GraphQL エンドポイント (POST)"
  (:require [com.walmartlabs.lacinia.pedestal2 :as lp]
            [gql.schema :as gql-schema]
            [io.pedestal.http :as http]))

(def port 8888)

(defn service-map
  "コンパイル済みスキーマから Pedestal のサービスマップを組み立てる。
  :graphiql true で GraphiQL (/ide) が有効になる。"
  []
  (lp/default-service (gql-schema/load-schema)
                      {:port     port
                       :graphiql true}))

;; ---------- REPL 用 ----------

(defonce ^:private server (atom nil))

(defn start!
  "REPL からサーバを起動する (ブロックしない)。"
  []
  (when-not @server
    (reset! server (-> (service-map)
                       (assoc ::http/join? false)
                       http/create-server
                       http/start))
    (println (str "GraphiQL: http://localhost:" port "/ide")))
  :started)

(defn stop!
  "REPL から起動したサーバを停止する。"
  []
  (when-let [s @server]
    (http/stop s)
    (reset! server nil))
  :stopped)

(defn restart!
  "スキーマや resolver を変更したあとに作り直す。"
  []
  (stop!)
  (start!))

;; ---------- ターミナル用 ----------

(defn -main
  [& _args]
  (println (str "GraphiQL: http://localhost:" port "/ide"))
  (-> (service-map)
      (assoc ::http/join? true)   ; メインスレッドを保持する
      http/create-server
      http/start))
