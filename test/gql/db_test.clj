(ns gql.db-test
  "gql.db の結合テスト。Docker の MySQL が起動している前提。
    docker compose up -d
    clj -M:test"
  (:require [clojure.test :refer [deftest is testing]]
            [gql.db :as db]))

(defn- cleanup!
  "テストが作った行を消して DB を元の状態に戻す。"
  [id]
  (when id (db/delete-todo! id)))

(deftest crud-cycle
  (let [created (db/create-todo! "テスト用のタスク")
        id      (:todos/id created)]
    (try
      (testing "作成した行が返ってくる"
        (is (some? id))
        (is (= "テスト用のタスク" (:todos/title created))))

      (testing "一覧に現れる"
        (is (contains? (set (map :todos/id (db/list-todos))) id)))

      (testing "toggle で done が反転する"
        (let [before (:todos/done (first (filter #(= id (:todos/id %)) (db/list-todos))))
              after  (:todos/done (db/toggle-todo! id))]
          (is (not= (boolean before) (boolean after)))))

      (testing "存在しない id の toggle は nil"
        (is (nil? (db/toggle-todo! -1))))

      (testing "削除すると一覧から消える"
        (is (true? (db/delete-todo! id)))
        (is (not (contains? (set (map :todos/id (db/list-todos))) id))))

      (testing "存在しない id の削除は false"
        (is (false? (db/delete-todo! -1))))

      (finally
        (cleanup! id)))))
