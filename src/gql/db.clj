(ns gql.db
  "MySQL への接続と SQL をここに閉じ込める。
  resolver から SQL が見えないようにするのが目的。

  前提: docker compose up -d で MySQL が起動していること。"
  (:require [next.jdbc :as jdbc]))

(def db-spec
  "接続情報。docker-compose.yml と揃えること。"
  {:dbtype   "mysql"
   :host     "localhost"
   :port     3307
   :dbname   "todo"
   :user     "todo"
   :password "todopass"})

;; DataSource は使い回す。接続プールは学習用途では要らないので入れていない。
(defonce ^:private ds (jdbc/get-datasource db-spec))

(defn list-todos
  "全件を新しい順で返す。"
  []
  (jdbc/execute! ds ["SELECT * FROM todos ORDER BY created_at DESC, id DESC"]))

(defn create-todo!
  "1 件追加して、追加された行を返す。

  LAST_INSERT_ID() は接続ごとの値なので、INSERT と SELECT を
  同じ接続で実行するためにトランザクションで囲っている。"
  [title]
  (jdbc/with-transaction [tx ds]
    (jdbc/execute-one! tx ["INSERT INTO todos (title) VALUES (?)" title])
    (jdbc/execute-one! tx ["SELECT * FROM todos WHERE id = LAST_INSERT_ID()"])))

(defn toggle-todo!
  "done を反転して、反転後の行を返す。該当 id が無ければ nil。"
  [id]
  (jdbc/with-transaction [tx ds]
    (let [updated (:next.jdbc/update-count
                   (jdbc/execute-one! tx ["UPDATE todos SET done = NOT done WHERE id = ?" id]))]
      (when (pos? updated)
        (jdbc/execute-one! tx ["SELECT * FROM todos WHERE id = ?" id])))))

(defn delete-todo!
  "1 件削除する。削除できたら true、該当 id が無ければ false。"
  [id]
  (pos? (:next.jdbc/update-count
         (jdbc/execute-one! ds ["DELETE FROM todos WHERE id = ?" id]))))
