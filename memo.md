## Clojure 勉強メモ

### deps.edn

node でいう package.json のようなもの。

### REPL

Read-Eval-Print-Loop の略。入力したコードをその場で評価して確認できる対話型の開発環境のこと。
node でいう 「>>> console.log('hello')」のようなことが clojure でできるようになる。

REPL を使っていない場合、「コーディング → 保存 → コンパイル → 起動 → 確認」という流れになる。
REPL を使うと、「コーディング → 評価 → 確認」という流れで効率よく開発ができるようになる。

VSCode などのエディタと REPL を繋げることで、コードを書きながらカーソル位置の関数だけを REPL で評価して確認することができるようになる。（便利）

### 名前空間（ns）

関数名や変数名が被らないように整理するための機能（普通のプログラミング言語と概念は同じ）
`ns` マクロを使って名前空間を定義する。

```clojure
(ns my-project.core)

(def app-name "My Application")

(defn greet [name])
  (str "Hello, " name)

;; REPL 上で名前空間を切り替える
(in-ns 'my-project.core)
```

名前空間に `⚪︎⚪︎⚪︎.core` という名前が使われているのは clojure の慣習で、つけなくても動くが、プロジェクトのエントリーポイントとして `core` を使うことが多い。

つまり、`.core` がついていたら「プロジェクト全体のメインの機能が集まっている場所」って認識しておけば良い。

```clojure
(require '[clojure.data.json :as json])
```

`clojure.data.json` のソースが読み込まれ、その中の関数（`write-str` など）が `clojure.data.json/write-str` という正式名でメモリ上に存在するようになる。すでにロード済みなら何もしない。

1. エイリアスの作成（今いる名前空間だけのローカルな効果）
`:as json` は「この名前空間の中でだけ、json と書いたら `clojure.data.json` の意味にする」という短縮名の登録。

### '[clojure.data.json :as json] の先頭の quote の有無の意味

- quote がある場合（`'[clojure.data.json :as json]`）は、リスト全体が評価されずにそのままの形で扱われる。
- quote がない場合（`[clojure.data.json :as json]`）は、リストが評価され、`require` マクロによって名前空間がロードされる。

つまり、' を付けると「これは “ロードしてほしい名前空間の指定書き” というただのデータですよ」と渡せる。付けないと Clojure が clojure.data.json を変数名だと思って中身を探しに行き、そんな変数はないのでエラーになる。

→ 名前空間をつけるときは基本つける！

## clojure 初学生向け

### 基本的な構文

- 関数呼び出しは `(関数名 引数1 引数2 ...)` の形
- 変数の定義は `(def 変数名 値)` の形
- 関数の定義は `(defn 関数名 [引数1 引数2 ...] 本体)` の形

### 例

```clojure
(def app-name "My Application")

(defn greet [name]
  (str "Hello, " name))

(greet "Alice") ;; => "Hello, Alice"
```

### 2. 基本データ型（リテラル）

|種類|例|用途|
|---|---|---|
|数値| 1 3.14 1/3 1/3 |は分数がそのまま
|文字列| "hello" |ダブルクォートのみ
|キーワード| :name :age |マップのキーによく使う。それ自体が値
|シンボル| x map |変数・関数の名前
|真偽| true false nil | nil と false |だけが偽、他は全部真

nil とは
  値が存在しないことを表す特殊な値。false と同様に条件式では偽とみなされる。（js でいう `null`）

### コレクション

```
[1 2 3]          ; ベクタ … 順序あり、添字アクセス。配列的
'(1 2 3)         ; リスト … コード構造。データではあまり使わない
{:a 1 :b 2}      ; マップ … キー↔値。一番よく使う
#{1 2 3}         ; セット … 重複なし
```

clojure はコレクション中心の言語。

[] → ベクタ
  JS の配列
{} → マップ
  JS のオブジェクト
() → リスト
  JS の配列（ただしイミュータブル）
${} → セット
  JS の new Set() ←　重複排除

「型を作らない」 = JSでクラスを書かずオブジェクトリテラルで済ます感覚。

TypeScript でわざわざ

```
class User { constructor(public name: string, public age: number) {} }
```

と書く代わりに、JS でよくやるように

```
const user = { name: "田中", age: 30 };
const users = [
  { name: "田中", age: 30 },
  { name: "佐藤", age: 25 },
];
```

で済ませる。Clojure は常にこのスタイル。だから扱うデータの大半が配列とオブジェクト（＝コレクション）になる。

### イミュータブル

Clojure のコレクションは基本的にイミュータブル（不変）である。つまり、一度作成したコレクションは変更できず、新しいコレクションを作成する操作が行われる。
例えば、ベクタに要素を追加する場合は `conj` 関数を使い、新しいベクタが返される。

```clojure
(def v [1 2 3])
(def v2 (conj v 4))
v  ;; => [1 2 3]
v2 ;; => [1 2 3 4]
```
