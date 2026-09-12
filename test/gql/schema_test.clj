(ns gql.schema-test
  "コンパイル済みスキーマに対してクエリを実行する結合テスト。
  Docker の MySQL が起動している前提。"
  (:require [clojure.test :refer [deftest is testing]]
            [com.walmartlabs.lacinia :as lacinia]
            [gql.db :as db]
            [gql.schema :as gql-schema]))

(defn- q
  "コンパイル済みスキーマに対してクエリを 1 本実行する。"
  [query variables]
  (lacinia/execute (gql-schema/load-schema) query variables nil))

(deftest todo-operations
  (let [created (-> (q "mutation($t:String!){ createTodo(title:$t){ id title done createdAt } }"
                       {:t "スキーマテスト用のタスク"})
                    (get-in [:data :createTodo]))
        id      (:id created)]
    (try
      (testing "createTodo が作成した Todo を返す"
        (is (some? id))
        (is (= "スキーマテスト用のタスク" (:title created)))
        (is (false? (:done created)))
        (is (string? (:createdAt created))))

      (testing "todos に含まれる"
        (let [ids (->> (q "{ todos { id } }" nil) :data :todos (map :id) set)]
          (is (contains? ids id))))

      (testing "toggleTodo で done が true になる"
        (is (true? (-> (q "mutation($i:Int!){ toggleTodo(id:$i){ done } }" {:i id})
                       (get-in [:data :toggleTodo :done])))))

      (testing "存在しない id の toggleTodo はエラーを返す"
        (let [result (q "mutation($i:Int!){ toggleTodo(id:$i){ done } }" {:i -1})]
          (is (seq (:errors result)))))

      (testing "deleteTodo が true を返す"
        (is (true? (-> (q "mutation($i:Int!){ deleteTodo(id:$i) }" {:i id})
                       (get-in [:data :deleteTodo])))))

      (testing "存在しない id の deleteTodo はエラーを返す"
        (is (seq (:errors (q "mutation($i:Int!){ deleteTodo(id:$i) }" {:i -1})))))

      (finally
        (when id (db/delete-todo! id))))))
