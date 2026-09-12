import { gql } from '@apollo/client'

export const GET_TODOS = gql`
  query GetTodos {
    todos {
      id
      title
      done
      createdAt
    }
  }
`

export const CREATE_TODO = gql`
  mutation CreateTodo($title: String!) {
    createTodo(title: $title) {
      id
      title
      done
      createdAt
    }
  }
`

export const TOGGLE_TODO = gql`
  mutation ToggleTodo($id: Int!) {
    toggleTodo(id: $id) {
      id
      done
    }
  }
`

export const DELETE_TODO = gql`
  mutation DeleteTodo($id: Int!) {
    deleteTodo(id: $id)
  }
`
