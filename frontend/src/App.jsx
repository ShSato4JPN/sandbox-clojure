import { useState } from 'react'
import { useQuery, useMutation } from '@apollo/client/react'
import { GET_TODOS, CREATE_TODO, TOGGLE_TODO, DELETE_TODO } from './queries'

// 更新後の一覧は refetchQueries で取り直す。
// Apollo のキャッシュを手で書き換える方法もあるが、今回は単純さを優先した。
const refetch = { refetchQueries: [GET_TODOS] }

export default function App() {
  const [title, setTitle] = useState('')
  const { data, loading, error } = useQuery(GET_TODOS)
  const [createTodo] = useMutation(CREATE_TODO, refetch)
  const [toggleTodo] = useMutation(TOGGLE_TODO, refetch)
  const [deleteTodo] = useMutation(DELETE_TODO, refetch)

  const onSubmit = async (event) => {
    event.preventDefault()
    const trimmed = title.trim()
    if (!trimmed) return
    await createTodo({ variables: { title: trimmed } })
    setTitle('')
  }

  return (
    <main>
      <h1>TODO</h1>

      <form onSubmit={onSubmit}>
        <input
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          placeholder="やること"
        />
        <button type="submit" disabled={!title.trim()}>
          追加
        </button>
      </form>

      {loading && <p>読み込み中…</p>}
      {error && <p className="error">エラー: {error.message}</p>}

      <ul>
        {data?.todos.map((todo) => (
          <li key={todo.id}>
            <label>
              <input
                type="checkbox"
                checked={todo.done}
                onChange={() => toggleTodo({ variables: { id: todo.id } })}
              />
              <span className={todo.done ? 'done' : undefined}>{todo.title}</span>
            </label>
            <button onClick={() => deleteTodo({ variables: { id: todo.id } })}>
              削除
            </button>
          </li>
        ))}
      </ul>
    </main>
  )
}
